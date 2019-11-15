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
 *    aint with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.BaseSamReporterCommand.SamRefInfo;

public class SamMappedReadsResult extends MapResult {

	public SamMappedReadsResult(SamRefInfo samRefInfo, int totalReads,
			int mappedToReference, int fwdMappedToReference, int reverseMappedToReference, 
			int notMappedToReference) {
		super("samMappedReadsResult", mapBuilder()
				.put("samRefName", samRefInfo.getSamRefName())
				.put("samRefIndex", new Integer(samRefInfo.getSamRefIndex()))
				.put("totalReads", new Integer(totalReads))
				.put("mappedToReference", new Integer(mappedToReference))
				.put("fwdMappedToReference", new Integer(fwdMappedToReference))
				.put("reverseMappedToReference", new Integer(reverseMappedToReference))
				.put("notMappedToReference", new Integer(notMappedToReference))
		);
	}
	
}
