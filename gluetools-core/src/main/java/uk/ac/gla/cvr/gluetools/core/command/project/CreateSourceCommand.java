package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","source"}, 
	docoptUsages={"[-a] <sourceName>"},
	docoptOptions={"-a, --allowExisting  Continue without error if the source already exists."},
	description="Create a new sequence source", 
	furtherHelp="A sequence source is a grouping of sequences where each sequence has a unique ID within the source.") 
public class CreateSourceCommand extends ProjectModeCommand<CreateResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String ALLOW_EXISTING = "allowExisting";
	
	private String sourceName;
	private boolean allowExisting;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		allowExisting = Optional.ofNullable(PluginUtils.
				configureBooleanProperty(configElem, ALLOW_EXISTING, false)).orElse(false);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		GlueDataObject.create(objContext, Source.class, Source.pkMap(sourceName), allowExisting);
		cmdContext.commit();
		return new CreateResult(Source.class, 1);
	}

}
