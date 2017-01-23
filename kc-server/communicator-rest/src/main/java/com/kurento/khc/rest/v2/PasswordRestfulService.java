package com.kurento.khc.rest.v2;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.agenda.datamodel.pojo.PasswordRecovery;
import com.kurento.agenda.services.pojo.PasswordRequestSecurityCode;
import com.kurento.agenda.services.pojo.PasswordResetPassword;
import com.kurento.khc.services.v2.AdministrativeService;

@Path("/password")
public class PasswordRestfulService {

	// TODO support internationalization
	private static final String PWD_MSG_SUBJECT = "Solcitud de recuperación de clave Kurento";
	private static final String PWD_MSG_BODY = "<br/>El código de seguridad para la recuperación de clave es: <br/>";

	@Autowired
	private AdministrativeService administrativeService;

	// PASSWORD_RESET_REQUEST
	@POST
	@Path("/code")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response sendSecurityCode(PasswordRequestSecurityCode request)
			throws Exception {
		PasswordRecovery pwdRecovery = administrativeService
				.getSecurityCode(request.getUserIdentity());

		administrativeService.sendSecurityCode(pwdRecovery, PWD_MSG_SUBJECT,
				PWD_MSG_BODY + pwdRecovery.getSecurityCode());

		return Response.status(Response.Status.NO_CONTENT).build();

	}

	// PASSWORD_SETUP
	@POST
	@Path("/reset")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response resetPassword(PasswordResetPassword reset) throws Exception {

		administrativeService.changePassword(reset.getSecurityCode(),
				reset.getNewPassword());
		return Response.status(Response.Status.NO_CONTENT).build();

	}

}
