package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.Optional;

public interface HasDisplayName extends HasName {

	public String getDisplayName();
	
	public default String getRenderedName() {
		return Optional.ofNullable(getDisplayName()).orElse(HasName.super.getRenderedName());
	}

}
