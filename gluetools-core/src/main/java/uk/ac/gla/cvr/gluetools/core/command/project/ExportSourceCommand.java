package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
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
		"[-b <batchSize>] <sourceName>"
	}, 
	docoptOptions={"-b <batchSize>, --batchSize <batchSize>  Commit batch size [default: 250]"},
	metaTags = { CmdMeta.consoleOnly },
	furtherHelp=
			"Creates a new directory called <sourceName> relative to the current load-save-path. "+
			"This directory contains the sequence data, one file per sequence. The first part of the "+
			"sequence file name will be the sequenceID, and the extension will be the standard file "+
			"extension for the sequence format, as specified in the \"list format sequence\" command output. "+
			"Sequences are retrieved from the database in batches. The <batchSize> option controls the size "+
			"of each batch.",
	description="Export all source sequences to files") 
public class ExportSourceCommand extends ProjectModeCommand<ExportSourceResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String BATCH_SIZE = "batchSize";

	private String sourceName;
	private Integer batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);
	}

	@Override
	public ExportSourceResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		if(consoleCmdContext.listMembers(false, true, "").contains(sourceName)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Directory "+
					new File(consoleCmdContext.getLoadSavePath(), sourceName).getAbsolutePath()+" already exists");
		}
		consoleCmdContext.mkdirs(sourceName);
		int exported = 0;
		int offset = 0;
		int numFound;
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		do {
			SelectQuery selectQuery = new SelectQuery(Sequence.class, ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, sourceName));
			selectQuery.setFetchOffset(offset);
			selectQuery.setFetchLimit(batchSize);
			List<Sequence> results = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			numFound = results.size();
			for(Sequence sequence: results) {
				String sequenceID = sequence.getSequenceID();
				AbstractSequenceObject sequenceObject = sequence.getSequenceObject();
				byte[] sequenceBytes = sequenceObject.toOriginalData();
				SequenceFormat seqFormat = sequenceObject.getSeqFormat();
				File filePath = new File(sourceName, sequenceID+"."+seqFormat.getStandardFileExtension());
				String filePathString = filePath.getPath();
				consoleCmdContext.saveBytes(filePathString, sequenceBytes);
				Map<String, Object> fileResult = new LinkedHashMap<String, Object>();
				fileResult.put("filePath", filePathString);
				fileResult.put("sourceName", sourceName);
				fileResult.put("sequenceID", sequenceID);
				fileResult.put("sequenceFormat", seqFormat.name());
				rowData.add(fileResult);
			}
			offset += batchSize;
			exported += numFound;
			GlueLogger.getGlueLogger().fine("Exported "+exported+" sequences.");
		} while(numFound >= batchSize);
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
