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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.fastaExporter.FastaExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.AbstractSequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

public abstract class BaseExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaExporter> implements ProvidedProjectModeCommand {
	
	public static final String LINE_FEED_STYLE = "lineFeedStyle";
	public static final String SUPPRESS_REVERSE_COMPLEMENT = "suppressReverseComplement";
	public static final String SUPPRESS_ROTATION = "suppressRotation";
	public static final String OFFSET = "offset";
	public static final String BATCH_SIZE = "batchSize";

	private LineFeedStyle lineFeedStyle;
	private Boolean suppressReverseComplement;
	private Boolean suppressRotation;
	private Integer offset;
	private Integer batchSize;


	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
		this.suppressReverseComplement = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, SUPPRESS_REVERSE_COMPLEMENT, false)).orElse(false);
		this.suppressRotation = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, SUPPRESS_ROTATION, false)).orElse(false);
		this.offset = PluginUtils.configureIntProperty(configElem, OFFSET, 1, true, null, false, false);
		this.batchSize = PluginUtils.configureIntProperty(configElem, BATCH_SIZE, 1, true, null, false, false);
		if( (this.offset == null && this.batchSize != null) || 
				(this.offset != null && this.batchSize == null) ) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <offset> and <batchSize> are to be specified, or neither");
		}
	}

	protected void export(CommandContext cmdContext, AbstractSequenceSupplier sequenceSupplier, 
			FastaExporter fastaExporter, PrintWriter printWriter) {
		AbstractSequenceConsumer sequenceConsumer = new AbstractSequenceConsumer() {
			@Override
			public void consumeSequence(CommandContext cmdContext, Sequence sequence) {
				String fastaId = fastaExporter.generateFastaId(sequence);
				printWriter.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId,
						sequence.getSequenceObject().getNucleotides(cmdContext, !suppressReverseComplement, !suppressRotation), lineFeedStyle));
				printWriter.flush();
			}
		};
		possiblyBatchedExport(cmdContext, sequenceSupplier, sequenceConsumer);
	}

	private void possiblyBatchedExport(CommandContext cmdContext, AbstractSequenceSupplier sequenceSupplier,
			AbstractSequenceConsumer sequenceConsumer) {
		if(this.offset == null) {
			FastaExporter.doExport(cmdContext, sequenceSupplier, sequenceConsumer);
		} else {
			FastaExporter.doExportSingleBatch(cmdContext, sequenceSupplier, sequenceConsumer, offset, batchSize);
		}
	}
	
	protected Map<String, DNASequence> export(CommandContext cmdContext, AbstractSequenceSupplier sequenceSupplier, 
			FastaExporter fastaExporter) {
		Map<String, DNASequence> ntFastaMap = new LinkedHashMap<String, DNASequence>();
		AbstractSequenceConsumer sequenceConsumer = new AbstractSequenceConsumer() {
			@Override
			public void consumeSequence(CommandContext cmdContext, Sequence sequence) {
				String fastaId = fastaExporter.generateFastaId(sequence);
				DNASequence ntSequence = FastaUtils.ntStringToSequence(sequence.getSequenceObject().getNucleotides(cmdContext, !suppressReverseComplement, !suppressRotation));
				ntFastaMap.put(fastaId, ntSequence);
			}
		};
		possiblyBatchedExport(cmdContext, sequenceSupplier, sequenceConsumer);
		return ntFastaMap;
	}
	
}