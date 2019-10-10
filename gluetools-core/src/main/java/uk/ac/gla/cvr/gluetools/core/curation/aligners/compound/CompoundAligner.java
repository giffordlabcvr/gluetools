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
package uk.ac.gla.cvr.gluetools.core.curation.aligners.compound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeConstrained;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.compound.CompoundAligner.CompoundAlignerResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.compound.CompoundAlignerException.Code;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

/**
 * Takes a list of multiple aligners and runs them all. 
 * Then, result segments are combined. Segments from aligners earlier in the list are preferred.
 * 
 * @author joshsinger
 *
 */
@PluginClass(elemName="compoundAligner", 
		description="Derives a pairwise homology using a combination of aligner types")
public class CompoundAligner extends Aligner<CompoundAlignerResult, CompoundAligner> implements SupportsComputeConstrained {

	private List<Aligner<?,?>> aligners = new ArrayList<Aligner<?,?>>();
	
	public CompoundAligner() {
		super();
		registerModulePluginCmdClass(CompoundAlignerAlignCommand.class);
		registerModulePluginCmdClass(CompoundAlignerFileAlignCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element alignersElem = PluginUtils.findConfigElement(configElem, "aligners");
		List<Element> alignerElems = GlueXmlUtils.findChildElements(alignersElem);
		for(Element alignerElem: alignerElems) {
			ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
			ModulePlugin<?> modulePlugin = pluginFactory.createFromElement(pluginConfigContext, alignerElem);
			if(!(modulePlugin instanceof Aligner)) {
				throw new CompoundAlignerException(Code.ELEMENT_IS_NOT_AN_ALIGNER, alignerElem.getNodeName());
			}
			aligners.add((Aligner<?,?>) modulePlugin);
		}
		
	}




	@Override
	public CompoundAlignerResult computeConstrained(CommandContext cmdContext,
			String refName, Map<String, DNASequence> queryIdToNucleotides) {
		// run all aligners on all queries.
		List<AlignerResult> alignerResults = new ArrayList<AlignerResult>();
		for(Aligner<?,?> aligner: aligners) {
			alignerResults.add(aligner.computeConstrained(cmdContext, refName, queryIdToNucleotides));
		}
		
		final Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments = 
				initFastaIdToAlignedSegments(queryIdToNucleotides.keySet());
		for(String queryId: queryIdToNucleotides.keySet()) {
			Comparator<IReferenceSegment> segmentComparator = new Comparator<IReferenceSegment>() {
				@Override
				public int compare(IReferenceSegment seg1, IReferenceSegment seg2) {
					return Integer.compare(seg1.getRefStart(), seg2.getRefStart());
				}};
			
			// this segment list will accumulate segments from the aligner results.
			// however, a segment from aligner result B will only appear if it
			// does not overlap a segment from earlier aligner result A on the reference or query.
			List<QueryAlignedSegment> finalAlignedSegs = new ArrayList<QueryAlignedSegment>();
			for(AlignerResult alignerResult: alignerResults) {
				List<QueryAlignedSegment> resultSegsForQuery = alignerResult.getQueryIdToAlignedSegments().get(queryId);
				if(resultSegsForQuery == null) {
					resultSegsForQuery = new ArrayList<QueryAlignedSegment>();
				}
				// remove overlaps on the reference.
				List<QueryAlignedSegment> resultSegsWithReferenceOverlapsRemoved = 
						ReferenceSegment.subtract(resultSegsForQuery, finalAlignedSegs);

				// remove overlaps on the query.
				List<QueryAlignedSegment> resultSegsInverted = 
						QueryAlignedSegment.invertList(resultSegsWithReferenceOverlapsRemoved);
				List<QueryAlignedSegment> finalSegsInverted = 
						QueryAlignedSegment.invertList(finalAlignedSegs);
				List<QueryAlignedSegment> resultSegsWithQueryOverlapsRemovedInverted = 
						ReferenceSegment.subtract(resultSegsInverted, finalSegsInverted);
				List<QueryAlignedSegment> resultSegsWithAllOverlapsRemoved = 
						QueryAlignedSegment.invertList(resultSegsWithQueryOverlapsRemovedInverted);

				// add everything together
				finalAlignedSegs.addAll(resultSegsWithAllOverlapsRemoved);
				Collections.sort(finalAlignedSegs, segmentComparator);
			}
			queryIdToAlignedSegments.put(queryId, finalAlignedSegs);
		}
		
		
		return new CompoundAlignerResult(queryIdToAlignedSegments);
		
	}

	
	
	
	
	
	
	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Aligner.AlignCommand> getComputeConstrainedCommandClass() {
		return CompoundAlignerAlignCommand.class;
	}
	
	public static class CompoundAlignerResult extends Aligner.AlignerResult {
		public CompoundAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("compoundAlignerResult", fastaIdToAlignedSegments);
		}
	}

	@Override
	public boolean supportsComputeConstrained() {
		for(Aligner<?, ?> aligner : aligners) {
			if(!(aligner instanceof SupportsComputeConstrained)) {
				return false;
			}
			if(!((SupportsComputeConstrained) aligner).supportsComputeConstrained()) {
				return false;
			}
		}
		return true;
	}

}
