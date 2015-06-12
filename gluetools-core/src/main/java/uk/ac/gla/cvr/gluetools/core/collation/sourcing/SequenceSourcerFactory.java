package uk.ac.gla.cvr.gluetools.core.collation.sourcing;

import uk.ac.gla.cvr.gluetools.core.collation.sourcing.ncbi.NCBISequenceSourcer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class SequenceSourcerFactory extends PluginFactory<SequenceSourcer>{

	public static Multiton.Creator<SequenceSourcerFactory> creator = new
			Multiton.SuppliedCreator<>(SequenceSourcerFactory.class, SequenceSourcerFactory::new);
	
	private SequenceSourcerFactory() {
		super();
		registerPluginClass(NCBISequenceSourcer.class);
	}
	
}
