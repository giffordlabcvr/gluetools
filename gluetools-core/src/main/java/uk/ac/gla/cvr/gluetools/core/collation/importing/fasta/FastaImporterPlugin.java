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

import uk.ac.gla.cvr.gluetools.core.collation.importing.ImporterPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaFieldParser.Result;
import uk.ac.gla.cvr.gluetools.core.collation.populating.FieldPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulatorPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="fastaImporter")
public class FastaImporterPlugin extends ImporterPlugin<FastaImporterPlugin> implements FieldPopulator {

	private Pattern nullRegex = null;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters = null;
	private String sourceName;
	private List<FastaFieldParser> fieldParsers;

	
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = Optional.ofNullable(PluginUtils.
				configureStringProperty(configElem, "sourceName", false)).orElse("local");
		List<Element> idParserElems = PluginUtils.findConfigElements(configElem, "idParser", 0, 1);
		if(!idParserElems.isEmpty()) {
			Element idParserElem  = idParserElems.get(0);
			nullRegex = Optional.ofNullable(
					PluginUtils.configureRegexPatternProperty(idParserElem, "nullRegex", false)).
					orElse(Pattern.compile(FieldPopulator.DEFAULT_NULL_REGEX));
			valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
					PluginUtils.findConfigElements(idParserElem, "valueConverter"));
			mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, idParserElem);
		}
		List<Element> fieldParserElems = PluginUtils.findConfigElements(configElem, "fieldParser");
		fieldParsers = PluginFactory.createPlugins(pluginConfigContext, FastaFieldParser.class, fieldParserElems);
		addProvidedCmdClass(ImportCommand.class);
		addProvidedCmdClass(ShowImporterCommand.class);
		addProvidedCmdClass(ConfigureImporterCommand.class);
	}

	public CommandResult doImport(ConsoleCommandContext cmdContext, String fileName) {
		byte[] fastaBytes = cmdContext.loadBytes(fileName);
		HeaderParser headerParser = new HeaderParser();
		Map<String, DNASequence> idToSequence = FastaUtils.parseFasta(fastaBytes, headerParser);
		ensureSourceExists(cmdContext, sourceName);
		idToSequence.forEach((id, seq) -> {
			String seqString = ">"+id+"\n"+seq.getSequenceAsString()+"\n";
			createSequence(cmdContext, sourceName, id, SequenceFormat.FASTA, seqString.getBytes());
			
			ProjectMode projectMode = (ProjectMode) cmdContext.peekCommandMode();
			cmdContext.pushCommandMode(new SequenceMode(projectMode.getProject(), sourceName, id));
			try {
				seq.getUserCollection().forEach(obj -> {
					FastaFieldParser.Result result = (Result) obj;
					SequencePopulatorPlugin.runSetFieldCommand(cmdContext, result.getFieldPopulator(), result.getFieldValue());
				});
			} finally {
				cmdContext.popCommandMode();
			}
		});
		return new CreateResult(Sequence.class, idToSequence.keySet().size());
	}

	
	private class HeaderParser implements SequenceHeaderParserInterface<DNASequence, NucleotideCompound> {

		@Override
		public void parseHeader(String header, DNASequence sequence) {
			String finalID = SequencePopulatorPlugin.runFieldPopulator(FastaImporterPlugin.this, header);
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
			docoptUsages={"-f <file>"},
			docoptOptions={
				"-f <file>, --fileName <file>  FASTA file"},
			description="Import sequences from a FASTA file", 
			furtherHelp="The file is loaded from a location relative to the current load/save directory.") 
	public static class ImportCommand extends ModuleProvidedCommand<FastaImporterPlugin> implements ProvidedProjectModeCommand {

		private String fileName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		}
		
		@Override
		protected CommandResult execute(CommandContext cmdContext, FastaImporterPlugin importerPlugin) {
			return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName);
		}
	}
		

	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this importer") 
	public static class ShowImporterCommand extends ShowConfigCommand<FastaImporterPlugin> {}

	@SimpleConfigureCommandClass(
			propertyNames={"sourceName"}
	)
	public static class ConfigureImporterCommand extends SimpleConfigureCommand<FastaImporterPlugin> {}

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
