package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;


@CommandClass( 
		commandWords={"create","variation"}, 
		docoptUsages={"[-C] <variationName> [-t <type>] [<description>]"},
		docoptOptions={
				"-C, --noCommit     Don't commit to the database [default: false]",
				"-t <type>, --translationType <type>  Possible values: [NUCLEOTIDE, AMINO_ACID]"
		},
		metaTags={CmdMeta.updatesDatabase},
		description="Create a new feature variation", 
		furtherHelp="A variation is a known motif which may occur in a sequence aligned to a reference. "+
		"The <type> of the variation defines whether its regular expression pattern is matched against the "+
		"nucleotides in the sequence or against the amino acid translation. If omitted, NUCLEOTIDE is the default.") 
public class CreateVariationCommand extends FeatureLocModeCommand<CreateResult> {

	public static final String NO_COMMIT = "noCommit";
	public static final String VARIATION_NAME = "variationName";
	public static final String DESCRIPTION = "description";
	public static final String TRANSLATION_TYPE = "translationType";

	private Boolean noCommit;
	private String variationName;
	private Optional<String> description;
	private TranslationFormat translationFormat;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
		variationName = PluginUtils.configureStringProperty(configElem, VARIATION_NAME, true);
		translationFormat = Optional.ofNullable(
				PluginUtils.configureEnumProperty(TranslationFormat.class, configElem, TRANSLATION_TYPE, false)).
				orElse(TranslationFormat.NUCLEOTIDE);
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);
		
		Variation variation = GlueDataObject.create(cmdContext, 
				Variation.class, Variation.pkMap(
						featureLoc.getReferenceSequence().getName(), 
						featureLoc.getFeature().getName(), variationName), false);
		variation.setFeatureLoc(featureLoc);
		variation.setTranslationType(translationFormat.name());
		description.ifPresent(d -> {variation.setDescription(d);});
		if(noCommit) {
			cmdContext.cacheUncommitted(variation);
		} else {
			cmdContext.commit();
		}
		return new CreateResult(Variation.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("type", TranslationFormat.class);
		}
	}

}
