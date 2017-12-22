package uk.ac.gla.cvr.gluetools.core.fastaUtility;

import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="fastaUtility", 
		description="Provides various commands to the scripting layer for processing FASTA data")
public class FastaUtility extends ModulePlugin<FastaUtility>{


	public FastaUtility() {
		super();
		setCmdGroup(new CommandGroup("file-operations", "Commands to operate on FASTA files", 90, false));
		registerModulePluginCmdClass(LoadNucleotideFastaCommand.class);
		registerModulePluginCmdClass(SaveNucleotideFastaCommand.class);
		setCmdGroup(new CommandGroup("string-operations", "Operations on FASTA strings", 91, false));
		registerModulePluginCmdClass(ReverseComplementFastaStringCommand.class);
	}

}
