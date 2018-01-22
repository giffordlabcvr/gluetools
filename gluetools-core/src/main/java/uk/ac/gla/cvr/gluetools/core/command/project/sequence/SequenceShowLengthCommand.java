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
package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;


@CommandClass( 
	commandWords={"show", "length"}, 
	docoptUsages={""},
	description="Show the length of the sequence in nucleotides") 
public class SequenceShowLengthCommand extends SequenceModeCommand<SequenceShowLengthCommand.SequenceShowLengthResult> {

	@Override
	public SequenceShowLengthResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext);
		return new SequenceShowLengthResult(sequence.getSequenceObject().getNucleotides(cmdContext).length());
	}

	public static class SequenceShowLengthResult extends MapResult {

		public SequenceShowLengthResult(int length) {
			super("lengthResult", mapBuilder().put("length", length));
		}
		
		public Integer getLength() {
			return getCommandDocument().getInteger("length");
		}

	}

}
