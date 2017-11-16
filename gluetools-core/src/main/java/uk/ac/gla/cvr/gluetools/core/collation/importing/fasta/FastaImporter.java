package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.AccessionID;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.io.template.SequenceHeaderParserInterface;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.SequenceImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaFieldParser.Result;
import uk.ac.gla.cvr.gluetools.core.collation.populating.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaImporter",
	description="Imports nucleotide data from a FASTA file, creating a set of Sequence objects")

public class FastaImporter extends SequenceImporter<FastaImporter> implements PropertyPopulator {

	private static final String SKIP_EXISTING_SEQUENCES = "skipExistingSequences";
	private static final String SOURCE_NAME = "sourceName";
	
	private Pattern nullRegex = null;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters = null;
	private String sourceName;
	private List<FastaFieldParser> fieldParsers;
	private boolean skipExistingSequences = false;

	public FastaImporter() {
		super();
		addModulePluginCmdClass(ImportCommand.class);
		addSimplePropertyName(SKIP_EXISTING_SEQUENCES);
		addSimplePropertyName(SOURCE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = Optional.ofNullable(PluginUtils.
				configureStringProperty(configElem, SOURCE_NAME, false)).orElse("local");
		skipExistingSequences = Optional.ofNullable(PluginUtils.
				configureBooleanProperty(configElem, SKIP_EXISTING_SEQUENCES, false)).orElse(false);
		List<Element> idParserElems = PluginUtils.findConfigElements(configElem, "idParser", 0, 1);
		if(!idParserElems.isEmpty()) {
			Element idParserElem  = idParserElems.get(0);
			nullRegex = Optional.ofNullable(
					PluginUtils.configureRegexPatternProperty(idParserElem, "nullRegex", false)).
					orElse(Pattern.compile(PropertyPopulator.DEFAULT_NULL_REGEX));
			valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
					PluginUtils.findConfigElements(idParserElem, "valueConverter"));
			mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, idParserElem);
		}
		List<Element> fieldParserElems = PluginUtils.findConfigElements(configElem, "fieldParser");
		fieldParsers = PluginFactory.createPlugins(pluginConfigContext, FastaFieldParser.class, fieldParserElems);
	}

	public CreateResult doImport(ConsoleCommandContext cmdContext, String fileName) {
		byte[] fastaBytes = cmdContext.loadBytes(fileName);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		HeaderParser headerParser = new HeaderParser();
		Map<String, DNASequence> idToSequence = FastaUtils.parseFasta(fastaBytes, headerParser);
		ensureSourceExists(cmdContext, sourceName);
		idToSequence.forEach((id, seq) -> {
			if(skipExistingSequences && sequenceExists(cmdContext, sourceName, id)) {
				return;
			}
			String sequenceAsString = seq.getSequenceAsString();
			String seqString = ">"+id+"\n"+sequenceAsString+"\n";
			createSequence(cmdContext, sourceName, id, SequenceFormat.FASTA, seqString.getBytes());
			
			try (ModeCloser seqMode = cmdContext.pushCommandMode("sequence", sourceName, id);){
				seq.getUserCollection().forEach(obj -> {
					FastaFieldParser.Result result = (Result) obj;
					SequencePopulator.runSetFieldCommand(cmdContext, result.getFieldPopulator(), result.getFieldValue(), false);
				});
			}
		});
		return new CreateResult(Sequence.class, idToSequence.keySet().size());
	}

	
	private class HeaderParser implements SequenceHeaderParserInterface<DNASequence, NucleotideCompound> {

		@Override
		public void parseHeader(String header, DNASequence sequence) {
			String finalID = SequencePopulator.runPropertyPopulator(FastaImporter.this, header);
			if(finalID == null) {
				throw new FastaImporterException(FastaImporterException.Code.NULL_IDENTIFIER, header);
			}
			sequence.setAccession(new AccessionID(finalID));
			Collection<Object> fieldParserResults = 
					fieldParsers.stream()
					.map(fParser -> fParser.parseField(header))
					.filter(Optional::isPresent)
				    .map(Optional::get)
				    .collect(Collectors.toList());
			sequence.setUserCollection(fieldParserResults);
		}
	}
	
	
	@CommandClass( 
			commandWords={"import"}, 
			docoptUsages={"-f <fileName>"},
			docoptOptions={
				"-f <fileName>, --fileName <fileName>  FASTA file"},
			description="Import sequences from a FASTA file", 
			metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
			furtherHelp="The file is loaded from a location relative to the current load/save directory.") 
	public static class ImportCommand extends ModulePluginCommand<CreateResult, FastaImporter> implements ProvidedProjectModeCommand {

		private String fileName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		}
		
		@Override
		protected CreateResult execute(CommandContext cmdContext, FastaImporter importerPlugin) {
			return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerPathLookup("fileName", false);
			}
		}

	}
		

	@Override
	public RegexExtractorFormatter getMainExtractor() {
		return mainExtractor;
	}

	@Override
	public List<RegexExtractorFormatter> getValueConverters() {
		return valueConverters;
	}

	@Override
	public Pattern getNullRegex() {
		return nullRegex;
	}

}
