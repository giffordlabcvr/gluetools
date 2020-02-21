package uk.ac.gla.cvr.gluetools.core.collation.populating.joinTableLinkUpdater;

import uk.ac.gla.cvr.gluetools.core.collation.populating.joinTableLinkUpdater.JoinTableLinkException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.LinkUpdateContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public interface JoinTableLinkUpdater {

	
	public static void applyUpdateToDB(CommandContext cmdContext, Sequence sequence, JoinTableLinkUpdate update) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		CustomTable joinTable = project.getCustomTable(update.getJoinTableName());
		if(joinTable == null) {
			throw new JoinTableLinkException(Code.JOIN_TABLE_LINK_UPDATE_FAILED, "Join table '"+update.getJoinTableName()+"' does not exist");
		}
		CustomTable destTable = project.getCustomTable(update.getDestTableName());
		if(destTable == null) {
			throw new JoinTableLinkException(Code.JOIN_TABLE_LINK_UPDATE_FAILED, "Dest table '"+update.getDestTableName()+"' does not exist");
		}
		CustomTableObject joinTableRow = GlueDataObject.create(cmdContext, joinTable.getRowClass(), CustomTableObject.pkMap(update.getNewJoinRowId()), true);
		CustomTableObject destTableRow = GlueDataObject.lookup(cmdContext, destTable.getRowClass(), CustomTableObject.pkMap(update.getDestRowId()), false);
		if(destTableRow == null) {
			throw new JoinTableLinkException(Code.JOIN_TABLE_LINK_UPDATE_FAILED, "No row with ID '"+update.getDestRowId()+"' exists in dest table '"+update.getDestTableName()+"'");
		}
		
		LinkUpdateContext sequenceJoinLUC = new LinkUpdateContext(project, "sequence", update.getJoinTableName());
		LinkUpdateContext.UpdateType.ADD.execute(sequenceJoinLUC, sequence, joinTableRow);

		LinkUpdateContext destJoinLUC = new LinkUpdateContext(project, update.getDestTableName(), update.getJoinTableName());
		LinkUpdateContext.UpdateType.ADD.execute(destJoinLUC, destTableRow, joinTableRow);

		cmdContext.cacheUncommitted(sequence);
		cmdContext.cacheUncommitted(joinTableRow);
		cmdContext.cacheUncommitted(destTableRow);
	}
}
