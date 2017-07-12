package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class EcmaFunctionResultTypeFactory extends PluginFactory<EcmaFunctionResultType<?>> {

	public static Multiton.Creator<EcmaFunctionResultTypeFactory> creator = new
			Multiton.SuppliedCreator<>(EcmaFunctionResultTypeFactory.class, EcmaFunctionResultTypeFactory::new);
	
	private EcmaFunctionResultTypeFactory() {
		super();
		registerPluginClass(EcmaFunctionDocumentResultType.class);
		registerPluginClass(EcmaFunctionTableFromObjectsResultType.class);
		registerPluginClass(EcmaFunctionOkFromNullResultType.class);
	}
	
}
