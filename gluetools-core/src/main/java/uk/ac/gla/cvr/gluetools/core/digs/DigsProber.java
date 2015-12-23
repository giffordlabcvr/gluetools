package uk.ac.gla.cvr.gluetools.core.digs;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="digsProber")
public class DigsProber extends ModulePlugin<DigsProber> {

	public DigsProber() {
		super();
		addProvidedCmdClass(BuildTargetGroupCommand.class);
	}

	
	
}
