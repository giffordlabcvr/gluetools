package uk.ac.gla.cvr.gluetools.core.collation.populating.joinTableLinkUpdater;

public class JoinTableLinkUpdate {

	private String joinTableName;
	private String destTableName;
	private String newJoinRowId;
	private String destRowId;
	
	public JoinTableLinkUpdate(String joinTableName, String destTableName, String newJoinRowId, String destRowId) {
		super();
		this.joinTableName = joinTableName;
		this.destTableName = destTableName;
		this.newJoinRowId = newJoinRowId;
		this.destRowId = destRowId;
	}

	public String getJoinTableName() {
		return joinTableName;
	}

	public String getDestTableName() {
		return destTableName;
	}

	public String getNewJoinRowId() {
		return newJoinRowId;
	}

	public String getDestRowId() {
		return destRowId;
	}
	
}
