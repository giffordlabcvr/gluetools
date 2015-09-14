package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionFormat;


@CommandClass( 
	commandWords={"create","variation"}, 
	docoptUsages={"<variationName> <refStart> <refEnd> [-t <type>] <regex> [<description>]"},
	docoptOptions={"-t <type>, --transcriptionType <type>  Possible values: [NUCLEOTIDE, AMINO_ACID]"},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new feature variation", 
	furtherHelp="A variation is a regular expression defining a known motif which may occur at this feature location.") 
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
	private TranscriptionFormat transcriptionFormat;
	private Pattern regex;
	private Optional<String> description;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		variationName = PluginUtils.configureStringProperty(configElem, VARIATON_NAME, true);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, true);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, true);
		
		transcriptionFormat = Optional.ofNullable(
				PluginUtils.configureEnumProperty(TranscriptionFormat.class, configElem, TRANSCRIPTION_TYPE, false)).
				orElse(TranscriptionFormat.NUCLEOTIDE);
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
		Sequence refSequence = featureLoc.getReferenceSequence().getSequence();
		int refSeqLength = refSequence.getSequenceObject().getNucleotides().length();
		if(refStart < 1 || refEnd > refSeqLength) {
			throw new VariationException(Code.VARIATION_LOCATION_OUT_OF_RANGE, 
					getRefSeqName(), getFeatureName(), variationName, 
					Integer.toString(refSeqLength), Integer.toString(refStart), Integer.toString(refEnd));
		}
		ObjectContext objContext = cmdContext.getObjectContext();
		Variation variation = GlueDataObject.create(objContext, 
				Variation.class, Variation.pkMap(
						featureLoc.getReferenceSequence().getName(), 
						featureLoc.getFeature().getName(), variationName), false);
		variation.setFeatureLoc(featureLoc);
		variation.setRefStart(refStart);
		variation.setRefEnd(refEnd);
		variation.setTranscriptionType(transcriptionFormat.name());
		variation.setRegex(regex.pattern());
		description.ifPresent(d -> {variation.setDescription(d);});
		cmdContext.commit();
		return new CreateResult(Variation.class, 1);
	}

}
