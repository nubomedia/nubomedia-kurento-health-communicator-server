package com.kurento.khc;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

@Provider
public class AccessDeniedExceptionMapper implements
		ExceptionMapper<AccessDeniedException> {

	private static Logger log = LoggerFactory
			.getLogger(AccessDeniedExceptionMapper.class);

	@Override
	public Response toResponse(AccessDeniedException exception) {
		log.warn("Unable to complete request due to Access denied exception:",
				exception);
		return Response.status(Response.Status.FORBIDDEN).build();
	}
}
