package uk.ac.gla.cvr.gluetools.ws;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.ListProjectCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class WsCmdContext extends CommandContext {

	public WsCmdContext(GluetoolsEngine gluetoolsEngine) {
		super(gluetoolsEngine);
		ServerRuntime rootServerRuntime = gluetoolsEngine.getRootServerRuntime();
		RootCommandMode rootCommandMode = new RootCommandMode(rootServerRuntime);
		pushCommandMode(rootCommandMode);
	}
	
	@Path("/project")
	@GET()
	@Produces(MediaType.APPLICATION_JSON)
	public String listProject() {
		return new ListProjectCommand().execute(this).getJsonObject().toString();
	}

	@Path("/project/{projectName}")
	@POST()
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String runCommandInProject(String commandString, @PathParam("projectName") String projectName) {
		Project project = GlueDataObject.lookup(this.getObjectContext(), Project.class, Project.pkMap(projectName));
		ProjectMode projectMode = new ProjectMode(this, project);
		pushCommandMode(projectMode);
		JsonObject jsonObject = JsonUtils.stringToJsonObject(commandString);
		if(jsonObject.keySet().size() != 1) {
			throw new GlueApplicationException("JSON for GLUE command must have exactly one key");
		}
		Map.Entry<String, JsonValue> singleEntry = jsonObject.entrySet().iterator().next();
		String key = singleEntry.getKey();
		JsonValue jsonValue = singleEntry.getValue();
		if(!(jsonValue instanceof JsonObject)) {
			throw new GlueApplicationException("JSON GLUE command value must be a JSON object");
		}
		Element cmdDocElem = XmlUtils.documentWithElement(key);
		JsonUtils.jsonObjectToElement(cmdDocElem, (JsonObject) jsonValue);
		Command command = commandFromElement(cmdDocElem);
		return command.execute(this).getJsonObject().toString();
	}
}
