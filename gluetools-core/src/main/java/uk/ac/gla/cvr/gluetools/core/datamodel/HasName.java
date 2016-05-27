package uk.ac.gla.cvr.gluetools.core.datamodel;

public interface HasName {

	public String getName();
	
	public default String getRenderedName() {
		return getName();
	}
	
}
