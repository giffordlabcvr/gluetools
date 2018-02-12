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
package uk.ac.gla.cvr.gluetools.core.collation.importing;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSourceCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class SequenceImporter<P extends SequenceImporter<P>> extends ModulePlugin<P> {

	protected final void ensureSourceExists(CommandContext cmdContext, String sourceName) {
		cmdContext.cmdBuilder(CreateSourceCommand.class).
			set(CreateSourceCommand.SOURCE_NAME, sourceName).
			set(CreateSourceCommand.ALLOW_EXISTING, "true").
			execute();
	}
	
	protected final boolean sequenceExists(CommandContext cmdContext, String sourceName, String sequenceID) {
		return GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), true) != null;
	}
	
	protected final Sequence createSequence(CommandContext cmdContext, String sourceName, String sequenceID, 
			SequenceFormat format, byte[] sequenceData) {
		
		Sequence sequence = CreateSequenceCommand.createSequence(cmdContext, sourceName, sequenceID, false);
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName));
		sequence.setSource(source);
		sequence.setFormat(format.name());
		sequence.setOriginalData(sequenceData);
		cmdContext.commit();
		return sequence;
	}
}
