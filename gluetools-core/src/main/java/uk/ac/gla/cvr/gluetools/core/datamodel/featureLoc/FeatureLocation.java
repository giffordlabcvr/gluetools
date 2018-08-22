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
package uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.CodonLabeler;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocationException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.AmbigNtTripletInfo;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.core.variationscanner.BaseVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;


@GlueDataClass(defaultListedProperties = {FeatureLocation.FEATURE_NAME_PATH})
public class FeatureLocation extends _FeatureLocation {
	
	public static final String REF_SEQ_NAME_PATH = 
			_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;
	public static final String FEATURE_NAME_PATH = 
			_FeatureLocation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String referenceSequenceName, String featureName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(REF_SEQ_NAME_PATH, referenceSequenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		return idMap;
	}

	
	private List<LabeledCodon> labeledCodons;
	private TIntObjectMap<LabeledCodon> refNtToLabeledCodon;
	private Map<String, LabeledCodon> labelToLabeledCodon;
	private List<LabeledCodonReferenceSegment> labeledCodonReferenceSegments;
	
	
	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}


	public List<LabeledCodon> getLabeledCodons(CommandContext cmdContext) {
		Feature feature = getFeature();
		feature.checkCodesAminoAcids();
		if(labeledCodons == null) {
			FeatureLocation codonNumberingAncestorLocation = getCodonNumberingAncestorLocation(cmdContext);
			if(codonNumberingAncestorLocation == null) {
				throw new FeatureLocationException(Code.FEATURE_OR_ANCESTOR_MUST_HAVE_OWN_CODON_NUMBERING, getFeature().getName());
			}
			CodonLabeler codonLabeler = codonNumberingAncestorLocation.getFeature().getCodonLabelerModule(cmdContext);
			if(codonLabeler == null) {
				Integer codon1Start = getCodon1Start(cmdContext);
				List<FeatureSegment> segments = getSegments();
				labeledCodons = new ArrayList<LabeledCodon>();
				if(segments.isEmpty()) { return labeledCodons; }
				int segIndex = 0;
				FeatureSegment currentSegment = segments.get(segIndex);
				int refNt = currentSegment.getRefStart();
				int refNtMinus1 = -1;
				int refNtMinus2 = -1;
				int startRefNt = refNt;
				int normalisedCodon1Start = (codon1Start - startRefNt) + 1;
				int featureNt = 1; // number of nucleotides through the feature.
				while(segIndex < segments.size() && refNt <= currentSegment.getRefEnd()) {
					if(TranslationUtils.isAtEndOfCodon(normalisedCodon1Start, featureNt)) {
						String codonLabel = Integer.toString(TranslationUtils.getCodon(normalisedCodon1Start, featureNt-2));
						labeledCodons.add(new LabeledCodon(codonLabel, refNtMinus2, refNtMinus1, refNt));
					}
					refNtMinus2 = refNtMinus1;
					refNtMinus1 = refNt;
					refNt++;
					featureNt++;
					if(refNt > currentSegment.getRefEnd()) {
						segIndex++;
						if(segIndex < segments.size()) {
							currentSegment = segments.get(segIndex);
							refNt = currentSegment.getRefStart();
						}
					}
				}
			} else {
				labeledCodons = codonLabeler.labelCodons(cmdContext, this);
			}
		}
		return labeledCodons;
	}
	
	public List<LabeledCodonReferenceSegment> getLabeledCodonReferenceSegments(CommandContext cmdContext) {
		if(labeledCodonReferenceSegments == null) {
			labeledCodonReferenceSegments = getLabeledCodons(cmdContext).stream()
					.flatMap(lc -> lc.getLcRefSegments().stream())
					.collect(Collectors.toList());
		}
		return labeledCodonReferenceSegments;
	}
	
	public TIntObjectMap<LabeledCodon> getRefNtToLabeledCodon(CommandContext cmdContext) {
		if(refNtToLabeledCodon == null) {
			refNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			for(LabeledCodon labeledCodon: labeledCodons) {
				refNtToLabeledCodon.put(labeledCodon.getNtStart(), labeledCodon);
			}
		}
		return refNtToLabeledCodon;
	}
	
	public Map<String, LabeledCodon> getLabelToLabeledCodon(CommandContext cmdContext) {
		if(labelToLabeledCodon == null) {
			labelToLabeledCodon = new LinkedHashMap<String, LabeledCodon>();
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			for(LabeledCodon labeledCodon: labeledCodons) {
				labelToLabeledCodon.put(labeledCodon.getCodonLabel(), labeledCodon);
			}
		}
		return labelToLabeledCodon;
	}
	
	public LabeledCodon getLabeledCodon(CommandContext cmdContext, String label) {
		LabeledCodon labeledCodon = getLabelToLabeledCodon(cmdContext).get(label);
		if(labeledCodon == null) {
			throw new FeatureLocationException(Code.FEATURE_LOCATION_INVALID_CODON_LABEL, 
					getReferenceSequence().getName(), getFeature().getName(), label);
		}
		return labeledCodon;
	}
	
	public LabeledCodon getFirstLabeledCodon(CommandContext cmdContext) {
		return getLabeledCodons(cmdContext).get(0);
	}

	public LabeledCodon getLastLabeledCodon(CommandContext cmdContext) {
		List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
		return labeledCodons.get(labeledCodons.size() - 1);
	}
	
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getReferenceSequence().getName(), getFeature().getName());
	}
	
	public FeatureLocation getNextAncestorLocation(CommandContext cmdContext) {
		Feature feature = getFeature();
		Feature nextAncestorFeature = feature.getNextAncestor();
		if(nextAncestorFeature != null) {
			return GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
					FeatureLocation.pkMap(getReferenceSequence().getName(), nextAncestorFeature.getName()), true);
		}
		return null;
		
	}
	
	public FeatureLocation getCodonNumberingAncestorLocation(CommandContext cmdContext) {
		if(getFeature().hasOwnCodonNumbering()) {
			return this;
		}
		FeatureLocation nextAncestor = getNextAncestorLocation(cmdContext);
		if(nextAncestor == null) {
			return null;
		}
		return nextAncestor.getCodonNumberingAncestorLocation(cmdContext);
	}

	
	public void validate(CommandContext cmdContext) {
		List<FeatureSegment> segments = getSegments();
		Feature feature = getFeature();
		if(segments.isEmpty()) {
			throw new FeatureLocationException(FeatureLocationException.Code.FEATURE_LOCATION_HAS_NO_SEGMENTS, 
					getReferenceSequence().getName(), feature.getName());
		}
		Feature nextAncestorFeature = feature.getNextAncestor();
		FeatureLocation nextAncestorFeatureLocation = getNextAncestorLocation(cmdContext);
		if(nextAncestorFeature != null && nextAncestorFeatureLocation == null) {
			throw new FeatureLocationException(FeatureLocationException.Code.NEXT_ANCESTOR_FEATURE_LOCATION_UNDEFINED, 
					getReferenceSequence().getName(), feature.getName(), nextAncestorFeature.getName());
		}
		if(nextAncestorFeatureLocation != null) {
			if(!ReferenceSegment.covers(nextAncestorFeatureLocation.getSegments(), segments)) {
				throw new FeatureLocationException(FeatureLocationException.Code.FEATURE_LOCATION_NOT_CONTAINED_WITHIN_NEXT_ANCESTOR, 
						getReferenceSequence().getName(), feature.getName(), nextAncestorFeature.getName());
			}
		}
		if(feature.codesAminoAcids()) {
			Integer codon1Start = getCodon1Start(cmdContext);
			int codon1Int = codon1Start.intValue();
			segments.forEach(seg -> {
				checkCodonAligned(feature, codon1Int, seg);
			});
		}
		getVariations().forEach(variation -> variation.validate(cmdContext));
		
	}

	private void checkCodonAligned(Feature feature, int codon1Start, IReferenceSegment seg) {
		if(!TranslationUtils.isAtStartOfCodon(codon1Start, seg.getRefStart()) || 
				!TranslationUtils.isAtEndOfCodon(codon1Start, seg.getRefEnd())) {
			throw new FeatureLocationException(
					FeatureLocationException.Code.FEATURE_LOCATION_SEGMENT_NOT_CODON_ALIGNED, 
					getReferenceSequence().getName(), feature.getName(), Integer.toString(seg.getRefStart()), 
					Integer.toString(seg.getRefEnd()), Integer.toString(codon1Start));
		}
	}
	
	/**
	 * Returns the reference NT number which points to codon 1 in the codon numbering system applicable to this
	 * feature location.
	 * @param cmdContext
	 * @return
	 */
	public Integer getCodon1Start(CommandContext cmdContext) {
		FeatureLocation codonNumberingAncestorLocation = getCodonNumberingAncestorLocation(cmdContext);
		if(codonNumberingAncestorLocation == null) {
			throw new FeatureLocationException(Code.FEATURE_OR_ANCESTOR_MUST_HAVE_OWN_CODON_NUMBERING, getFeature().getName());
		}
		List<FeatureSegment> segments = codonNumberingAncestorLocation.getSegments();
		if(!segments.isEmpty()) {
			// first segment establishes codon1start
			return segments.get(0).getRefStart();
		}
		throw new FeatureLocationException(Code.FEATURE_LOCATION_MUST_HAVE_SEGMENTS_TO_ESTABLISH_READING_FRAME, codonNumberingAncestorLocation.getReferenceSequence().getName(), codonNumberingAncestorLocation.getFeature().getName());
	}

	/**
	 *  Returns the highest-numbered codon in the feature-location's own codon numbering. 
	 *  This is null unless the following are true of the feature-location
	 * -- its feature has the OWN_CODON_NUMBERING metatag 
	 * -- it has a non-empty list of segments 
	 **/
	public Integer getOwnCodonNumberingMax(CommandContext cmdContext) {
		if(!getFeature().hasOwnCodonNumbering()) {
			return null;
		}
		List<FeatureSegment> segments = getSegments();
		if(segments.isEmpty()) {
			return null;
		}
		int codons = 0;
		for(FeatureSegment segment: segments) {
			codons += segment.getCurrentLength() / 3;
		}
		return codons;
	}
	
	
	public List<ReferenceSegment> segmentsAsReferenceSegments() {
		return getSegments().stream().map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
	}

	
	public static List<VariationScanResult<?>> variationScan(
			CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToRefNtSegs, String queryNts, String qualityString, List<Variation> variationsToScan, 
			boolean excludeAbsent, boolean excludeInsufficientCoverage) {
		List<BaseVariationScanner<?>> scanners = variationsToScan
				.stream()
				.map(variation -> variation.getScanner(cmdContext))
				.collect(Collectors.toList());
		return variationScan(queryToRefNtSegs, queryNts, qualityString, scanners, excludeAbsent, excludeInsufficientCoverage);
	}
	
	public static List<VariationScanResult<?>> variationScan(
			List<NtQueryAlignedSegment> queryToRefNtSegs, String queryNts, String qualityString, List<BaseVariationScanner<?>> scanners, 
			boolean excludeAbsent, boolean excludeInsufficientCoverage) {
		List<VariationScanResult<?>> variationScanResults = new ArrayList<VariationScanResult<?>>();
		for(BaseVariationScanner<?> scanner: scanners) {
			VariationScanResult<?> scanResult = scanner.scan(queryToRefNtSegs, queryNts, qualityString);
			if(scanResult.isPresent() || !excludeAbsent) {
				if(scanResult.isSufficientCoverage() || !excludeInsufficientCoverage) {
					variationScanResults.add(scanResult);
				}
			}
		}
		return variationScanResults;
	}

	public List<Variation> getVariationsQualified(CommandContext cmdContext, Expression variationWhereClauseExtra) {
		Expression variationWhereClause = 
				ExpressionFactory.matchExp(Variation.FEATURE_NAME_PATH, getFeature().getName())
				.andExp(ExpressionFactory.matchExp(Variation.REF_SEQ_NAME_PATH, getReferenceSequence().getName()));

		if(variationWhereClauseExtra != null) {
			variationWhereClause = variationWhereClause.andExp(variationWhereClauseExtra);
		}
		SelectQuery query = new SelectQuery(Variation.class, variationWhereClause);
		query.addOrdering(Variation.NAME_PROPERTY, SortOrder.ASCENDING);
		return GlueDataObject.query(cmdContext, Variation.class, query);
	}


	/**
	 * Given some nucleotide query content, a featureLoc on some reference, and a mapping between the query and the reference, 
	 * translate the content to AAs, labeled according to the reference codon numbering.
	 */
	public static List<LabeledQueryAminoAcid> translateNtContent(
			CommandContext cmdContext, FeatureLocation featureLoc,
			List<QueryAlignedSegment> queryToRefSegs,
			NucleotideContentProvider queryNucleotideContent) {
		List<LabeledCodonReferenceSegment> labeledCodonReferenceSegments = featureLoc.getLabeledCodonReferenceSegments(cmdContext);
	
		List<LabeledCodonQueryAlignedSegment> lcQaSegs = 
				ReferenceSegment.intersection(queryToRefSegs, labeledCodonReferenceSegments,
				new BiFunction<QueryAlignedSegment, LabeledCodonReferenceSegment, LabeledCodonQueryAlignedSegment>() {
					@Override
					public LabeledCodonQueryAlignedSegment apply(
							QueryAlignedSegment qaSeg,
							LabeledCodonReferenceSegment lcRefSeg) {
							LabeledCodonQueryAlignedSegment lcQaSeg = new LabeledCodonQueryAlignedSegment(lcRefSeg.getLabeledCodon(), 
											qaSeg.getRefStart(), qaSeg.getRefEnd(), 
											qaSeg.getQueryStart(), qaSeg.getQueryEnd());
							int leftOverhang = lcRefSeg.getRefStart() - qaSeg.getRefStart();
							if(leftOverhang > 0) {
								lcQaSeg.truncateLeft(leftOverhang);
							}
							int rightOverhang = qaSeg.getRefEnd() - lcRefSeg.getRefEnd() ;
							if(rightOverhang > 0) {
								lcQaSeg.truncateRight(rightOverhang);
							}
							
						return lcQaSeg;
					}
				});
		
	
		Map<LabeledCodon, List<LabeledCodonQueryAlignedSegment>> labeledCodonToLcQaSegs = 
				lcQaSegs.stream().collect(Collectors.groupingBy(LabeledCodonQueryAlignedSegment::getLabeledCodon));
		
		final Translator translator = new CommandContextTranslator(cmdContext);
		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = new ArrayList<LabeledQueryAminoAcid>();
	
		ArrayList<LabeledCodon> labeledCodons = new ArrayList<LabeledCodon>(labeledCodonToLcQaSegs.keySet());
		labeledCodons.sort(new Comparator<LabeledCodon>() {
			@Override
			public int compare(LabeledCodon o1, LabeledCodon o2) {
				return Integer.compare(o1.getNtStart(), o2.getNtStart());
			}
		});
		
		labeledCodons.forEach(labeledCodon -> {
			List<LabeledCodonQueryAlignedSegment> codonLcQaSegs = labeledCodonToLcQaSegs.get(labeledCodon);
			if(ReferenceSegment.covers(codonLcQaSegs, labeledCodon.getLcRefSegments())) {
				final char[] nts = new char[3];
				codonLcQaSegs.forEach(lcQaSeg -> {
					for(int i = 0; i < lcQaSeg.getCurrentLength(); i++) {
						if(lcQaSeg.getRefStart()+i == labeledCodon.getNtStart()) {
							nts[0] = queryNucleotideContent.nt(cmdContext, lcQaSeg.getQueryStart()+i);
						} else if(lcQaSeg.getRefStart()+i == labeledCodon.getNtMiddle()) {
							nts[1] = queryNucleotideContent.nt(cmdContext, lcQaSeg.getQueryStart()+i);
						} else if(lcQaSeg.getRefStart()+i == labeledCodon.getNtEnd()) {
							nts[2] = queryNucleotideContent.nt(cmdContext, lcQaSeg.getQueryStart()+i);
						} 
					}
				});
				AmbigNtTripletInfo ambigNtTripletInfo = translator.translate(new String(nts)).get(0);
				LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(labeledCodon, ambigNtTripletInfo);
				labeledQueryAminoAcids.add(new LabeledQueryAminoAcid(labeledAminoAcid, QueryAlignedSegment.minQueryStart(codonLcQaSegs)));
			};
		});
		return labeledQueryAminoAcids;
	}
	

}

