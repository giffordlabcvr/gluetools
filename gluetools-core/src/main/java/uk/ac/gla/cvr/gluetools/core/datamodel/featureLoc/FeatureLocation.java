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

import java.util.ArrayList;
import java.util.Arrays;
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

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
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

	// set to true iff:
	// (a) is a coding feature
	// (b) is defined by a single contiguous reference segment.
	// (c) all labeled codons are of length 3
	// (d) labeled codons run along the reference in a straightforward order.
	private Boolean isSimpleCodingFeature;
	
	
	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}


	public synchronized List<LabeledCodon> getLabeledCodons(CommandContext cmdContext) {
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
						labeledCodons.add(new LabeledCodon(codonNumberingAncestorLocation.getFeature().getName(), codonLabel, refNtMinus2, refNtMinus1, refNt));
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
	
	public synchronized List<LabeledCodonReferenceSegment> getLabeledCodonReferenceSegments(CommandContext cmdContext) {
		if(labeledCodonReferenceSegments == null) {
			labeledCodonReferenceSegments = getLabeledCodons(cmdContext).stream()
					.flatMap(lc -> lc.getLcRefSegments().stream())
					.collect(Collectors.toList());
		}
		return labeledCodonReferenceSegments;
	}
	
	public synchronized Boolean getSimpleCodingFeature(CommandContext cmdContext) {
		if(isSimpleCodingFeature == null) {
			List<FeatureSegment> segments = getSegments();
			if(segments.size() != 1) {
				isSimpleCodingFeature = false;
			} else {
				ReferenceSegment singleFeatureSeg = segments.get(0).asReferenceSegment();
				List<LabeledCodonReferenceSegment> labeledCodonReferenceSegments = getLabeledCodonReferenceSegments(cmdContext);
				int nt = singleFeatureSeg.getRefStart();
				int labeledCodonIndex = 0;
				while(nt <= singleFeatureSeg.getRefEnd() - 2 && labeledCodonIndex < labeledCodonReferenceSegments.size()) {
					LabeledCodonReferenceSegment labeledCodonRefSeg = labeledCodonReferenceSegments.get(labeledCodonIndex);
					if(labeledCodonRefSeg.getRefStart() != nt) {
						isSimpleCodingFeature = false;
						break;
					}
					if(labeledCodonRefSeg.getCurrentLength() != 3) {
						isSimpleCodingFeature = false;
						break;
					}
					nt += 3;
					labeledCodonIndex ++;
				}
				if(isSimpleCodingFeature == null) {
					if(nt <= singleFeatureSeg.getRefEnd() - 2) {
						isSimpleCodingFeature = false;
					} else if(labeledCodonIndex < labeledCodonReferenceSegments.size()) {
						isSimpleCodingFeature = false;
					} else {
						isSimpleCodingFeature = true;
					}
				}
			}
		}
		return isSimpleCodingFeature;
	}
	
	public synchronized TIntObjectMap<LabeledCodon> getRefNtToLabeledCodon(CommandContext cmdContext) {
		if(refNtToLabeledCodon == null) {
			refNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			for(LabeledCodon labeledCodon: labeledCodons) {
				refNtToLabeledCodon.put(labeledCodon.getNtStart(), labeledCodon);
			}
		}
		return refNtToLabeledCodon;
	}
	
	public synchronized Map<String, LabeledCodon> getLabelToLabeledCodon(CommandContext cmdContext) {
		if(labelToLabeledCodon == null) {
			labelToLabeledCodon = new LinkedHashMap<String, LabeledCodon>();
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			for(LabeledCodon labeledCodon: labeledCodons) {
				labelToLabeledCodon.put(labeledCodon.getCodonLabel(), labeledCodon);
			}
		}
		return labelToLabeledCodon;
	}
	
	public synchronized LabeledCodon getLabeledCodon(CommandContext cmdContext, String label) {
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
			List<QueryAlignedSegment> queryToRefSegs, String queryNts, String qualityString, List<BaseVariationScanner<?>> scanners, 
			boolean excludeAbsent, boolean excludeInsufficientCoverage) {
		List<VariationScanResult<?>> variationScanResults = new ArrayList<VariationScanResult<?>>();
		for(BaseVariationScanner<?> scanner: scanners) {
			VariationScanResult<?> scanResult = scanner.scan(cmdContext, queryToRefSegs, queryNts, qualityString);
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


	public List<LabeledQueryAminoAcid> getReferenceAminoAcidContent(CommandContext cmdContext) {
		// feature area coordinates.
		List<ReferenceSegment> featureLocRefSegs = this.segmentsAsReferenceSegments();
		// maps reference to itself in the feature area.
		List<QueryAlignedSegment> featureLocQaSegs = ReferenceSegment.asQueryAlignedSegments(featureLocRefSegs);
		AbstractSequenceObject refSeqObj = this.getReferenceSequence().getSequence().getSequenceObject();
		Translator translator = new CommandContextTranslator(cmdContext);
		return translateQueryNucleotides(cmdContext, translator, featureLocQaSegs, refSeqObj);
	}


	/**
	 * Given some nucleotide query content, and a mapping between the query and this feature loc's reference, 
	 * translate the content to AAs, labeled according to the reference codon numbering.
	 */
	public List<LabeledQueryAminoAcid> translateQueryNucleotides(
			CommandContext cmdContext, 
			Translator translator,
			List<QueryAlignedSegment> queryToRefSegs,
			NucleotideContentProvider queryNucleotideContent) {
		if(this.getSimpleCodingFeature(cmdContext)) {
			ReferenceSegment singleRefSeg = this.getSegments().get(0).asReferenceSegment();
			List<QueryAlignedSegment> queryToRefFeatureSegs = ReferenceSegment.intersection(queryToRefSegs, Arrays.asList(singleRefSeg), ReferenceSegment.cloneLeftSegMerger());
			List<LabeledQueryAminoAcid> labeledQueryAminoAcids = new ArrayList<LabeledQueryAminoAcid>();
			TIntObjectMap<LabeledCodon> refNtToLabeledCodon = getRefNtToLabeledCodon(cmdContext);
			LabeledCodon labeledCodon = null;
			char[] nextAaNts = new char[3];
			Integer queryNtStart = null;
			Integer queryNtMiddle = null;
			Integer queryNtEnd = null;
			
			for(QueryAlignedSegment queryToRefFeatureSeg: queryToRefFeatureSegs) {
				for(int offset = 0; offset < queryToRefFeatureSeg.getCurrentLength(); offset++) {
					int refNt = queryToRefFeatureSeg.getRefStart()+offset;
					LabeledCodon foundLabeledCodon = refNtToLabeledCodon.get(refNt);
					if(foundLabeledCodon != null) {
						labeledCodon = foundLabeledCodon;
						queryNtStart = queryToRefFeatureSeg.getQueryStart()+offset;
						queryNtMiddle = null;
						queryNtEnd = null;
					} else if(labeledCodon != null) {
						if(refNt == labeledCodon.getNtMiddle()) {
							queryNtMiddle = queryToRefFeatureSeg.getQueryStart()+offset;
						} else if(refNt == labeledCodon.getNtEnd()) {
							queryNtEnd = queryToRefFeatureSeg.getQueryStart()+offset;
							if(queryNtStart != null && queryNtMiddle != null) {
								nextAaNts[0] = queryNucleotideContent.nt(cmdContext, queryNtStart);
								nextAaNts[1] = queryNucleotideContent.nt(cmdContext, queryNtMiddle);
								nextAaNts[2] = queryNucleotideContent.nt(cmdContext, queryNtEnd);
								AmbigNtTripletInfo ambigNtTripletInfo = translator.translate(new String(nextAaNts)).get(0);
								LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(labeledCodon, ambigNtTripletInfo);
								labeledQueryAminoAcids.add(new LabeledQueryAminoAcid(labeledAminoAcid, 
										queryNtStart, queryNtMiddle, queryNtEnd));
							}
						} else {
							labeledCodon = null;
							queryNtStart = null;
							queryNtMiddle = null;
							queryNtEnd = null;
						}
					}
				}
			}
			return labeledQueryAminoAcids;
		} else {
			// one segment per labeled codon associating it with its reference location.
			List<LabeledCodonReferenceSegment> labeledCodonReferenceSegments = this.getLabeledCodonReferenceSegments(cmdContext);


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
					char[] nts = new char[3];
					Integer queryNtStart = null;
					Integer queryNtMiddle = null;
					Integer queryNtEnd = null;
					for(LabeledCodonQueryAlignedSegment lcQaSeg : codonLcQaSegs) {
						for(int i = 0; i < lcQaSeg.getCurrentLength(); i++) {
							if(lcQaSeg.getRefStart()+i == labeledCodon.getNtStart()) {
								queryNtStart = lcQaSeg.getQueryStart()+i;
								nts[0] = queryNucleotideContent.nt(cmdContext, queryNtStart);
							} else if(lcQaSeg.getRefStart()+i == labeledCodon.getNtMiddle()) {
								queryNtMiddle = lcQaSeg.getQueryStart()+i;
								nts[1] = queryNucleotideContent.nt(cmdContext, queryNtMiddle);
							} else if(lcQaSeg.getRefStart()+i == labeledCodon.getNtEnd()) {
								queryNtEnd = lcQaSeg.getQueryStart()+i;
								nts[2] = queryNucleotideContent.nt(cmdContext, queryNtEnd);
							} 
						}
					}
					AmbigNtTripletInfo ambigNtTripletInfo = translator.translate(new String(nts)).get(0);
					LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(labeledCodon, ambigNtTripletInfo);
					labeledQueryAminoAcids.add(new LabeledQueryAminoAcid(labeledAminoAcid, 
							queryNtStart, queryNtMiddle, queryNtEnd));
				};
			});
			return labeledQueryAminoAcids;
		}
	}


	public List<LabeledCodon> getLabeledCodons(CommandContext cmdContext, LabeledCodon startLabeledCodon, LabeledCodon endLabeledCodon) {
		List<LabeledCodon> selectedLabeledCodons = new ArrayList<LabeledCodon>();
		List<LabeledCodon> allLabeledCodons = getLabeledCodons(cmdContext);
		boolean withinSelection = false; 
		for(LabeledCodon labeledCodon: allLabeledCodons) {
			if(labeledCodon.getCodonLabel().equals(startLabeledCodon.getCodonLabel())) {
				withinSelection = true;
			}
			if(withinSelection) {
				selectedLabeledCodons.add(labeledCodon);
			}
			if(labeledCodon.getCodonLabel().equals(endLabeledCodon.getCodonLabel())) {
				withinSelection = false;
			}
		}
		return selectedLabeledCodons;
	}



}

