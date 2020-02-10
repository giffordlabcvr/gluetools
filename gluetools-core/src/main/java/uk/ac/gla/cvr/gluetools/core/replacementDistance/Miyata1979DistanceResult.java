package uk.ac.gla.cvr.gluetools.core.replacementDistance;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class Miyata1979DistanceResult extends MapResult {
	public Miyata1979DistanceResult(double distance) {
		super("miyata1979DistanceResult", 
				mapBuilder().
					put("distance", distance));
	}
}
