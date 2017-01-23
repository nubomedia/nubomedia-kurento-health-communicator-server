package com.kurento.khc.rest.v2;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.services.pojo.ChannelCreate;
import com.kurento.agenda.services.pojo.ChannelEdit;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.LogService;

@Path("/channel")
public class ChannelRestfulService {

	private static final String CHANNEL_ID = "channelId";

	@Autowired
	private CommandService commandService;

	@Autowired
	private LogService logService;

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response createChannel(ChannelCreate notchRequest)
			throws Exception {

		// Find out if there is already a channel with requested notch
		Channel notch = notchRequest.buildNoticicationChannelPojo();
		notch = commandService.createNotificationChannel(notch);

		return Response.status(Response.Status.OK)
				.entity(notch.buildNotificationChannelCreateResponse()).build();
	}

	@PUT
	@Path("/{" + CHANNEL_ID + "}")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response editChannel(@PathParam(CHANNEL_ID) Long channelId,
			ChannelEdit notch) throws Exception {

		// Set notch ID from request instead of pojo
		// Do not trust requester
		notch.setId(channelId);
		commandService.updateNotificationChannel(notch
				.buildNoticicationChannelPojo());
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@DELETE
	@Path("/{" + CHANNEL_ID + "}")
	public Response deleteChannel(@PathParam(CHANNEL_ID) Long channelId)
			throws Exception {

		commandService.deleteNotificationChannel(channelId);
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@POST
	@Path("/{" + CHANNEL_ID + "}/log")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response logTest(@PathParam(CHANNEL_ID) Long channelId,
			String clientLog) throws Exception {
		// Write log to log directory
		logService.log(channelId, clientLog);
		return Response.status(Response.Status.CREATED).build();
	}

}
