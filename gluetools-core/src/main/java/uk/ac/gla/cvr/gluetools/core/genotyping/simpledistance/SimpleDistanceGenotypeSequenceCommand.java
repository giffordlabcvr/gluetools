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
package uk.ac.gla.cvr.gluetools.core.genotyping.simpledistance;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.genotyping.GenotypeSequenceCommand;

@CommandClass(
		commandWords={"genotype", "sequence"}, 
		description = "Genotype one or more stored sequences", 
		docoptUsages = { "(-w <whereClause> | -a) [-l <detailLevel> | -c] [-d <dataDir>]" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>  Qualify the sequences to be genotyped",
				"-a, --allSequences                             Genotype all sequences in the project",
				"-l <detailLevel>, --detailLevel <detailLevel>  Table result detail level",
				"-c, --documentResult                           Output document rather than table result",
				"-d <dataDir>, --dataDir <dataDir>              Save algorithmic data in this directory",
		},
		furtherHelp = "If supplied, <dataDir> must either not exist or be an empty directory",
		metaTags = {}	
)
public class SimpleDistanceGenotypeSequenceCommand extends GenotypeSequenceCommand<SimpleDistanceGenotyper> {


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("dataDir", true);
			registerEnumLookup("detailLevel", DetailLevel.class);
		}
	}
	
}
