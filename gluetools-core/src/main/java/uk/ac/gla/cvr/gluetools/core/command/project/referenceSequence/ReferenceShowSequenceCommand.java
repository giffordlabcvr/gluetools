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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords={"show", "sequence"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the source/sequenceID sequence"
)
public class ReferenceShowSequenceCommand extends ReferenceSequenceModeCommand<ReferenceShowSequenceCommand.ReferenceShowSequenceResult> {

	
	@Override
	public ReferenceShowSequenceResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = lookupRefSeq(cmdContext);
		return new ReferenceShowSequenceResult(refSeq.getSequence().getSource().getName(), refSeq.getSequence().getSequenceID());
	}

	public static class ReferenceShowSequenceResult extends MapResult {

		public ReferenceShowSequenceResult(String sourceName, String sequenceID) {
			super("showSequenceResult", mapBuilder()
				.put(ReferenceSequence.SEQ_SOURCE_NAME_PATH, sourceName)
				.put(ReferenceSequence.SEQ_ID_PATH, sequenceID));
		}

		public String getSourceName() {
			return getCommandDocument().getString(ReferenceSequence.SEQ_SOURCE_NAME_PATH);
		}

		public String getSequenceID() {
			return getCommandDocument().getString(ReferenceSequence.SEQ_ID_PATH);
		}

		
	}

}
