package uk.ac.gla.cvr.gluetools.utils;

public interface RenderContext {

	public default Integer floatDecimalPlacePrecision() {
		return 2;
	}

}
