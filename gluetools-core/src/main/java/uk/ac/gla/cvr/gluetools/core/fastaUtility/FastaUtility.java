package uk.ac.gla.cvr.gluetools.core.fastaUtility;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="fastaUtility")
public class FastaUtility extends ModulePlugin<FastaUtility>{


	public FastaUtility() {
		super();
		addModulePluginCmdClass(LoadNucleotideFastaCommand.class);
		addModulePluginCmdClass(SaveNucleotideFastaCommand.class);
		addModulePluginCmdClass(ReverseComplementFastaStringCommand.class);
	}

}