package com.kurento.khc;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo;
import com.kurento.agenda.services.pojo.KhcInvalidDataInfoResponse;

@Provider
public class KhcInvalidDataExceptionMapper implements
		ExceptionMapper<KhcInvalidDataException> {

	private static Logger log = LoggerFactory
			.getLogger(KhcInvalidDataExceptionMapper.class);

	@Override
	public Response toResponse(KhcInvalidDataException exception) {
		log.warn("Unable to complete request due to wrong data input: " + exception.getCode() + ":"
				+ exception.getMessage());
		log.debug("Stacktrace of invalid data exception", exception);
		KhcInvalidDataInfo invalidData = new KhcInvalidDataInfo();
		invalidData.setCode(exception.getCode());
		return Response.status(Response.Status.BAD_REQUEST)
				.entity(buildKhcInvalidDataInfoResponse(invalidData))
				.type(MediaType.APPLICATION_JSON).build();
	}

	private KhcInvalidDataInfoResponse buildKhcInvalidDataInfoResponse(
			KhcInvalidDataInfo invalidData) {
		KhcInvalidDataInfoResponse response = new KhcInvalidDataInfoResponse();
		response.setCode(invalidData.getCode());
		return response;
	}
}
