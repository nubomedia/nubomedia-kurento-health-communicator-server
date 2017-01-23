package com.kurento.khc.rest.v2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Message;
import com.kurento.agenda.datamodel.pojo.Timeline;
import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.MessageService;
import com.kurento.khc.utils.FileRepository;

@Path("/message")
public class MessageRestfulService {

	@Autowired
	private MessageService messageService;
	@Autowired
	private AdministrativeService administrativeService;

	@Autowired
	private UserRestfulService userRestfulService;

	@Autowired
	private FileRepository fileRepository;

	private static final String MESSAGE_ID = "messageId";
	private static final String TIMELINE_ID = "timelineId";
	private static final String THUMBNAIL_SIZE = "thumbnailSize";
	private static final String AVATAR_SIZE = "avatarSize";

	// READ_MESSAGE
	@GET
	@Path("/{" + TIMELINE_ID + "}/message")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response readMessageListFromTimeline(
			@PathParam(value = TIMELINE_ID) Long timelineId,
			@QueryParam(value = Timeline.PARAM_LAST_MESSAGE) Long lastMessage,
			@QueryParam(value = Timeline.PARAM_MAX_MESSAGE) Integer maxMessage)
			throws Exception {
		List<Message> messages = messageService.getMessageListFromTimeline(
				timelineId, lastMessage, maxMessage);
		List<MessageReadResponse> messageResponses = new ArrayList<MessageReadResponse>();
		for (Message message : messages) {
			messageResponses.add(message.buildMessageReadResponse());
		}

		return Response.status(Response.Status.OK).entity(messageResponses)
				.build();
	}

	// READ_MESSAGE_CONTENT
	@GET
	@Path("/{" + TIMELINE_ID + "}/{" + MESSAGE_ID + "}/content")
	public Response readMessageContent(@PathParam(TIMELINE_ID) Long timelineId,
			@PathParam(MESSAGE_ID) Long messageId) throws Exception {
		Content content = messageService.getMessageContent(timelineId,
				messageId);
		File contentFile = fileRepository.getMediaFile(content.getContentUrl());
		String contentType = content.getContentType();
		return Response.status(Status.OK).type(contentType)
				.entity(new DataHandler(new FileDataSource(contentFile)))
				.build();
	}

	// READ_MESSAGE_THUMBNAIL
	@GET
	@Path("/{" + TIMELINE_ID + "}/{" + MESSAGE_ID + "}/thumbnail/{"
			+ THUMBNAIL_SIZE + "}")
	public Response readMessageContentThumbnail(
			@PathParam(TIMELINE_ID) Long timelineId,
			@PathParam(MESSAGE_ID) Long messageId,
			@PathParam(THUMBNAIL_SIZE) String size) throws Exception {
		Content content = messageService.getMessageContent(timelineId,
				messageId);
		String contentUrl = content.getUrl(size);
		File contentFile = fileRepository.getMediaFile(contentUrl);
		String contentType = content.getThumbnailType();
		return Response.status(Status.OK).type(contentType)
				.entity(new DataHandler(new FileDataSource(contentFile)))
				.build();

	}

	// READ_MESSAGE_AVATAR
	@GET
	@Path("/{" + TIMELINE_ID + "}/{" + MESSAGE_ID + "}/avatar{" + AVATAR_SIZE
			+ ":(/" + AVATAR_SIZE + "/[^/]+?)?}")
	public Response readMessageAvatar(@PathParam(TIMELINE_ID) Long timelineId,
			@PathParam(MESSAGE_ID) Long messageId,
			@PathParam(AVATAR_SIZE) String size) throws Exception {
		Content content = messageService
				.getMessageAvatar(timelineId, messageId);
		String contentUrl = content.getContentUrl();
		File contentFile = fileRepository.getMediaFile(contentUrl);
		String contentType = content.getContentType();
		return Response.status(Status.OK).type(contentType)
				.entity(new DataHandler(new FileDataSource(contentFile)))
				.build();

	}

}
