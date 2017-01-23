package com.kurento.khc.rest.v2;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.agenda.datamodel.pojo.Account;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.agenda.services.pojo.AccountReadInfoResponse;
import com.kurento.agenda.services.pojo.UserCreate;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.ContentService;

@Path("/account")
public class AccountRestfulService {

	public static final String ACCOUNT_ID = "accountId";

	@Autowired
	private AdministrativeService administrativeService;
	@Autowired
	private ContentService contentService;
	@Autowired
	private UserRestfulService userRestfulService;

	// READ_ACCOUNT_INFO
	@GET
	@Path("/{" + ACCOUNT_ID + "}/info")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response readAccountInfoById(@PathParam(ACCOUNT_ID) String accountId)
			throws Exception {
		Account account;
		try {
			account = administrativeService.getAccountInfo(Long
					.valueOf(accountId));
		} catch (NumberFormatException e) {
			account = administrativeService.getAccountInfo(accountId);
		}
		AccountReadInfoResponse accountReadInfo = account
				.buildAccountReadInfo();
		return Response.status(Response.Status.OK).entity(accountReadInfo)
				.build();
	}

	// CREATE_USER
	@POST
	@Path("/{" + ACCOUNT_ID + "}/user")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response createUserSimple(@PathParam(ACCOUNT_ID) Long accountId,
			UserCreate user) throws Exception {

		User userResponse = administrativeService.createUserInAutoregister(
				user.buildUserPojo(), accountId);

		return Response.status(Response.Status.CREATED)
				.entity(userResponse.buildUserCreateResponse()).build();
	}

	// CREATE_USER MULTIPART
	@POST
	@Path("/{" + ACCOUNT_ID + "}/user")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response createUserMultipart(
			@PathParam(ACCOUNT_ID) Long accountId,
			@Multipart(value = "user", type = MediaType.APPLICATION_JSON) UserCreate user,
			@Multipart(value = "picture") Attachment picture) throws Exception {

		String contentType = picture.getContentType().toString();
		InputStream contentStream = picture.getDataHandler().getInputStream();

		// Store picture
		Content content = contentService
				.saveContent(contentStream, contentType);

		try {
			// Save user in database. Minimize transaction time
			User userResponse = administrativeService.createUserInAutoregister(
					user.buildUserPojo(), accountId, content);
			return Response.status(Response.Status.CREATED)
					.entity(userResponse.buildUserCreateResponse()).build();
		} catch (Exception e) {
			contentService.deleteContent(content);
			throw e;
		}
	}

}
