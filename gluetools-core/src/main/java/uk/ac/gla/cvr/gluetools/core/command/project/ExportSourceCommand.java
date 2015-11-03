package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ExportSourceCommand.ExportSourceResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"export", "source"}, 
	docoptUsages={
		"[ ( -i | -u ) ] [-b <batchSize>] <sourceName>"
	}, 
	docoptOptions={
		"-i, --incremental                        Add to directory, don't overwrite",
		"-u, --update                             Add to directory, overwrite",
		"-b <batchSize>, --batchSize <batchSize>  Batch size [default: 250]"},
	metaTags = { CmdMeta.consoleOnly },
	furtherHelp=
			"Saves sequences to a directory called <sourceName>, relative to the current load-save-path. "+
			"If the --incremental or --update option is used, the directory may already exist. Otherwise "+
			"it should not exist. If it doesn't exist it will be created by the command."+
			"This sequence data, one file per sequence, will be written to the files in the directory. "+
			"In the --incremental case sequences will not be saved to overwrite existing sequences in the directory. "+
			"In the --update case saved sequences will overwrite existing sequences in the directory. "+
			"The first part of the "+
			"sequence file name will be the sequenceID, and the extension will be the standard file "+
			"extension for the sequence format, as specified in the \"list format sequence\" command output. "+
			"Sequences are retrieved from the database in batches. The <batchSize> option controls the size "+
			"of each batch.",
	description="Export all source sequences to files") 
public class ExportSourceCommand extends ProjectModeCommand<ExportSourceResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String BATCH_SIZE = "batchSize";
	public static final String INCREMENTAL = "incremental";
	public static final String UPDATE = "update";


	private String sourceName;
	private Integer batchSize;
	private Boolean incremental;
	private Boolean update;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);
		incremental = PluginUtils.configureBooleanProperty(configElem, INCREMENTAL, true);
		update = PluginUtils.configureBooleanProperty(configElem, UPDATE, true);
		if(incremental && update) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "May not specify both --incremental and --update");
		}

	}

	@Override
	public ExportSourceResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		if(!update && !incremental && consoleCmdContext.listMembers(false, true, "").contains(sourceName)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Directory "+
					new File(consoleCmdContext.getLoadSavePath(), sourceName).getAbsolutePath()+" already exists");
		}
		GlueLogger.getGlueLogger().fine("Finding sequences in source "+sourceName);
		List<Map<String, String>> pkMaps = 
				GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName))
				.getSequences()
				.stream().map(seq -> seq.pkMap())
				.collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Found "+pkMaps.size()+" sequences.");
		
		int exported = 0;
		int skipped = 0;
		consoleCmdContext.mkdirs(sourceName);
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		for(Map<String, String> pkMap: pkMaps) {
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, pkMap);
			String sequenceID = sequence.getSequenceID();
			SequenceFormat seqFormat = sequence.getSequenceFormat();
			File filePath = new File(sourceName, sequenceID+"."+seqFormat.getStandardFileExtension());
			if(incremental && consoleCmdContext.isFile(filePath.toString())) {
				skipped++;
			} else {
				if(update && consoleCmdContext.isFile(filePath.toString())) {
					consoleCmdContext.delete(filePath.toString());
				}
				AbstractSequenceObject sequenceObject = sequence.getSequenceObject();
				byte[] sequenceBytes = sequenceObject.toOriginalData();
				String filePathString = filePath.getPath();
				consoleCmdContext.saveBytes(filePathString, sequenceBytes);
				Map<String, Object> fileResult = new LinkedHashMap<String, Object>();
				fileResult.put("filePath", filePathString);
				fileResult.put("sourceName", sourceName);
				fileResult.put("sequenceID", sequenceID);
				fileResult.put("sequenceFormat", seqFormat.name());
				rowData.add(fileResult);
				exported ++;
			}
			if( (exported+skipped) % batchSize == 0) {
				GlueLogger.getGlueLogger().fine("Sequences exported: "+exported+ ", skipped: "+skipped);
			}
		}
		GlueLogger.getGlueLogger().fine("Sequences exported: "+exported+ ", skipped: "+skipped);
		return new ExportSourceResult(rowData);
	}

	public static class ExportSourceResult extends TableResult {

		public ExportSourceResult(List<Map<String, Object>> rowData) {
			super("exportSourceResult", Arrays.asList("sourceName", "sequenceID", "sequenceFormat", "filePath"), rowData);
		}
		
	}
		
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
		}
	}

}
