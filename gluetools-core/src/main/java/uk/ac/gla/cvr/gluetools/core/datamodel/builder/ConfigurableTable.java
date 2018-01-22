/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.datamodel.builder;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ModePathElement;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

// tables within a project where fields can be added / deleted.
public enum ConfigurableTable { 
	sequence(Sequence.class, 
			ModelBuilder.keyword("sequence"), 
			ModelBuilder.pkPath(Sequence.SOURCE_NAME_PATH), 
			ModelBuilder.pkPath(Sequence.SEQUENCE_ID_PROPERTY)),
	variation(Variation.class, 
			ModelBuilder.keyword("reference"), 
			ModelBuilder.pkPath(Variation.REF_SEQ_NAME_PATH), 
			ModelBuilder.keyword("feature-location"), 
			ModelBuilder.pkPath(Variation.FEATURE_NAME_PATH), 
			ModelBuilder.keyword("variation"), 
			ModelBuilder.pkPath(Variation.NAME_PROPERTY)),
	feature_location(FeatureLocation.class, 
			ModelBuilder.keyword("reference"), 
			ModelBuilder.pkPath(FeatureLocation.REF_SEQ_NAME_PATH), 
			ModelBuilder.keyword("feature-location"), 
			ModelBuilder.pkPath(FeatureLocation.FEATURE_NAME_PATH)), 
	feature(Feature.class, 
			ModelBuilder.keyword("feature"), 
			ModelBuilder.pkPath(Feature.NAME_PROPERTY)),
	alignment(Alignment.class, 
			ModelBuilder.keyword("alignment"), 
			ModelBuilder.pkPath(Alignment.NAME_PROPERTY)),
	reference(ReferenceSequence.class, 
			ModelBuilder.keyword("reference"), 
			ModelBuilder.pkPath(ReferenceSequence.NAME_PROPERTY)),
	alignment_member(AlignmentMember.class, 
			ModelBuilder.keyword("alignment"), 
			ModelBuilder.pkPath(AlignmentMember.ALIGNMENT_NAME_PATH), 
			ModelBuilder.keyword("member"), 
			ModelBuilder.pkPath(AlignmentMember.SOURCE_NAME_PATH),
			ModelBuilder.pkPath(AlignmentMember.SEQUENCE_ID_PATH)),
	var_almt_note(VarAlmtNote.class, 
			ModelBuilder.keyword("reference"), 
			ModelBuilder.pkPath(VarAlmtNote.REF_SEQ_NAME_PATH), 
			ModelBuilder.keyword("feature-location"), 
			ModelBuilder.pkPath(VarAlmtNote.FEATURE_NAME_PATH), 
			ModelBuilder.keyword("variation"), 
			ModelBuilder.pkPath(VarAlmtNote.VARIATION_NAME_PATH),
			ModelBuilder.keyword("var-almt-note"), 
			ModelBuilder.pkPath(VarAlmtNote.ALIGNMENT_NAME_PATH)),
	member_floc_note(MemberFLocNote.class, 
			ModelBuilder.keyword("alignment"), 
			ModelBuilder.pkPath(MemberFLocNote.ALIGNMENT_NAME_PATH), 
			ModelBuilder.keyword("member"), 
			ModelBuilder.pkPath(MemberFLocNote.SOURCE_NAME_PATH),
			ModelBuilder.pkPath(MemberFLocNote.SEQUENCE_ID_PATH),
			ModelBuilder.keyword("member-floc-note"), 
			ModelBuilder.pkPath(MemberFLocNote.REF_SEQ_NAME_PATH),
			ModelBuilder.pkPath(MemberFLocNote.FEATURE_NAME_PATH));
	
	Class<? extends GlueDataObject> dataObjectClass;
	private ModePathElement[] modePath;

	private ConfigurableTable(Class <? extends GlueDataObject> dataObjectClass, ModePathElement ... modePath) {
		this.dataObjectClass = dataObjectClass;
		this.modePath = modePath;
	}

	public Class<? extends GlueDataObject> getDataObjectClass() {
		return dataObjectClass;
	}
	
	public ModePathElement[] getModePath() {
		return modePath;
	}
	
}