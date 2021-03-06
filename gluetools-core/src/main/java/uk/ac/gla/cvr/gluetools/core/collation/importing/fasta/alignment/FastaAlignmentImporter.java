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
package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaAlignmentImporter",
description="Imports a nucleotide alignment from a FASTA file")
public class FastaAlignmentImporter extends FastaNtAlignmentImporter<FastaAlignmentImporter> {

	public static final String SEQUENCE_GAP_REGEX = "sequenceGapRegex";
	public static final String REQUIRE_TOTAL_COVERAGE = "requireTotalCoverage";
	public static final String ALLOW_AMBIGUOUS_SEGMENTS = "allowAmbiguousSegments";
	public static final String SKIP_ROWS_WITH_MISSING_SEGMENTS = "skipRowsWithMissingSegments";

	
	private Boolean requireTotalCoverage = true;
	private Pattern sequenceGapRegex = null;
	private Boolean allowAmbiguousSegments = false;
	private Boolean skipRowsWithMissingSegments;
	
	public FastaAlignmentImporter() {
		super();
		registerModulePluginCmdClass(FastaAlignmentImporterImportCommand.class);
		registerModulePluginCmdClass(FastaAlignmentImporterPreviewCommand.class);
		addSimplePropertyName(SEQUENCE_GAP_REGEX);
		addSimplePropertyName(REQUIRE_TOTAL_COVERAGE);
		addSimplePropertyName(ALLOW_AMBIGUOUS_SEGMENTS);
		addSimplePropertyName(SKIP_ROWS_WITH_MISSING_SEGMENTS);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sequenceGapRegex = Optional
				.ofNullable(PluginUtils.configureRegexPatternProperty(configElem, SEQUENCE_GAP_REGEX, false))
				.orElse(Pattern.compile("[Nn-]"));
		requireTotalCoverage = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, REQUIRE_TOTAL_COVERAGE, false))
				.orElse(true);
		allowAmbiguousSegments = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_AMBIGUOUS_SEGMENTS, false))
				.orElse(false);
		skipRowsWithMissingSegments = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, SKIP_ROWS_WITH_MISSING_SEGMENTS, false))
				.orElse(false);
	}

	


	@Override
	public List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext, 
			String queryId, String queryNucleotides, 
			List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs, 
			List<ReferenceSegment> navigationRegion) {

		List<QueryAlignedSegment> queryAlignedSegs = new ArrayList<QueryAlignedSegment>();
    	
    	int alignmentNtIndex = 1;
    	int foundSequenceNtIndex = 1;

    	QueryAlignedSegment queryAlignedSeg = null;
    	while(alignmentNtIndex <= fastaAlignmentNTs.length()) {
    		char fastaAlignmentNT = FastaUtils.nt(fastaAlignmentNTs, alignmentNtIndex);
    		if(isGapChar(fastaAlignmentNT)) {
    			if(queryAlignedSeg != null) {
    				foundSequenceNtIndex = completeQueryAlignedSeg(existingSegs,
    						fastaAlignmentNTs, queryId, queryId,
    						queryNucleotides, queryAlignedSegs,
    						foundSequenceNtIndex, queryAlignedSeg);
    				queryAlignedSeg = null;
    				if(foundSequenceNtIndex == -1) {
    					return null;
    				}        					
    			}
    		} else {
    			if(queryAlignedSeg == null) {
    				queryAlignedSeg = new QueryAlignedSegment(alignmentNtIndex, alignmentNtIndex, 1, 1);
    			}
    			queryAlignedSeg.setRefEnd(alignmentNtIndex);
    		}
			alignmentNtIndex++;
    	}
		if(queryAlignedSeg != null) {
			foundSequenceNtIndex = completeQueryAlignedSeg(existingSegs,
					fastaAlignmentNTs, queryId, queryId,
					queryNucleotides, queryAlignedSegs,
					foundSequenceNtIndex, queryAlignedSeg);
		}
		if(requireTotalCoverage && foundSequenceNtIndex != queryNucleotides.length()+1) {
			throw new FastaAlignmentImporterException(Code.MISSING_COVERAGE, foundSequenceNtIndex, queryNucleotides.length(), queryId, queryId);
		}
		return queryAlignedSegs;
	}

	public int completeQueryAlignedSeg(List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs,
			String exceptionReportingID, String whereClauseString, String foundSequenceNTs,
			List<QueryAlignedSegment> queryAlignedSegs,
			int foundSequenceNtIndex, QueryAlignedSegment queryAlignedSeg) {
		Integer refStart = queryAlignedSeg.getRefStart();
		Integer refEnd = queryAlignedSeg.getRefEnd();
		String subSequence = FastaUtils
				.subSequence(fastaAlignmentNTs, refStart, refEnd).toString().toUpperCase();
		int foundIdxOfSubseq = FastaUtils.find(foundSequenceNTs, subSequence, foundSequenceNtIndex);
		if(foundIdxOfSubseq == -1) {
			if(skipRowsWithMissingSegments) {
				logMissingSegmentSkippedRow(exceptionReportingID, refStart, refEnd, whereClauseString);
				return -1;
			} else {
				throw new FastaAlignmentImporterException(Code.SUBSEQUENCE_NOT_FOUND, refStart, refEnd, exceptionReportingID, whereClauseString);
			}
		}
		if(requireTotalCoverage && foundIdxOfSubseq != foundSequenceNtIndex) {
			throw new FastaAlignmentImporterException(Code.MISSING_COVERAGE, foundSequenceNtIndex, foundIdxOfSubseq-1, exceptionReportingID, whereClauseString);
		}
		if(!allowAmbiguousSegments) {
			int nextIdxOfSubseq = FastaUtils.find(foundSequenceNTs, subSequence, foundIdxOfSubseq+1);
			if(nextIdxOfSubseq != -1) {
				throw new FastaAlignmentImporterException(Code.AMBIGUOUS_SEGMENT, refStart, refEnd, exceptionReportingID, whereClauseString, foundSequenceNtIndex);
			}
		}
		queryAlignedSeg.setQueryStart(foundIdxOfSubseq);
		queryAlignedSeg.setQueryEnd(foundIdxOfSubseq+(subSequence.length()-1));

		List<QueryAlignedSegment> intersection = ReferenceSegment.intersection(existingSegs, 
				Collections.singletonList(queryAlignedSeg), 
				ReferenceSegment.cloneLeftSegMerger());
		if(!intersection.isEmpty()) {
			QueryAlignedSegment firstOverlap = intersection.get(0);
			throw new FastaAlignmentImporterException(Code.SEGMENT_OVERLAPS_EXISTING, 
					firstOverlap.getRefStart(), firstOverlap.getRefEnd(), 
					exceptionReportingID, whereClauseString);
		}
		queryAlignedSegs.add(queryAlignedSeg);
		existingSegs.add(queryAlignedSeg);
		foundSequenceNtIndex = queryAlignedSeg.getQueryEnd()+1;
		return foundSequenceNtIndex;
	}
	
	private void logMissingSegmentSkippedRow(String fastaId, int startColumnNumber, int endColumnNumber, String whereClause) {
		GlueLogger.getGlueLogger().warning("Skipping alignment row "+fastaId+
				": segment ["+startColumnNumber+", "+endColumnNumber+
				"] is missing in sequence identified by "+whereClause);
	}

	
	private boolean isGapChar(char seqChar) {
		return sequenceGapRegex.matcher(new String(new char[]{seqChar})).find();
	}

	@Override
	public List<QueryAlignedSegment> alignmentRowImport(CommandContext cmdContext, String queryId, String queryNucleotides, String alignmentRow) {
		return findAlignedSegs(cmdContext, queryId, queryNucleotides, new ArrayList<QueryAlignedSegment>(), alignmentRow);
	}

	
}
