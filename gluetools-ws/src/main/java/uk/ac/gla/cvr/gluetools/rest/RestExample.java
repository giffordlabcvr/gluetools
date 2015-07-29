package uk.ac.gla.cvr.gluetools.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/hello")
public class RestExample {

    @GET()
    public String hello() {
    	return "hello: "+this.toString();
    }
}