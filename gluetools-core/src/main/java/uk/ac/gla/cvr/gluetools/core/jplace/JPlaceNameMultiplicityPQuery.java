package uk.ac.gla.cvr.gluetools.core.jplace;

import java.util.ArrayList;
import java.util.List;

public class JPlaceNameMultiplicityPQuery extends JPlacePQuery {

	private List<JPlaceNameMultiplicity> nameMultiplicities = new ArrayList<JPlaceNameMultiplicity>();

	public List<JPlaceNameMultiplicity> getNameMultiplicities() {
		return nameMultiplicities;
	}

	public void setNameMultiplicities(
			List<JPlaceNameMultiplicity> nameMultiplicities) {
		this.nameMultiplicities = nameMultiplicities;
	}


}
