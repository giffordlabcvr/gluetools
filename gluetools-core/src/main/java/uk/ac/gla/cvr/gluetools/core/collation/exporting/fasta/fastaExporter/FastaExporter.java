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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.fastaExporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.AbstractFastaExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.AbstractSequenceConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.AbstractSequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="fastaExporter", 
description="Exports nucleotide data from a set of Sequence objects to a FASTA file")
public class FastaExporter extends AbstractFastaExporter<FastaExporter> {


	public FastaExporter() {
		super();
		registerModulePluginCmdClass(ExportCommand.class);
		registerModulePluginCmdClass(WebExportCommand.class);
		registerModulePluginCmdClass(ExportMemberCommand.class);
		registerModulePluginCmdClass(WebExportMemberCommand.class);
	}

	public static void doExport(CommandContext cmdContext, 
			AbstractSequenceSupplier sequenceSupplier, 
			AbstractSequenceConsumer sequenceConsumer) {
		
		int batchSize = 500;
		int processed = 0;
		int offset = 0;
		int totalNumSeqs = sequenceSupplier.countSequences(cmdContext);

		while(processed < totalNumSeqs) {
			List<Sequence> sequences = sequenceSupplier.supplySequences(cmdContext, offset, batchSize);
			sequences.forEach(seq -> {
				sequenceConsumer.consumeSequence(cmdContext, seq);
			});
			offset += batchSize;
			processed += sequences.size();
			GlueLogger.getGlueLogger().finest("Processed "+processed+" of "+totalNumSeqs+" sequences");
			cmdContext.newObjectContext();
		}
	}
	
}
