package com.kurento.khc;

import java.io.FileNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNotFoundExceptionMapper implements
		ExceptionMapper<FileNotFoundException> {

	private static final Logger log = LoggerFactory
			.getLogger(FileNotFoundExceptionMapper.class);

	@Override
	public Response toResponse(FileNotFoundException exception) {
		Throwable cause = exception;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		log.warn("Unable to complete KHC request because requested file was not found: "
				+ cause);
		return Response.status(Response.Status.NOT_FOUND).build();
	}
}
