package uk.ac.gla.cvr.gluetools.core.command.render;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.RenderableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.render.defaultRenderer.DefaultObjectRenderer;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.IObjectRenderer;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.ObjectRenderer;


@CommandClass(
		commandWords={"render-object"}, 
		description = "Render current mode object as a document", 
		docoptUsages = { "[<rendererModuleName>]" },
		docoptOptions = { },
		metaTags = { },
		furtherHelp = "The supplied <rendererModuleName> refers to a module implementing the IObjectRenderer interface. "+
		"If no <rendererModuleName> is supplied, a default renderer is used."
)
public class RenderObjectCommand extends Command<CommandResult> {

	public static final String RENDERER_MODULE_NAME = "rendererModuleName";
	
	private String rendererModuleName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		rendererModuleName = PluginUtils.configureStringProperty(configElem, RENDERER_MODULE_NAME, false);
	}

	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		RenderableObjectMode renderableObjectMode = (RenderableObjectMode) cmdContext.peekCommandMode();
		GlueDataObject renderableObject = renderableObjectMode.getRenderableObject(cmdContext);
		IObjectRenderer renderer = null;
		if(rendererModuleName == null) {
			renderer = DefaultObjectRenderer.getDefaultObjectRenderer(renderableObject.getClass());
		} else {
			renderer = ObjectRenderer.getRenderer(cmdContext, rendererModuleName);
		}
		return renderer.render(cmdContext, renderableObject);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("rendererModuleName", Module.class, Module.NAME_PROPERTY);
		}
	}
	
}
