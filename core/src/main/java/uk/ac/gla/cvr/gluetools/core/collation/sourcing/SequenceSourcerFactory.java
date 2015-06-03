package uk.ac.gla.cvr.gluetools.core.collation.sourcing;

import uk.ac.gla.cvr.gluetools.core.collation.sourcing.ncbi.NCBISequenceSourcer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public class SequenceSourcerFactory extends PluginFactory<SequenceSourcer>{

	private static SequenceSourcerFactory instance;
	
	private SequenceSourcerFactory() {
		super("sequenceSourcer");
		registerPluginClass(NCBISequenceSourcer.TYPE, NCBISequenceSourcer.class);
	}
	
	public static SequenceSourcerFactory getInstance() {
		if(instance == null) {
			instance = new SequenceSourcerFactory();
		}
		return instance;
	}
	
}
