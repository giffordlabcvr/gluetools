package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;

public class FeaturePresence {

	private FeatureLocation featureLoc;
	private Integer membersWherePresent;
	private Integer totalMembers;
	
	public FeaturePresence(FeatureLocation featureLoc, Integer membersWherePresent, Integer totalMembers) {
		super();
		this.featureLoc = featureLoc;
		this.membersWherePresent = membersWherePresent;
		this.totalMembers = totalMembers;
	}

	public FeatureLocation getFeatureLoc() {
		return featureLoc;
	}

	public Integer getMembersWherePresent() {
		return membersWherePresent;
	}

	public Double getPercentWherePresent() {
		if(totalMembers.equals(0)) {
			return null;
		}
		return ( membersWherePresent * 100.0) / new Double(totalMembers);
	}

	public Integer getTotalMembers() {
		return totalMembers;
	}
	
	public void incrementMembersWherePresent() {
		this.membersWherePresent++;
	}
}
