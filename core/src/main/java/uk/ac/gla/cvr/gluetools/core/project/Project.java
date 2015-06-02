package uk.ac.gla.cvr.gluetools.core.project;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcer;
import uk.ac.gla.cvr.gluetools.core.datafield.DataField;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulator;

public class Project {

	private String id;
	
	private String displayName;
	
	private List<SequenceSourcer> sequenceSourcers;
	
	private List<DataField<?>> dataFields;
	
	private List<DataFieldPopulator> dataFieldPopulators;
	
	private List<CollatedSequence> collatedSequences;
	
}
