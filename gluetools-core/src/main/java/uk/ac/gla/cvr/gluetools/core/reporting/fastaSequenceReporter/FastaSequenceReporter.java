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
package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceReporter.TranslatedQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.textToQuery.TextToQueryTransformer;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaSequenceReporter",
		description="Provides commands for protein translation and variation scanning on FASTA nucleotide files")
public class FastaSequenceReporter extends ModulePlugin<FastaSequenceReporter> {

	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";


	private String alignerModuleName;


	public FastaSequenceReporter() {
		super();
		registerModulePluginCmdClass(FastaSequenceAminoAcidCommand.class);
		registerModulePluginCmdClass(FastaSequenceVariationScanCommand.class);
		registerModulePluginCmdClass(FastaSequenceStringVariationScanCommand.class);
		addSimplePropertyName(ALIGNER_MODULE_NAME);

	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, false);
	}
	
	public String getAlignerModuleName() {
		return alignerModuleName;
	}
	
	public AlignerResult alignToTargetReference(CommandContext cmdContext, String relatedRefName, 
			String fastaID, DNASequence fastaNTSeq) {
		Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, getAlignerModuleName());
		return aligner.computeConstrained(cmdContext, relatedRefName, fastaID, fastaNTSeq);
	}
	


	
	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
	}



	public static class TranslatedQueryAlignedSegment {

		private QueryAlignedSegment queryAlignedSegment;
		private String translation;
		
		public TranslatedQueryAlignedSegment(QueryAlignedSegment queryAlignedSegment, String translation) {
			this.queryAlignedSegment = queryAlignedSegment;
			this.translation = translation;
		}

		public QueryAlignedSegment getQueryAlignedSegment() {
			return queryAlignedSegment;
		}

		public String getTranslation() {
			return translation;
		}
	}
	
	public static Entry<String, DNASequence> getFastaEntry(ConsoleCommandContext consoleCmdContext, String fileName) {
		byte[] fastaFileBytes = consoleCmdContext.loadBytes(fileName);
		FastaUtils.normalizeFastaBytes(consoleCmdContext, fastaFileBytes);
		Map<String, DNASequence> headerToSeq = FastaUtils.parseFasta(fastaFileBytes);
		if(headerToSeq.size() > 1) {
			throw new FastaSequenceException(Code.MULTIPLE_FASTA_FILE_SEQUENCES, fileName);
		}
		if(headerToSeq.size() == 0) {
			throw new FastaSequenceException(Code.NO_FASTA_FILE_SEQUENCES, fileName);
		}
		Entry<String, DNASequence> singleEntry = headerToSeq.entrySet().iterator().next();
		return singleEntry;
	}

	public List<TranslatedQueryAlignedSegment> translateNucleotides(
			CommandContext cmdContext, FeatureLocation featureLoc,
			List<QueryAlignedSegment> queryToTargetFeatureArea, String queryNTs) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
