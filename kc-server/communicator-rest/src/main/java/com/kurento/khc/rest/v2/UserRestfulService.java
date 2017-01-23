package com.kurento.khc.rest.v2;

import java.io.File;
import java.util.List;

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
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.agenda.services.pojo.UserMemberGroupsResponse;
import com.kurento.agenda.services.pojo.UserReadContactResponse;
import com.kurento.agenda.services.pojo.UserReadResponse;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.utils.FileRepository;

@Path("/user")
public class UserRestfulService {

	public static final String USER_ID = "userId";
	public static final String GROUP_ID = "groupId";
	public static final String PATTERN = "pattern";
	public static final String FIRST_RESULT = "firstResult";
	public static final String MAX_RESULT = "maxResult";
	public static final String AVATAR_SIZE = "avatarSize";

	@Autowired
	private AdministrativeService administrativeService;
	@Autowired
	private ContentService contentService;
	@Autowired
	private FileRepository repository;

	// READ_USER
	@GET
	@Path("/{" + USER_ID + "}")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response readUser(@PathParam(USER_ID) Long userId) throws Exception {

		User user = administrativeService.getUser(userId);
		UserReadResponse userRead = user.buildUserReadResponse();
		return Response.status(Response.Status.OK).entity(userRead).build();

	}

	// READ_USER_AVATAR
	@GET
	@Path("/{" + USER_ID + "}/avatar{" + AVATAR_SIZE + ":(/" + AVATAR_SIZE
			+ "/[^/]+?)?}")
	public Response readUserAvatar(@PathParam(USER_ID) Long userId,
			@PathParam(AVATAR_SIZE) String size) throws Exception {

		Content content = administrativeService.getUserAvatar(userId);
		String contentUrl = content.getUrl(size);
		File contentFile = repository.getMediaFile(contentUrl);
		String contentType = content.getContentType();

		return Response.status(Status.OK).type(contentType)
				.entity(new DataHandler(new FileDataSource(contentFile)))
				.build();

	}

	// READ_ME
	@GET
	@Path("/me")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response readMe() throws Exception {
		UserReadResponse user = administrativeService.getMe()
				.buildUserReadResponse();
		return Response.status(Response.Status.OK).entity(user).build();
	}

	// READ_ME_AVATAR
	@GET
	@Path("me/avatar{" + AVATAR_SIZE + ":(/" + AVATAR_SIZE + "/[^/]+?)?}")
	public Response getMeAvatar(@PathParam(AVATAR_SIZE) String size)
			throws Exception {
		Content content = administrativeService.getMeAvatar();
		String contentUrl = content.getUrl(size);
		File contentFile = repository.getMediaFile(contentUrl);
		String contentType = content.getContentType();

		return Response.status(Status.OK).type(contentType)
				.entity(new DataHandler(new FileDataSource(contentFile)))
				.build();
	}

	// READ_USER_CONTACT
	@GET
	@Path("/{" + USER_ID + "}/contact")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response readUserContact(@PathParam(USER_ID) Long userId)
			throws Exception {
		User user = administrativeService.getUserContact(userId);
		UserReadContactResponse userContact = user
				.buildUserReadContactResponse();
		return Response.status(Response.Status.OK).entity(userContact).build();
	}

	// READ_USER_CONTACT_AVATAR
	@GET
	@Path("/{" + USER_ID + "}/contact/avatar{" + AVATAR_SIZE + ":(/"
			+ AVATAR_SIZE + "/[^/]+?)?}")
	public Response readUserContactAvatar(@PathParam(USER_ID) Long userId,
			@PathParam(AVATAR_SIZE) String size) throws Exception {

		Content content = administrativeService.getUserContactAvatar(userId);
		String contentUrl = content.getUrl(size);
		File contentFile = repository.getMediaFile(contentUrl);
		String contentType = content.getContentType();

		return Response.status(Status.OK).type(contentType)
				.entity(new DataHandler(new FileDataSource(contentFile)))
				.build();
	}

	// READ_USER_MEMBER_GROUPS
	@GET
	@Path("/{" + USER_ID + "}/memberGroups")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response readUserMemberGroups(@PathParam(USER_ID) Long userId)
			throws Exception {
		// only returns groups where authed user is admin (also works for
		// account admins)
		List<Group> groups = administrativeService
				.getGroupsWhereUserIsMember(userId);
		UserMemberGroupsResponse userMemberGroups = new UserMemberGroupsResponse();
		userMemberGroups.addGroups(groups);
		return Response.status(Response.Status.OK).entity(userMemberGroups)
				.build();
	}
}