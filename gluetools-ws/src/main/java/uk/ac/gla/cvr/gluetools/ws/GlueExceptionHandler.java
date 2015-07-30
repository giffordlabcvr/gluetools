package uk.ac.gla.cvr.gluetools.ws;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public class GlueExceptionHandler implements ExceptionMapper<GlueException>{

    @Context
    private HttpHeaders headers;

    private MediaType getAcceptType(){
         List<MediaType> accepts = headers.getAcceptableMediaTypes();
         if(accepts!=null && accepts.size() > 0) {
        	 if(accepts.contains(MediaType.APPLICATION_XML_TYPE)) {
            	 return MediaType.APPLICATION_XML_TYPE;
        	 } 
         }
    	 return MediaType.APPLICATION_JSON_TYPE;
    }

	@Override
	public Response toResponse(GlueException glueException) {
        MediaType acceptType = getAcceptType();
        String entity;
        if(acceptType == MediaType.APPLICATION_JSON_TYPE) {
        	entity = JsonUtils.prettyPrint(glueException.toJsonObject());
        } else {
        	// entity = new String(XmlUtils.prettyPrint(glueException.toDocument()));
        	entity = "XML error handling not implemented";
        }
		return Response.status(500).entity(entity).type(acceptType).build();
	}
}