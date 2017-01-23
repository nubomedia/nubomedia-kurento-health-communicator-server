package com.kurento.khc.rest.v2;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.services.pojo.CommandRead;
import com.kurento.agenda.services.pojo.CommandReadResponse;
import com.kurento.agenda.services.pojo.CommandSend;
import com.kurento.khc.KhcInternalServerException;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.ContentService;

@Path("/command")
public class CommandRestfulService {

	private static Logger log = LoggerFactory
			.getLogger(CommandRestfulService.class);

	private static final int MAX_RETRIES = 10;
	private static final int MAX_BACKOFF = 10;
	private static final SecureRandom rnd = new SecureRandom();

	@Autowired
	CommandService commandService;
	@Autowired
	ContentService contentService;

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response sendCommandSimple(CommandSend command) throws Exception {

		return sendCommand(command, null);

	}

	@POST
	@Path("/transaction")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response sendCommandTransaction(List<List<CommandSend>> transactions)
			throws Exception {

		List<Map<String, Object>> responses = new ArrayList<Map<String, Object>>();
		for (List<CommandSend> transaction : transactions) {
			Map<String, Object> response = new HashMap<String, Object>();
			try {
				sendTransaction(transaction);
				response.put("code", Code.OK);
			} catch (KhcNotFoundException e) {
				response.put("entity", e.getEntity());
				response.put("filter", e.getFilter());

			} catch (KhcInvalidDataException e) {
				response.put("code", e.getCode());
			}
			responses.add(response);
		}

		return Response.status(Response.Status.CREATED).entity(responses)
				.type(MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response sendCommandMultipart(
			@Multipart(value = "command", type = MediaType.APPLICATION_JSON) CommandSend command,
			@Multipart(value = "content") Attachment content) throws Exception {

		return sendCommand(command, content);

	}

	private Response sendCommand(CommandSend command, Attachment attachment)
			throws InterruptedException, IOException {

		Long startTime = System.currentTimeMillis();
		log.debug("Start '{}' command processing {}", command.getMethod());
		Content content = null;
		if (attachment != null) {
			String contentType = attachment.getContentType().toString();
			InputStream contentStream = attachment.getDataHandler()
					.getInputStream();
			content = contentService.saveContent(contentStream, contentType);
			log.debug("Save command content in {}", System.currentTimeMillis()
					- startTime);
			String conLenHeader = null;
			try {
				conLenHeader = attachment.getHeader(HTTP.CONTENT_LEN);
				long attachmentSize = Long.parseLong(conLenHeader);
				if (!content.getContentSize().equals(attachmentSize)) {
					contentService.deleteContent(content);
					throw new KhcInvalidDataException("Content incomplete, local="
							+ content.getContentSize() + " <> remote="
							+ attachmentSize, Code.INVALID_DATA);
				}
			} catch (NumberFormatException e) {
				contentService.deleteContent(content);
				throw new KhcInvalidDataException(
						"Content attachment with invalid content-lenght header: "
						+ conLenHeader, Code.COMMAND_INVALID_FORMAT);
			}
		}
		RuntimeException rtex;
		int retries = 0;
		int backoff = rnd.nextInt(MAX_BACKOFF);
		while (true) {
			try {
				commandService.executeCommand(command.buildCommandPojo(),
						content, false);
				return Response.status(Response.Status.CREATED).build();
			} catch (RuntimeException e) {
				rtex = verifyLock(e, content);
			}
			String method = command.getMethod();
			if (retries++ >= MAX_RETRIES) {
				if (content != null) {
					contentService.deleteContent(content);
				}
				throw new KhcInternalServerException(
						"Unable to execute command due to persistent locking:"
								+ method, rtex);
			}
			Thread.sleep(backoff * retries * retries);
			log.info("DDBB LOCK. Retry method: " + method);
		}
	}

	private void sendTransaction(List<CommandSend> commands)
			throws InterruptedException {
		RuntimeException rtex;
		int retries = 0;
		int backoff = rnd.nextInt(MAX_BACKOFF);
		List<Command> cmds = new ArrayList<Command>();
		for (CommandSend command : commands) {
			cmds.add(command.buildCommandPojo());
		}
		while (true) {
			try {
				commandService.executeTransaction(cmds, false);
				return;
			} catch (RuntimeException e) {
				rtex = verifyLock(e, null);
			}
			if (retries++ >= MAX_RETRIES) {
				throw new KhcInternalServerException(
						"Unable to execute command transaction due to persistent locking",
						rtex);
			}
			Thread.sleep(backoff * retries * retries);
			log.info("DDBB LOCK. Retry transaction");
		}
	}

	private RuntimeException verifyLock(RuntimeException exception,
			Content content) throws RuntimeException {
		Throwable cause = exception;
		while (cause.getCause() != null) {
			if (cause.getMessage().contains("LockAcquisitionException")) {
				return exception;
			} else {
				cause = cause.getCause();
			}
		}
		contentService.deleteContent(content);
		throw exception;
	}

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response readCommand(
			@QueryParam(CommandRead.PARAM_CHANNEL_ID) Long channelId,
			@QueryParam(CommandRead.PARAM_LAST_SEQUENCE) Long lastSequence)
			throws Exception {

		if (channelId == null) {
			throw new KhcInvalidDataException(
					"Channel ID is mandatory when reading pending commands",
					Code.INVALID_DATA);
		}
		if (lastSequence == null) {
			throw new KhcInvalidDataException(
					"Las executed command sequence is mandatory when reading pending commands",
					Code.INVALID_DATA);
		}
		List<Command> pendingCommands = commandService.getPendingCommands(
				channelId, lastSequence);
		List<CommandReadResponse> commandResponseList = new ArrayList<CommandReadResponse>();
		for (Command cmd : pendingCommands) {
			commandResponseList.add(cmd.buildCommandReadResponse());
		}
		return Response.status(Response.Status.OK).entity(commandResponseList)
				.build();
	}
}
