package uk.ac.gla.cvr.gluetools.core.http;

import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.http.HttpRunnerException.Code;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"get"}, 
		description = "Run HTTP GET", 
		docoptUsages = { "<urlPath>" }	
)
public class HttpRunnerGetCommand extends ModulePluginCommand<HttpRunnerResult, HttpRunner> {

	public static final String URL_PATH = "urlPath";
	
	private String urlPath;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		String urlPath = PluginUtils.configureStringProperty(configElem, URL_PATH, true);
		if(!urlPath.startsWith("/")) {
			urlPath = "/"+urlPath;
		}
		this.urlPath = urlPath;
	}

	@Override
	protected HttpRunnerResult execute(CommandContext cmdContext, HttpRunner httpRunner) {
        String entityAsString = null;
		try(CloseableHttpClient httpclient = HttpClients.custom().build()) {
	        HttpGet httpget = new HttpGet(httpRunner.getBaseUrl()+urlPath);
	        GlueLogger.getGlueLogger().finest("Executing request " + httpget.getRequestLine());
	        try(CloseableHttpResponse response = httpclient.execute(httpget)) {
	            try {
	                entityAsString = EntityUtils.toString(response.getEntity());
	            } catch(IOException ioe) {
	            	throw new HttpRunnerException(ioe, Code.ENTITY_STRING_ERROR, ioe.getLocalizedMessage());
	            } catch(ParseException pe) {
	            	throw new HttpRunnerException(pe, Code.ENTITY_STRING_ERROR, pe.getLocalizedMessage());
	            }
	        } catch (ClientProtocolException cpe) {
            	throw new HttpRunnerException(cpe, Code.REQUEST_IO_ERROR, cpe.getLocalizedMessage());
			} catch (IOException ioe) {
            	throw new HttpRunnerException(ioe, Code.REQUEST_IO_ERROR, ioe.getLocalizedMessage());
			} 			
		} catch (IOException ioe) {
        	throw new HttpRunnerException(ioe, Code.REQUEST_IO_ERROR, ioe.getLocalizedMessage());
		}
        return new HttpRunnerResult(entityAsString);
    }
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		
	}

}
