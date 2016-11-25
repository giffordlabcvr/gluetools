package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"copy","sequence"}, 
		docoptUsages={"<toSourceName> (-w <whereClause> | -a) [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify copy sequences",
				"-a, --allSequences                             Copy all sequences",
				"-b <batchSize>, --batchSize <batchSize>        Sequence copy batch size"},
		description="Copy sequences to a specific source", 
		furtherHelp="If sequences are not already in the destination source, they will be copied there. "+
		"Sequences are skipped if a sequence exists in the destination with the same sequence ID. "+
		"Copies are committed to the database in batches. Default batch size is 250.") 
public class CopySequenceCommand extends ProjectModeCommand<CopySequenceCommand.CopySequenceResult> {

		private String toSourceName;
		private Boolean allSequences;
		private Optional<Expression> whereClause;
		private Integer batchSize;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			toSourceName = PluginUtils.configureStringProperty(configElem, "toSourceName", false);
			allSequences = PluginUtils.configureBooleanProperty(configElem, "allSequences", true);
			whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false));
			batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "batchSize", false)).orElse(250);
			if( !allSequences && !whereClause.isPresent()) {
				usageError();
			}
		}

		private void usageError() {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allSequences or must be specified");
		}
		
		@Override
		public CopySequenceResult execute(CommandContext cmdContext) {
			List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
			SelectQuery selectQuery = null;
			Expression exp = ExpressionFactory.noMatchExp(Sequence.SOURCE_NAME_PATH, toSourceName);
			if(whereClause.isPresent()) {
				exp = exp.andExp(whereClause.get());
			}
			selectQuery = new SelectQuery(Sequence.class, exp);
			GlueLogger.getGlueLogger().fine("Finding sequences to copy");
			List<Sequence> sequencesToMove = 
					GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			List<Map<String, String>> sequencePkMaps = sequencesToMove.stream().map(seq -> seq.pkMap()).collect(Collectors.toList());
			GlueLogger.getGlueLogger().fine("Found "+sequencePkMaps.size()+" sequences");
			Source toSource = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(toSourceName));
			int numCopied = 0;
			Set<String> copiedIDs = new LinkedHashSet<String>();
			for(Map<String, String> sPkMap: sequencePkMaps) {
				if(toSource == null) {
					toSource = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(toSourceName));
				}
				Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, sPkMap);
				String sequenceID = sequence.getSequenceID();
				Map<String, String> existingPkMap = Sequence.pkMap(toSourceName, sequenceID);
				if(copiedIDs.contains(sequenceID)) {
					GlueLogger.getGlueLogger().fine("Skipping sequence, ID exists: "+existingPkMap);
					continue;
				}
				Sequence existing = GlueDataObject.lookup(cmdContext, Sequence.class, existingPkMap, true);
				if(existing != null) {
					GlueLogger.getGlueLogger().fine("Skipping sequence, ID exists: "+existingPkMap);
					continue;
				}
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				row.put("sequenceID", sequenceID);
				row.put("fromSourceName", sequence.getSource().getName());
				row.put("toSourceName", toSource.getName());
				rowData.add(row);
				List<String> customFieldNames = getProjectMode(cmdContext).getProject().getCustomFieldNames(ConfigurableTable.sequence.name());
				Map<String, Object> customFieldValues = new LinkedHashMap<String, Object>();
				for(String fieldName: customFieldNames) {
					customFieldValues.put(fieldName, sequence.readProperty(fieldName));
				}
				byte[] origData = sequence.getOriginalData();
				SequenceFormat seqFormat = sequence.getSequenceFormat();
				Sequence newSequence = CreateSequenceCommand.createSequence(cmdContext, toSource.getName(), sequenceID, false);
				customFieldValues.forEach((n,v) -> newSequence.writeProperty(n, v));
				newSequence.setFormat(seqFormat.name());
				newSequence.setOriginalData(origData);
				newSequence.setSource(toSource);
				numCopied++;
				copiedIDs.add(sequenceID);
				if(numCopied % batchSize == 0) {
					cmdContext.commit();
					cmdContext.newObjectContext();
					GlueLogger.getGlueLogger().finest("Copied "+numCopied+" sequences");
					toSource = null;
				}
			}
			cmdContext.commit();
			cmdContext.newObjectContext();
			GlueLogger.getGlueLogger().finest("Copied "+numCopied+" sequences");
			return new CopySequenceResult(rowData);
		}

		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerDataObjectNameLookup("toSourceName", Source.class, Source.NAME_PROPERTY);
			}
		}
		
		public static class CopySequenceResult extends TableResult {

			public CopySequenceResult(List<Map<String, Object>> rowData) {
				super("copySequenceResult", Arrays.asList("sequenceID", "fromSourceName","toSourceName"), rowData);
			}
			
		}
		
}