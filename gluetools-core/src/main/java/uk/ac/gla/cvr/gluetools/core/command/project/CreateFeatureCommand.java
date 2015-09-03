package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionFormat;


@CommandClass( 
	commandWords={"create","feature"}, 
	docoptUsages={"<featureName> [-t <type>] [-p <parent>] [<description>]"},
	docoptOptions={"-t <type>, --transcriptionType <type>  Possible values: [NUCLEOTIDE, AMINO_ACID]",
			       "-p <featureName>, --parentName <featureName>  Name of parent feature"},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new genome feature", 
	furtherHelp="A feature is a named genome region which is of particular interest.") 
public class CreateFeatureCommand extends ProjectModeCommand<CreateResult> {

	public static final String FEATURE_NAME = "featureName";
	public static final String PARENT_NAME = "parentName";
	public static final String DESCRIPTION = "description";
	public static final String TRANSCRIPTION_TYPE = "transcriptionType";
	
	private String featureName;
	private Optional<String> description;
	private Optional<String> parentName;
	private TranscriptionFormat transcriptionFormat;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		parentName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, PARENT_NAME, false));
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
		transcriptionFormat = Optional.ofNullable(
				PluginUtils.configureEnumProperty(TranscriptionFormat.class, configElem, TRANSCRIPTION_TYPE, false)).
				orElse(TranscriptionFormat.NUCLEOTIDE);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Feature feature = GlueDataObject.create(objContext, 
				Feature.class, Feature.pkMap(featureName), false);
		description.ifPresent(d -> {feature.setDescription(d);});
		parentName.ifPresent(pname -> {
			Feature parentFeature = GlueDataObject.lookup(objContext, Feature.class, Feature.pkMap(pname));
			feature.setParent(parentFeature);
		});
		feature.setTranscriptionType(transcriptionFormat.name());
		cmdContext.commit();
		return new CreateResult(Feature.class, 1);
	}

}
