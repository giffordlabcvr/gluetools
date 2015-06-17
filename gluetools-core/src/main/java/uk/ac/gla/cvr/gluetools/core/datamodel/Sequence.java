package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;

@GlueDataClass(listColumnHeaders = {"ID", "Sourcer", "SourcerID"})
public class Sequence extends _Sequence {

	@Override
	public String[] populateListRow() {
		return new String[]{
				getObjectId().getIdSnapshot().get(ID_PK_COLUMN).toString(), 
				Optional.ofNullable(getSourcer()).map(Sourcer::getName).orElse("n/a"), 
				Optional.ofNullable(getSourcerId()).orElse("n/a"), 
		};
	}
	
	public static Sequence lookupSequence(ObjectContext objContext, String sequenceId) {
		Map<String, String> idMap = Collections.singletonMap(Sequence.ID_PK_COLUMN, sequenceId);
		Sequence sequence = Cayenne.objectForPK(objContext, Sequence.class, idMap);
		if(sequence == null) {
			throw new DataModelException(Code.OBJECT_NOT_FOUND, Sequence.class.getSimpleName(), idMap);
		}
		return sequence;
	}


}
