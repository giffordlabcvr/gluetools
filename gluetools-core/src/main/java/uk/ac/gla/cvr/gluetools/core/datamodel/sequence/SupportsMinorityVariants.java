package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.ArrayList;
import java.util.List;

/**
 * A sequence object class implements this interface if it is capable of representing
 * genetic variation within a sample.
 */
public interface SupportsMinorityVariants<A extends AbstractSequenceObject, F extends MinorityVariantFilter<A>> {

	@SuppressWarnings("unchecked")
	public default List<NtMinorityVariant> getMinorityVariants(int ntStartIndex, int ntEndIndex, F filter) {
		List<NtMinorityVariant> minorityVariants = new ArrayList<NtMinorityVariant>();
		for(int i = ntStartIndex; i <= ntEndIndex; i++) {
			minorityVariants.addAll(filter.getMinorityVariants(i, (A) this));
		}
		return minorityVariants;
	}
	
}
