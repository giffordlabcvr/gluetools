package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ImportSourceCommand.ImportSourceResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"import", "source"}, 
	docoptUsages={
		"<sourceName>"
	}, 
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
	furtherHelp=
		"The <sourceName> names the new source which will be created. "+ 
		"There must be a directory called <sourceName> relative to the current load-save-path. "+
		"This directory contains the sequence data, one file per sequence. The first part of the "+
		"sequence file name will become the sequenceID, and the extension will be the standard file "+
		"extension for the sequence format, as specified in the \"list format sequence\" command output.",
	description="Populate source from directory containing sequence files") 
public class ImportSourceCommand extends ProjectModeCommand<ImportSourceResult> {

	public static final String SOURCE_NAME = "sourceName";

	private String sourceName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
	}

	@Override
	public ImportSourceResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		if(!consoleCmdContext.listMembers(false, true).contains(sourceName)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No directory "+
					new File(consoleCmdContext.getLoadSavePath(), sourceName).getAbsolutePath()+" exists");
		}
		Source source = GlueDataObject.create(objContext, Source.class, Source.pkMap(sourceName), false);
		List<String> fileNames = consoleCmdContext.listMembers(sourceName, true, false);
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		fileNames.forEach(fileName -> {
			File filePath = new File(sourceName, fileName);
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
			
			Sequence sequence = GlueDataObject.create(objContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), false);
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
		});
		
		cmdContext.commit();
		return new ImportSourceResult(rowData);
	}
	
	public static class ImportSourceResult extends TableResult {

		public ImportSourceResult(List<Map<String, Object>> rowData) {
			super("importSourceResult", Arrays.asList("filePath", "sourceName", "sequenceID", "sequenceFormat"), rowData);
		}
		
	}

}
