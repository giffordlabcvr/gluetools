package uk.ac.gla.cvr.gluetools.core.jplace;

import java.util.ArrayList;
import java.util.List;

public class JPlaceNamePQuery extends JPlacePQuery {

	private List<String> names = new ArrayList<String>();

	public List<String> getNames() {
		return names;
	}

	public void setNames(List<String> names) {
		this.names = names;
	}
	
}
