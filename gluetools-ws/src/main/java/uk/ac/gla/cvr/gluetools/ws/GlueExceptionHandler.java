/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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