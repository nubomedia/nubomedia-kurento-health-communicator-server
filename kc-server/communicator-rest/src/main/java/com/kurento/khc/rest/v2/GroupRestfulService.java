package com.kurento.khc.rest.v2;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.utils.FileRepository;

@Path("/group")
public class GroupRestfulService {

	public static final String GROUP_ID = "groupId";
	public static final String AVATAR_SIZE = "avatarSize";

	@Autowired
	private AdministrativeService administrativeService;
	@Autowired
	private ContentService contentService;
	@Autowired
	private FileRepository repository;

	// READ_GROUP
	@GET
	@Path("/{" + GROUP_ID + "}")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response readGroup(@PathParam(GROUP_ID) Long groupId) {
		Group group = administrativeService.getGroup(groupId);

		return Response.status(Status.OK)
				.entity(group.buildGroupReadResponse()).build();
	}

	// READ_GROUP_AVATAR
	@GET
	@Path("/{" + GROUP_ID + "}/avatar{" + AVATAR_SIZE + ":(/" + AVATAR_SIZE
			+ "/[^/]+?)?}")
	public Response readGroupAvatar(@PathParam(GROUP_ID) Long groupId,
			@PathParam(AVATAR_SIZE) String size) throws Exception {

		Content content = administrativeService.getGroupAvatar(groupId);
		String contentUrl = content.getUrl(size);
		File contentFile = repository.getMediaFile(contentUrl);
		String contentType = content.getContentType();

		return Response.status(Status.OK).type(contentType)
				.entity(new DataHandler(new FileDataSource(contentFile)))
				.build();

	}

}
