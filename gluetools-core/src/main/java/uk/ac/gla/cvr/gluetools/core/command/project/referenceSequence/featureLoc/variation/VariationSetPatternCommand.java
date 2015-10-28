package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;

@CommandClass( 
		commandWords={"set","pattern"}, 
		docoptUsages={"[-t <type>] <regex>"},
		docoptOptions={"-t <type>, --transcriptionType <type>  Possible values: [NUCLEOTIDE, AMINO_ACID]"},
		metaTags={CmdMeta.updatesDatabase},
		description="Set the variation's regular expression and type",
		furtherHelp="The <type> of the variation defines whether the regular expression pttern is matched against the "+
		"nucleotides in the sequence or against the amino acid translation.") 
public class VariationSetPatternCommand extends VariationModeCommand<OkResult> {

	public static final String TRANSCRIPTION_TYPE = "transcriptionType";
	public static final String REGEX = "regex";
	
	private TranslationFormat transcriptionFormat;
	private Pattern regex;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		transcriptionFormat = Optional.ofNullable(
				PluginUtils.configureEnumProperty(TranslationFormat.class, configElem, TRANSCRIPTION_TYPE, false)).
				orElse(TranslationFormat.NUCLEOTIDE);
		regex = PluginUtils.configureRegexPatternProperty(configElem, REGEX, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		variation.setTranscriptionType(transcriptionFormat.name());
		variation.setRegex(regex.pattern());
		cmdContext.commit();
		return new UpdateResult(Variation.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("type", TranslationFormat.class);
		}
	}

	
}
