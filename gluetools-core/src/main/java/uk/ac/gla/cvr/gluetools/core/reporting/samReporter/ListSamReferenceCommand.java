package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.io.IOException;
import java.util.List;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"list", "sam-reference"}, 
		description = "Provide basic information about SAM references", 
				docoptUsages = { "-i <fileName>" },
		docCategory = "Type-specific module commands",
		docoptOptions = { 
						"-i <fileName>, --fileName <fileName>                    SAM/BAM input file",
				},
		metaTags = {CmdMeta.consoleOnly}	
)
public class ListSamReferenceCommand extends ModulePluginCommand<ListSamReferenceResult, SamReporter> {

	public static final String FILE_NAME = "fileName";
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}
	
	@Override
	protected ListSamReferenceResult execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
        try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, fileName, 
				samReporter.getSamReaderValidationStringency())) {
        	List<SAMSequenceRecord> samSequenceRecords = samReader.getFileHeader().getSequenceDictionary().getSequences();
        	return new ListSamReferenceResult(samSequenceRecords);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}
	
}
