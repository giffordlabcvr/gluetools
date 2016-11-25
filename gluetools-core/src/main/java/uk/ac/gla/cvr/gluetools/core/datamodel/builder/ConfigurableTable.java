package uk.ac.gla.cvr.gluetools.core.datamodel.builder;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ModePathElement;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
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
			ModelBuilder.pkPath(VarAlmtNote.ALIGNMENT_NAME_PATH));
	
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