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
package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import htsjdk.samtools.SAMRecord;
import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public abstract class BaseVariationScanner<M extends VariationScannerMatchResult> {
	
	private List<VariationMetatagType> allowedMetatagTypes;
	private List<VariationMetatagType> requiredMetatagTypes;
	private Variation variation;
	private Map<VariationMetatagType, String> metatagsMap;
	private boolean validated;
	
	protected BaseVariationScanner(List<VariationMetatagType> allowedMetatagTypes, List<VariationMetatagType> requiredMetatagTypes) {
		super();
		this.allowedMetatagTypes = allowedMetatagTypes;
		this.requiredMetatagTypes = requiredMetatagTypes;
		for(VariationMetatagType requiredMetatagType: requiredMetatagTypes) {
			if(!allowedMetatagTypes.contains(requiredMetatagType)) {
				throw new RuntimeException("Required metatag type must also be listed as allowed.");
			}
		}
	}
	
	public final void init(CommandContext cmdContext, Variation variation) {
		this.variation = variation;
		this.metatagsMap = variation.getMetatagsMap();
		try {
			this.init(cmdContext);
		} catch(GlueException ge) {
			throwScannerException(ge, "Unable to initialise: "+ge.getLocalizedMessage());
		}
	}
	
	protected void init(CommandContext cmdContext) {
	}

	public Variation getVariation() {
		return variation;
	}

	protected Map<VariationMetatagType, String> getMetatagsMap() {
		return metatagsMap;
	}

	public void validate() {
		for(VariationMetatagType requiredMetatagType: requiredMetatagTypes) {
			if(!metatagsMap.containsKey(requiredMetatagType)) {
				throwScannerException("Missing required metatag with key: "+requiredMetatagType.name());
			}
		}
		for(VariationMetatagType metatagType: metatagsMap.keySet()) {
			if(!allowedMetatagTypes.contains(metatagType)) {
				throwScannerException("Metatag key: "+metatagType.name()+" is not allowed for variation type "+variation.getVariationType().name());
			}
		}
	}

	public void throwScannerException(String errorTxt) {
		throw new VariationException(Code.VARIATION_SCANNER_EXCEPTION, variation.getFeatureLoc().getReferenceSequence().getName(), 
				variation.getFeatureLoc().getFeature().getName(), 
				variation.getName(), errorTxt);
	}

	public void throwScannerException(Throwable cause, String errorTxt) {
		throw new VariationException(cause, Code.VARIATION_SCANNER_EXCEPTION, variation.getFeatureLoc().getReferenceSequence().getName(), 
				variation.getFeatureLoc().getFeature().getName(), 
				variation.getName(), errorTxt);
	}

	
	public List<VariationMetatagType> getAllowedMetatagTypes() {
		return allowedMetatagTypes;
	}

	public final VariationScanResult<M> scan(CommandContext cmdContext, List<QueryAlignedSegment> queryToRefSegs, String queryNts, String qualityString) {
		if(!this.validated) {
			this.validate();
			this.validated = true;
		}
		return this.scanInternal(cmdContext, queryToRefSegs, queryNts, qualityString);
	}

	protected abstract VariationScanResult<M> scanInternal(CommandContext cmdContext, List<QueryAlignedSegment> queryToRefSegs, String queryNts, String qualityString);

	protected Pattern parseRegex(String stringPattern) {
		try {
			return Pattern.compile(stringPattern);
		} catch(PatternSyntaxException pse) {
			throwScannerException("Syntax error in variation regex: "+pse.getMessage());
			return null; // unreachable!
		}
	}

	protected boolean computeSufficientCoverage(List<QueryAlignedSegment> queryToRefSegs) {
		List<ReferenceSegment> coverageSegments = getSegmentsToCover();
		List<ReferenceSegment> segmentsToCover = coverageSegments;
		return ReferenceSegment.covers(queryToRefSegs, segmentsToCover);
	}

	public abstract List<ReferenceSegment> getSegmentsToCover();

	protected Integer getIntMetatagValue(VariationMetatagType type) {
		String stringValue = getStringMetatagValue(type);
		if(stringValue != null) {
			try {
				return Integer.parseInt(stringValue);
			} catch(NumberFormatException nfe) {
				throwScannerException("Expected integer value for metatag: "+type.name());
			}
		}
		return null;
	}

	protected Boolean getBooleanMetatagValue(VariationMetatagType type) {
		String stringValue = getStringMetatagValue(type);
		if(stringValue != null) {
			return Boolean.parseBoolean(stringValue);
		}
		return null;
	}

	protected Double getDoubleMetatagValue(VariationMetatagType type) {
		String stringValue = getStringMetatagValue(type);
		if(stringValue != null) {
			try {
				return Double.parseDouble(stringValue);
			} catch(NumberFormatException nfe) {
				throwScannerException("Expected double value for metatag: "+type.name());
			}
		}
		return null;
	}

	public Integer getRefStart() {
		return getVariation().getRefStart();
	}

	public Integer getRefEnd() {
		return getVariation().getRefEnd();
	}

	protected String getStringMetatagValue(VariationMetatagType type) {
		return getMetatagsMap().get(type);
	}
	
	public VariationScanResult<M> resolvePairedReadResults(
			SAMRecord record1, VariationScanResult<?> uncastResult1, 
			SAMRecord record2, VariationScanResult<?> uncastResult2) {
		int readNameHashCoinFlip = Math.abs(record1.getReadName().hashCode()) % 2;

		@SuppressWarnings("unchecked")
		VariationScanResult<M> result1 = (VariationScanResult<M>) uncastResult1;
		@SuppressWarnings("unchecked")
		VariationScanResult<M> result2 = (VariationScanResult<M>) uncastResult2;
		
		Integer qScore1 = result1.getQScore();
		Integer qScore2 = result2.getQScore();
		if(qScore1 != null && qScore2 != null) {
			if(qScore1 > qScore2) {
				return result1;
			} else if(qScore1 < qScore2) {
				return result2;
			}
		}
		int mappingQuality1 = record1.getMappingQuality();
		int mappingQuality2 = record2.getMappingQuality();
		if(mappingQuality1 > mappingQuality2) {
			return result1;
		} else if(mappingQuality1 < mappingQuality2) {
			return result2;
		}
		if(readNameHashCoinFlip == 0) {
			return result1;
		} else {
			return result2;
		}
	}

	protected Integer bestQScoreOfMatchResults(List<? extends VariationScannerMatchResult> matchResults) {
		Integer bestQScore = null;
		for(VariationScannerMatchResult matchResult: matchResults) {
			Integer matchResultQScore = matchResult.getWorstContributingQScore();
			if(matchResultQScore != null) {
				if(bestQScore == null || matchResultQScore > bestQScore){
					bestQScore = matchResultQScore;
				}
			}
		}
		return bestQScore;
	}

	protected Integer worstQScoreOfSegments(String qualityString, List<? extends QueryAlignedSegment> qaSegs) {
		Integer worstQScore = null;
		for(QueryAlignedSegment qaSeg: qaSegs) {
			Integer segQScore = SamUtils.worstQScore(qualityString, qaSeg.getQueryStart(), qaSeg.getQueryEnd());
			if(segQScore != null) {
				if(worstQScore == null || segQScore < worstQScore){
					worstQScore = segQScore;
				}
			}
		}
		return worstQScore;
	}
	
}
