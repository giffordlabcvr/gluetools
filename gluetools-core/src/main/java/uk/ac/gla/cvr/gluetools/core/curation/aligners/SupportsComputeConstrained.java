package uk.ac.gla.cvr.gluetools.core.curation.aligners;

public interface SupportsComputeConstrained {

	@SuppressWarnings("rawtypes")
	public Class<? extends Aligner.AlignCommand> getComputeConstrainedCommandClass();

	public default boolean supportsComputeConstrained() {
		return true;
	}

}
