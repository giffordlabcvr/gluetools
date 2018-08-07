/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
		registerModulePluginCmdClass(LoadAminoAcidFastaCommand.class);
		registerModulePluginCmdClass(SaveAminoAcidFastaCommand.class);
		setCmdGroup(new CommandGroup("string-operations", "Operations on FASTA strings", 91, false));
		registerModulePluginCmdClass(ReverseComplementFastaStringCommand.class);
	}

}
