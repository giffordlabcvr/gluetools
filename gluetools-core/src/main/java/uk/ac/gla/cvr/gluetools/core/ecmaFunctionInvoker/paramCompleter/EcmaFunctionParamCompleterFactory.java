package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.paramCompleter;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class EcmaFunctionParamCompleterFactory extends PluginFactory<EcmaFunctionParamCompleter> {

	public static Multiton.Creator<EcmaFunctionParamCompleterFactory> creator = new
			Multiton.SuppliedCreator<>(EcmaFunctionParamCompleterFactory.class, EcmaFunctionParamCompleterFactory::new);
	
	private EcmaFunctionParamCompleterFactory() {
		super();
		registerPluginClass(EcmaFunctionParamPathCompleter.class);
	}
	
}
