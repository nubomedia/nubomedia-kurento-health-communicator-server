package com.kurento.khc;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;

@Provider
public class KhcNotFoundExceptionMapper implements
		ExceptionMapper<KhcNotFoundException> {

	private static final Logger log = LoggerFactory
			.getLogger(KhcNotFoundExceptionMapper.class);

	@Override
	public Response toResponse(KhcNotFoundException exception) {
		KhcNotFoundInfo response = new KhcNotFoundInfo();
		response.setCode(exception.getCode());
		Map<String, Object> query = new HashMap<String, Object>();
		query.put("entity", exception.getEntity());
		query.put("filter", exception.getFilter());
		response.setQuery(query);
		log.warn("Unable to complete KHC request because requested entity was not found:"
						+ exception.getEntity() + " - " + exception.getFilter(),
				exception);

		return Response.status(Response.Status.NOT_FOUND).entity(response)
				.type(MediaType.APPLICATION_JSON).build();
	}

}
