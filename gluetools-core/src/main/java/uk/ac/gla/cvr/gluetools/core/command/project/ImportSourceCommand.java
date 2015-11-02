package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ImportSourceCommand.ImportSourceResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"import", "source"}, 
	docoptUsages={
		"[-b <batchSize>] <sourcePath>"
	}, 
	docoptOptions={"-b <batchSize>, --batchSize <batchSize>  Commit batch size [default: 250]"},
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
	furtherHelp=
		"The argument <sourcePath> names a directory, which may be relative to the current load-save-path. "+
	    "The name of the new source will be the name of this directory. "+
		"The directory contains the sequence data, one file per sequence. The first part of the "+
		"sequence file name will become the sequenceID, and the extension will be the standard file "+
		"extension for the sequence format, as specified in the \"list format sequence\" command output. "+
		"The <batchSize> argument allows you to control how often sequences are committed to the database "+
		"during the import. The default is every 250 sequences. A larger <batchSize> means fewer database "+
		"accesses, but requires more Java heap memory.",
	description="Populate source from directory containing sequence files") 
public class ImportSourceCommand extends ProjectModeCommand<ImportSourceResult> {

	public static final String SOURCE_PATH = "sourcePath";
	public static final String BATCH_SIZE = "batchSize";

	private String sourcePath;
	private Integer batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourcePath = PluginUtils.configureStringProperty(configElem, SOURCE_PATH, true);
		batchSize = PluginUtils.configureIntProperty(configElem, BATCH_SIZE, true);
	}

	@Override
	public ImportSourceResult execute(CommandContext cmdContext) {
		
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		File fullPath;
		File sourceFile = new File(sourcePath);
		if(sourceFile.isAbsolute()) {
			fullPath = sourceFile;
		} else {
			fullPath = new File(consoleCmdContext.getLoadSavePath(), sourcePath);
		}
		if(!consoleCmdContext.isDirectory(sourcePath)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No directory "+
					fullPath.getAbsolutePath()+" exists");
		}
		String sourceName = fullPath.getName();
		Source source = GlueDataObject.create(cmdContext, Source.class, Source.pkMap(sourceName), false);

		List<String> fileNames = consoleCmdContext.listMembers(sourcePath, true, false, "");
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		int lastCommitSequencesAdded = 0, sequencesAdded = 0;
		for(String fileName: fileNames) {
			File filePath = new File(fullPath, fileName);
			int lastIndexOfDot = fileName.lastIndexOf('.');
			if(lastIndexOfDot == -1) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "File "+
						filePath.getPath()+" has no extension");
			}
			if(lastIndexOfDot == 0) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Cannot get sequenceID from file "+
						filePath.getPath());
			}
			String sequenceID = fileName.substring(0, lastIndexOfDot);
			String extension = fileName.substring(lastIndexOfDot+1, fileName.length());
			SequenceFormat seqFormat = SequenceFormat.detectFormatFromExtension(extension);
			
			Sequence sequence = CreateSequenceCommand.createSequence(cmdContext, sourceName, sequenceID);
			source = ensureSource(source, cmdContext, sourceName);
			sequence.setSource(source);
			sequence.setFormat(seqFormat.name());
			byte[] sequenceData = ((ConsoleCommandContext) cmdContext).loadBytes(filePath.getPath());
			sequence.setOriginalData(sequenceData);
			Map<String, Object> fileResult = new LinkedHashMap<String, Object>();
			fileResult.put("filePath", filePath.getPath());
			fileResult.put("sourceName", sourceName);
			fileResult.put("sequenceID", sequenceID);
			fileResult.put("sequenceFormat", seqFormat.name());
			rowData.add(fileResult);
			sequencesAdded++;
			if(sequencesAdded % batchSize.intValue() == 0) {
				cmdContext.commit();
				cmdContext.newObjectContext();
				source = null;
				GlueLogger.getGlueLogger().fine("Sequences added: "+sequencesAdded);
				lastCommitSequencesAdded = sequencesAdded;
			}
		}
		if(sequencesAdded != lastCommitSequencesAdded) {
			cmdContext.commit();
			GlueLogger.getGlueLogger().fine("Sequences added: "+sequencesAdded);
		}
		return new ImportSourceResult(rowData);
	}

	public Source ensureSource(Source source, CommandContext cmdContext, String sourceName) {
		if(source != null) {
			return source;
		}
		return GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName), false);
	}
	
	public static class ImportSourceResult extends TableResult {

		public ImportSourceResult(List<Map<String, Object>> rowData) {
			super("importSourceResult", Arrays.asList("filePath", "sourceName", "sequenceID", "sequenceFormat"), rowData);
		}
		
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("sourcePath", true);
		}
	}

	
}
