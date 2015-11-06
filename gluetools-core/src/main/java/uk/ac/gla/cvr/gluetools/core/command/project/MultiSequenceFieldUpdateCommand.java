package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class MultiSequenceFieldUpdateCommand extends ProjectModeCommand<UpdateResult> {

	public static final String BATCH_SIZE = "batchSize";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_SEQUENCES = "allSequences";
	public static final String FIELD_NAME = "fieldName";

	private String fieldName;
	private Boolean allSequences;
	private Optional<Expression> whereClause;
	private int batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		allSequences = PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);
		if( !allSequences && !whereClause.isPresent() ) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allSequences must be specified");
	}

	
	@Override
	public final UpdateResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		project.checkValidCustomSequenceFieldNames(Collections.singletonList(fieldName));

		SelectQuery selectQuery = null;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(Sequence.class, whereClause.get());
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		GlueLogger.getGlueLogger().fine("Finding sequences to update");
		List<Sequence> sequencesToUpdate = 
				GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
		List<Map<String, String>> sequencePkMaps = sequencesToUpdate.stream().map(seq -> seq.pkMap()).collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Found "+sequencePkMaps.size()+" sequences");

		int numUpdated = 0;
		for(Map<String, String> sPkMap: sequencePkMaps) {
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, sPkMap);
			updateSequence(cmdContext, sequence, fieldName);
			numUpdated++;
			if(numUpdated % batchSize == 0) {
				cmdContext.commit();
				cmdContext.newObjectContext();
				GlueLogger.getGlueLogger().finest("Updated "+numUpdated+" sequences");
			}
		}
		cmdContext.commit();
		cmdContext.newObjectContext();
		GlueLogger.getGlueLogger().finest("Updated "+numUpdated+" sequences");
		return new UpdateResult(Sequence.class, numUpdated);
	}

	protected abstract void updateSequence(CommandContext cmdContext, Sequence sequence, String fieldName);
	
}
