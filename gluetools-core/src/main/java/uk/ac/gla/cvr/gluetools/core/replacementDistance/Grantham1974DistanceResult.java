package uk.ac.gla.cvr.gluetools.core.replacementDistance;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class Grantham1974DistanceResult extends MapResult {
	public Grantham1974DistanceResult(double distance) {
		super("grantham1974DistanceResult", 
				mapBuilder().
					put("distanceDouble", distance).
					put("distanceInt", (int)Math.round(distance)));
	}
}
