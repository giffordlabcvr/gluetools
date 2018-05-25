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
package uk.ac.gla.cvr.gluetools.core.samFileGenerator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.translation.ResidueUtils;

public abstract class BaseSamReadSet implements Plugin {

	public abstract void writeReads(CommandContext cmdContext, SAMFileHeader samFileHeader, SamFileGenerator samFileGenerator, SAMFileWriter samFileWriter);

	protected String deAmbiguizeNts(String nts) {
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < nts.length(); i++) {
			int ambigNtInt = ResidueUtils.ambigNtToInt(nts.charAt(i));
			int[] concreteNtInts = ResidueUtils.ambigNtToConcreteNts(ambigNtInt);
			buf.append(ResidueUtils.intToConcreteNt(concreteNtInts[0]));
		}
		return buf.toString();
	}
	
}
