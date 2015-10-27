package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ExportSourceCommand.ExportSourceResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"export", "source"}, 
	docoptUsages={
		"<sourceName>"
	}, 
	metaTags = { CmdMeta.consoleOnly },
	furtherHelp=
			"Creates a new directory called <sourceName> relative to the current load-save-path. "+
			"This directory contains the sequence data, one file per sequence. The first part of the "+
			"sequence file name will be the sequenceID, and the extension will be the standard file "+
			"extension for the sequence format, as specified in the \"list format sequence\" command output.",
	description="Export all source sequences to files") 
public class ExportSourceCommand extends ProjectModeCommand<ExportSourceResult> {

	public static final String SOURCE_NAME = "sourceName";

	private String sourceName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
	}

	@Override
	public ExportSourceResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		if(consoleCmdContext.listMembers(false, true, "").contains(sourceName)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Directory "+
					new File(consoleCmdContext.getLoadSavePath(), sourceName).getAbsolutePath()+" already exists");
		}
		consoleCmdContext.mkdirs(sourceName);
		
		SelectQuery selectQuery = new SelectQuery(Sequence.class, ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, sourceName));
		ListResult listResult = CommandUtils.runListCommand(cmdContext, Sequence.class, selectQuery, Arrays.asList(Sequence.SEQUENCE_ID_PROPERTY));
		List<String> seqIDs = listResult.asListOfMaps()
				.stream()
				.map(map -> map.get(Sequence.SEQUENCE_ID_PROPERTY).toString())
				.collect(Collectors.toList());
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		seqIDs.forEach(sequenceID -> {
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), false);
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
		});
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
