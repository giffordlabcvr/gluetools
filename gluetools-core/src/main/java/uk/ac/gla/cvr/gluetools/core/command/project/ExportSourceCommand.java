package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
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
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;


@CommandClass( 
	commandWords={"export", "source"}, 
	docoptUsages={
		"[-b <batchSize>] [-p <parentDir>] <sourceName> [-w <whereClause>] [-t <idTemplate>]"
	}, 
	docoptOptions={
		"-b <batchSize>, --batchSize <batchSize>        Batch size [default: 250]",
		"-p <parentDir>, --parentDir <parentDir>        Parent directory",
		"-w <whereClause>, --whereClause <whereClause>  Qualify exported sequences",
		"-t <idTemplate>, --idTemplate <idTemplate>     Freemarker template to use as ID",
	},
	metaTags = { CmdMeta.consoleOnly },
	furtherHelp=
			"Saves sequences to a directory called <sourceName>. "+
			"If <parentDir> is provided, the directory will be located inside <parentDir>. "+
			"Otherwise it will be located inside the current load-save-path directory. "+
			"If the directory doesn't exist it will be created by the command."+
			"This sequence data, one file per sequence, will be written to the files in the directory. "+
			"Saved sequences will overwrite existing sequences in the directory. "+
			"The first part of the "+
			"sequence file name will be the sequenceID, unless --idTemplate is specified, in which case this Freemarker template "+
			"will be used to form the first part of the file name. "+
			"This option can thereby be used to redefine the sequenceID for a set of sequences. "+
			"The file extension will be the standard file "+
			"extension for the sequence format, as specified in the \"list format sequence\" command output. "+
			"Sequences are retrieved from the database in batches. The <batchSize> option controls the size "+
			"of each batch.",
	description="Export sequences to files in a source dir") 
public class ExportSourceCommand extends ProjectModeCommand<ExportSourceResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String BATCH_SIZE = "batchSize";
	public static final String PARENT_DIR = "parentDir";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ID_TEMPLATE = "idTemplate";


	private String sourceName;
	private String parentDir;
	private Integer batchSize;
	private Optional<Expression> whereClause;
	private Template idTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		parentDir = PluginUtils.configureStringProperty(configElem, PARENT_DIR, false);
		idTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, ID_TEMPLATE, false);
	}

	@Override
	public ExportSourceResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		File parentDirFile = consoleCmdContext.getLoadSavePath();

		if(parentDir != null) {
			parentDirFile = consoleCmdContext.fileStringToFile(parentDir);
		}
		GlueLogger.getGlueLogger().fine("Finding sequences in source "+sourceName);
		Expression exp = ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, sourceName);
		if(whereClause.isPresent()) {
			exp = exp.andExp(whereClause.get());
		}
		List<Sequence> sequences = 
				GlueDataObject.query(cmdContext, Sequence.class, new SelectQuery(Sequence.class, exp));
		List<Map<String, String>> pkMaps = sequences
				.stream().map(seq -> seq.pkMap())
				.collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Found "+pkMaps.size()+" sequences.");
		
		int exported = 0;
		File sourceDirFile = new File(parentDirFile, sourceName);
		
		consoleCmdContext.mkdirs(sourceDirFile);
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		for(Map<String, String> pkMap: pkMaps) {
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, pkMap);
			String sequenceID = sequence.getSequenceID();
			SequenceFormat seqFormat = sequence.getSequenceFormat();
			String fileName = sequenceID;
			if(idTemplate != null) {
				fileName = FreemarkerUtils.processTemplate(idTemplate, FreemarkerUtils.templateModelForObject(sequence));
			}
			File filePath = new File(sourceDirFile, fileName+"."+seqFormat.getGeneratedFileExtension(cmdContext));
			if(consoleCmdContext.isFile(filePath.toString())) {
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
			if( exported % batchSize == 0) {
				GlueLogger.getGlueLogger().fine("Sequences exported: "+exported);
				cmdContext.newObjectContext(); // ensure processed sequence objects are GC'd
			}
		}
		GlueLogger.getGlueLogger().fine("Sequences exported: "+exported);
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
			registerPathLookup("parentDir", true);
		}
	}

}
