package com.kurento.khc.rest.v2;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.datamodel.pojo.Topic;
import com.kurento.agenda.services.pojo.SubscriptionCreate;
import com.kurento.agenda.services.pojo.topic.TopicCreate;
import com.kurento.khc.services.v2.CommandService;

@Path("/subscription")
public class SubscriptionRestfulService {

	private static final String CHANNEL_ID = "channelId";
	private static final String TOPIC_KEY = "topicKey";

	@Autowired
	private CommandService commandService;

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response createSubscription(SubscriptionCreate subsRequest) throws Exception {
		Channel notch = commandService.createSubscription(subsRequest.getInstanceId());
		return Response.status(Response.Status.OK)
				.entity(notch.buildNotificationChannelCreateResponse()).build();
	}

	@DELETE
	@Path("/{" + CHANNEL_ID + "}")
	public Response deleteSubscription(@PathParam(CHANNEL_ID) Long notchId) throws Exception {
		commandService.deleteSubscription(notchId);
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@POST
	@Path("/{" + CHANNEL_ID + "}/topic")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response addTopic(@PathParam(CHANNEL_ID) Long notchId, TopicCreate topicRequest)
			throws Exception {
		Topic topic = topicRequest.buildTopicPojo();
		commandService.addSubscriptionTopic(notchId, topic);
		return Response.status(Response.Status.OK).entity(topic.buildTopicCreateResponse())
				.type(MediaType.APPLICATION_JSON).build();
	}

	@DELETE
	@Path("/{" + CHANNEL_ID + "}/topic/{" + TOPIC_KEY + "}")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response deleteTopic(@PathParam(CHANNEL_ID) Long notchId,
			@PathParam(TOPIC_KEY) String topicKey)
			throws Exception {
		commandService.removeSubscriptionTopic(notchId, Topic.deconstructTopicKey(topicKey));
		return Response.status(Response.Status.NO_CONTENT).build();
	}
}
