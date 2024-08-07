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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords={"show", "reference"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the name of the alignment's reference sequence"
)
public class AlignmentShowReferenceSequenceCommand extends AlignmentModeCommand<AlignmentShowReferenceSequenceCommand.ShowReferenceResult> {

	
	@Override
	public ShowReferenceResult execute(CommandContext cmdContext) {
		return new ShowReferenceResult(lookupAlignment(cmdContext).getRefSequence());
	}

	public static class ShowReferenceResult extends MapResult {

		public ShowReferenceResult(ReferenceSequence refSequence) {
			super("showReferenceResult", mapBuilder()
					.put("referenceName", refSequence != null ? refSequence.getName() : null)
					.put("referenceRenderedName", refSequence != null ? refSequence.getRenderedName() : null)
				);
		}

		public String getReferenceName() {
			return getCommandDocument().getString("referenceName");
		}
		
	}
}
