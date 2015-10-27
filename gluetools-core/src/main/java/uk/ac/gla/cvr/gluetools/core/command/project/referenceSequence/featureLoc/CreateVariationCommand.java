package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.Optional;
import java.util.regex.Pattern;

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
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;


@CommandClass( 
		commandWords={"create","variation"}, 
		docoptUsages={"<variationName> <refStart> <refEnd> [-t <type>] <regex> [<description>]"},
		docoptOptions={"-t <type>, --transcriptionType <type>  Possible values: [NUCLEOTIDE, AMINO_ACID]"},
		metaTags={CmdMeta.updatesDatabase},
		description="Create a new feature variation", 
		furtherHelp="A variation is a regular expression defining a known motif which may occur in a query sequence. "+
		"Note that the meaning of refStart and refEnd are dependent on the transcription type and on the feature location: "+
		"For variations of type NUCLEOTIDE, refStart and refEnd define simply the NT region of the reference sequence to "+
		"which the motif should be aligned. For variations of type AMINO_ACID, refStart and refEnd define the codon-numbered "+
		"region to which the query should be aligned, based on the numbering scheme of the smallest-scope feature ancestor of "+
		"this variation which has its own codon numbering.") 
public class CreateVariationCommand extends FeatureLocModeCommand<CreateResult> {

	private static final String REF_END = "refEnd";
	private static final String REF_START = "refStart";
	public static final String VARIATON_NAME = "variationName";
	public static final String TRANSCRIPTION_TYPE = "transcriptionType";
	public static final String REGEX = "regex";
	public static final String DESCRIPTION = "description";
	
	private String variationName;
	private Integer refStart;
	private Integer refEnd;
	private TranslationFormat translationFormat;
	private Pattern regex;
	private Optional<String> description;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		variationName = PluginUtils.configureStringProperty(configElem, VARIATON_NAME, true);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, true);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, true);
		
		translationFormat = Optional.ofNullable(
				PluginUtils.configureEnumProperty(TranslationFormat.class, configElem, TRANSCRIPTION_TYPE, false)).
				orElse(TranslationFormat.NUCLEOTIDE);
		regex = PluginUtils.configureRegexPatternProperty(configElem, REGEX, true);

		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		if(refStart > refEnd) {
			throw new VariationException(Code.VARIATION_ENDPOINTS_REVERSED, 
					getRefSeqName(), getFeatureName(), variationName, Integer.toString(refStart), Integer.toString(refEnd));
		}
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);
		
		Variation variation = GlueDataObject.create(cmdContext, 
				Variation.class, Variation.pkMap(
						featureLoc.getReferenceSequence().getName(), 
						featureLoc.getFeature().getName(), variationName), false);
		variation.setFeatureLoc(featureLoc);
		variation.setRefStart(refStart);
		variation.setRefEnd(refEnd);
		variation.setTranscriptionType(translationFormat.name());
		variation.setRegex(regex.pattern());
		description.ifPresent(d -> {variation.setDescription(d);});
		cmdContext.commit();
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
