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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.CodonLabeler;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.ModifiedLabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.SimpleLabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocationException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.AmbigNtTripletInfo;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.ResidueUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.core.translationModification.OutputAminoAcid;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifier;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifierException;
import uk.ac.gla.cvr.gluetools.core.variationscanner.BaseVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;


@GlueDataClass(defaultListedProperties = {FeatureLocation.REF_SEQ_NAME_PATH, FeatureLocation.FEATURE_NAME_PATH})
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
	private LabeledCodon[] translationIndexToLabeledCodon;
	private TIntObjectMap<LabeledCodon> startRefNtToLabeledCodon;
	private TIntObjectMap<LabeledCodon> endRefNtToLabeledCodon;
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


	public synchronized LabeledCodon[] getTranslationIndexToLabeledCodon(CommandContext cmdContext) {
		if(translationIndexToLabeledCodon == null) {
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			translationIndexToLabeledCodon = new LabeledCodon[labeledCodons.size()];
			for(LabeledCodon labeledCodon: labeledCodons) {
				int translationIndex = labeledCodon.getTranslationIndex();
				if(translationIndex >= translationIndexToLabeledCodon.length) {
					// RuntimeException because this should never happen
					throw new RuntimeException("Unexpected transcription index");
				}
				if(translationIndexToLabeledCodon[translationIndex] != null) {
					// RuntimeException because this should never happen
					throw new RuntimeException("Duplicate transcription index");
				}
				translationIndexToLabeledCodon[translationIndex] = labeledCodon;
			}
		}
		return translationIndexToLabeledCodon;
	}

	public synchronized List<LabeledCodon> getLabeledCodons(CommandContext cmdContext) {
		Feature feature = getFeature();
		feature.checkCodesAminoAcids();
		if(labeledCodons == null) {
			if(!feature.hasOwnCodonNumbering()) {
				FeatureLocation codonNumberingAncestorLocation = getCodonNumberingAncestorLocation(cmdContext);
				if(codonNumberingAncestorLocation == null) {
					throw new FeatureLocationException(Code.FEATURE_OR_ANCESTOR_MUST_HAVE_OWN_CODON_NUMBERING, getFeature().getName());
				}
				List<FeatureSegment> featureSegments = getRotatedReferenceSegments();
				List<LabeledCodonReferenceSegment> overlappingLcRefSegs = 
						ReferenceSegment.intersection(featureSegments, 
								codonNumberingAncestorLocation.getLabeledCodonReferenceSegments(cmdContext), ReferenceSegment.cloneRightSegMerger());
				this.labeledCodons = overlappingLcRefSegs.stream().map(lcRefSeg -> lcRefSeg.getLabeledCodon()).collect(Collectors.toList());
			} else {
				int codonLabelInteger = 1;
				List<FeatureSegment> featureSegments = getRotatedReferenceSegments();
				
				boolean revComp = feature.reverseComplementTranslation();
				if(revComp) {
					Collections.reverse(featureSegments);
				}
				
				ArrayList<LabeledCodon> newLabeledCodons = new ArrayList<LabeledCodon>();
				Integer currentCodonStart = null;
				Integer currentCodonMiddle = null;
				int translationIndex = 0;

				for(FeatureSegment featureSegment: featureSegments) {
					String translationModifierName = featureSegment.getTranslationModifierName();
					if(translationModifierName == null) {
						int firstRefNt = revComp ? featureSegment.getRefEnd() : featureSegment.getRefStart();
						int afterLastRefNt = revComp ? featureSegment.getRefStart()-1 : featureSegment.getRefEnd()+1;
						int refIncrement = revComp ? -1 : 1;
						
						for(int refNt = firstRefNt; refNt != afterLastRefNt; refNt += refIncrement) {
							if(currentCodonStart == null) {
								currentCodonStart = refNt;
								continue;
							} else if(currentCodonMiddle == null) {
								currentCodonMiddle = refNt;
								continue;
							} else {
								if(revComp) {
									// actually put the dependent ref NT order back in forwards direction, reversing it will come later.
									newLabeledCodons.add(new SimpleLabeledCodon(getFeature().getName(), 
											Integer.toString(codonLabelInteger), refNt, currentCodonMiddle, currentCodonStart, translationIndex));
								} else {
									newLabeledCodons.add(new SimpleLabeledCodon(getFeature().getName(), 
											Integer.toString(codonLabelInteger), currentCodonStart, currentCodonMiddle, refNt, translationIndex));
								}
								translationIndex++;
								codonLabelInteger++;
								currentCodonStart = null;
								currentCodonMiddle = null;
							}
						}
					} else {
						if(currentCodonStart != null || currentCodonMiddle != null) {
							throw new TranslationModifierException(TranslationModifierException.Code.MODIFICATION_ERROR, "Feature segment with translation modifier begins at a mid-codon location");
						}
						TranslationModifier translationModifier = Module.resolveModulePlugin(cmdContext, TranslationModifier.class, translationModifierName);
						if(translationModifier.getSegmentNtLength() != featureSegment.getCurrentLength()) {
							throw new TranslationModifierException(TranslationModifierException.Code.MODIFICATION_ERROR, "Feature segment length must match translation modifier length");
						}
						List<OutputAminoAcid> outputAminoAcids = translationModifier.getOutputAminoAcids();
						final int refStart = featureSegment.getRefStart();
						if(revComp) {
							Collections.reverse(outputAminoAcids);
						}
						for(OutputAminoAcid outputAminoAcid: outputAminoAcids) {
							List<Integer> dependentRefNts = outputAminoAcid.getDependentNtPositions().stream().map(dntp -> (dntp - 1) + refStart).collect(Collectors.toList());
							ModifiedLabeledCodon modifiedLabeledCodon = new ModifiedLabeledCodon(getFeature().getName(), Integer.toString(codonLabelInteger), translationModifierName, dependentRefNts, translationIndex);
							newLabeledCodons.add(modifiedLabeledCodon);
							translationIndex++;
							codonLabelInteger++;
						}
					}
				}
				CodonLabeler codonLabeler = getFeature().getCodonLabelerModule(cmdContext);
				if(codonLabeler != null) {
					codonLabeler.relabelCodons(cmdContext, this, newLabeledCodons);
				}
				this.labeledCodons = newLabeledCodons;
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
			for(FeatureSegment segment: segments) {
				if(segment.getTranslationModifierName() != null) {
					isSimpleCodingFeature = false;
					return isSimpleCodingFeature;
				}
				if(segment.getSpliceIndex() != 1) {
					isSimpleCodingFeature = false;
					return isSimpleCodingFeature;
				}
				if(segment.getTranscriptionIndex() != 1) {
					isSimpleCodingFeature = false;
					return isSimpleCodingFeature;
				}
			}
			if(segments.size() != 1) {
				isSimpleCodingFeature = false;
				return isSimpleCodingFeature;
			} else {
				ReferenceSegment singleFeatureSeg = segments.get(0).asReferenceSegment();
				List<LabeledCodonReferenceSegment> labeledCodonReferenceSegments = getLabeledCodonReferenceSegments(cmdContext);
				int nt = singleFeatureSeg.getRefStart();
				int labeledCodonIndex = 0;
				while(nt <= singleFeatureSeg.getRefEnd() - 2 && labeledCodonIndex < labeledCodonReferenceSegments.size()) {
					LabeledCodonReferenceSegment labeledCodonRefSeg = labeledCodonReferenceSegments.get(labeledCodonIndex);
					if(labeledCodonRefSeg.getRefStart() != nt) {
						isSimpleCodingFeature = false;
						return isSimpleCodingFeature;
					}
					if(labeledCodonRefSeg.getCurrentLength() != 3) {
						isSimpleCodingFeature = false;
						return isSimpleCodingFeature;
					}
					nt += 3;
					labeledCodonIndex ++;
				}
				if(isSimpleCodingFeature == null) {
					if(nt <= singleFeatureSeg.getRefEnd() - 2) {
						isSimpleCodingFeature = false;
						return isSimpleCodingFeature;
					} else if(labeledCodonIndex < labeledCodonReferenceSegments.size()) {
						isSimpleCodingFeature = false;
						return isSimpleCodingFeature;
					} else {
						isSimpleCodingFeature = true;
						return isSimpleCodingFeature;
					}
				}
			}
		}
		return isSimpleCodingFeature;
	}
	
	public synchronized TIntObjectMap<LabeledCodon> getStartRefNtToLabeledCodon(CommandContext cmdContext) {
		if(startRefNtToLabeledCodon == null) {
			startRefNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			for(LabeledCodon labeledCodon: labeledCodons) {
				startRefNtToLabeledCodon.put(labeledCodon.getNtStart(), labeledCodon);
			}
		}
		return startRefNtToLabeledCodon;
	}

	public synchronized TIntObjectMap<LabeledCodon> getEndRefNtToLabeledCodon(CommandContext cmdContext) {
		if(endRefNtToLabeledCodon == null) {
			endRefNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			for(LabeledCodon labeledCodon: labeledCodons) {
				endRefNtToLabeledCodon.put(labeledCodon.getNtEnd(), labeledCodon);
			}
		}
		return endRefNtToLabeledCodon;
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
		ReferenceSegment.sortByRefStart(segments);
		Feature feature = getFeature();
		// feature location must have segments.
		if(segments.isEmpty()) {
			throw new FeatureLocationException(FeatureLocationException.Code.FEATURE_LOCATION_HAS_NO_SEGMENTS, 
					getReferenceSequence().getName(), feature.getName());
		}
		// if the feature has a next ancestor (first non-informational ancestor) then there must be a feature location for
		// that next ancestor.
		Feature nextAncestorFeature = feature.getNextAncestor();
		FeatureLocation nextAncestorFeatureLocation = getNextAncestorLocation(cmdContext);
		if(nextAncestorFeature != null && nextAncestorFeatureLocation == null) {
			throw new FeatureLocationException(FeatureLocationException.Code.NEXT_ANCESTOR_FEATURE_LOCATION_UNDEFINED, 
					getReferenceSequence().getName(), feature.getName(), nextAncestorFeature.getName());
		}
		// if the feature location has a next ancestor, this feature location must be contained within it.
		if(nextAncestorFeatureLocation != null) {
			List<FeatureSegment> ancestorSegments = nextAncestorFeatureLocation.getSegments();
			ReferenceSegment.sortByRefStart(ancestorSegments);
			if(!ReferenceSegment.covers(ancestorSegments, segments)) {
				throw new FeatureLocationException(FeatureLocationException.Code.FEATURE_LOCATION_NOT_CONTAINED_WITHIN_NEXT_ANCESTOR, 
						getReferenceSequence().getName(), feature.getName(), nextAncestorFeature.getName());
			}
		}
		int lastSpliceIndex = Integer.MIN_VALUE;
		String lastModifierName = "NULL";
		for(FeatureSegment segment: segments) {
			if(segment.getSpliceIndex() < lastSpliceIndex) {
				throw new FeatureLocationException(FeatureLocationException.Code.SPLICE_INDEX_ERROR, 
						getReferenceSequence().getName(), feature.getName(), "Splice index must increase monotonically along the genome");
				
			}
			String segModifierName = segment.getTranslationModifierName();
			if(segModifierName == null) {segModifierName = "NULL";} 
			if(!segModifierName.equals(lastModifierName) && segment.getSpliceIndex() == lastSpliceIndex) {
				throw new FeatureLocationException(FeatureLocationException.Code.SPLICE_INDEX_ERROR, 
						getReferenceSequence().getName(), feature.getName(), "Segments with translation modifiers must have a unique splice index");
			}
			lastSpliceIndex = segment.getSpliceIndex();
		}
		Map<Integer, List<FeatureSegment>> spliceIndexToFSegs = segments.stream().collect(Collectors.groupingBy(FeatureSegment::getSpliceIndex));
		for(List<FeatureSegment> fSegs: spliceIndexToFSegs.values()) {
			Set<Integer> transcriptionIndices = fSegs.stream().map(FeatureSegment::getTranscriptionIndex).collect(Collectors.toSet());
			if(transcriptionIndices.size() != 1) {
				throw new FeatureLocationException(FeatureLocationException.Code.SPLICE_INDEX_ERROR, 
						getReferenceSequence().getName(), feature.getName(), "Segments with the same splice index must have the same transcription index");
			}
		}
		
		if(feature.codesAminoAcids()) {
			checkCodingFeatureLocation(cmdContext);
		}
		getVariations().forEach(variation -> variation.validate(cmdContext));
		
	}
	
	/*
	 * Coding features are where CODES_AMINO_ACIDS is true.
	 * 
	 * There are two kinds of coding features, different rules apply.
	 * 
	 * (a) Coding features that define their own translation details (OWN_CODON_NUMBERING = true).
	 * 
	 * -- they may have CODON_LABELER_MODULE non-null
	 * 
	 * (b) Coding features that inherit their translation details from an ancestor (OWN_CODON_NUMBERING = false)
	 *     
	 * -- these may not have a non-null CODON_LABELER_MODULE
	 * -- segments may not have any side properties, such as translationModifier, splice index etc.
	 * -- must have a coding next ancestor
	 * -- feature-location's segments must pick out a non-empty set of labeled codons from that ancestor, 
	 * these become the feature-location's labeled codons. The region defined by the segments of the feature location 
	 * and defined by the labeled codons must be the same region.
	 *     
	 */

	private void checkCodingFeatureLocation(CommandContext cmdContext) {
		List<FeatureSegment> featureSegs = getSegments();
		ReferenceSegment.sortByRefStart(featureSegs);
		
		Feature feature = getFeature();

		if(!feature.hasOwnCodonNumbering()) {
			for(FeatureSegment featureSeg: featureSegs) {
				if(featureSeg.getSpliceIndex() != null) {
					throw new FeatureLocationException(FeatureLocationException.Code.CODING_FEATURE_LOCATION_ERROR, 
							getReferenceSequence().getName(), feature.getName(), "Coding feature without own codon numbering may not use splice index on segments");
					
				}
				if(featureSeg.getTranslationModifierName() != null) {
					throw new FeatureLocationException(FeatureLocationException.Code.CODING_FEATURE_LOCATION_ERROR, 
							getReferenceSequence().getName(), feature.getName(), "Coding feature without own codon numbering may not use translation modifier on segments");
				}
			}
		}
		
		List<ReferenceSegment> uncodedRegions = new ArrayList<ReferenceSegment>();
		for(FeatureSegment featureSeg: featureSegs) {
			String translationModifierName = featureSeg.getTranslationModifierName();
			if(translationModifierName == null) {
				uncodedRegions.add(featureSeg.asReferenceSegment());
			} else {
				TranslationModifier translationModifier;
				try {
					translationModifier = Module.resolveModulePlugin(cmdContext, TranslationModifier.class, translationModifierName);
				} catch(GlueException ge) {
					throw new FeatureLocationException(ge, FeatureLocationException.Code.CODING_FEATURE_LOCATION_ERROR, 
							getReferenceSequence().getName(), feature.getName(), "Error resolving translation modifier: "+ge.getLocalizedMessage());
				
				}
				if(featureSeg.getCurrentLength() != translationModifier.getSegmentNtLength()) {
					throw new FeatureLocationException(FeatureLocationException.Code.CODING_FEATURE_LOCATION_ERROR, 
							getReferenceSequence().getName(), feature.getName(), "Segment of incorrect length ("+featureSeg.getCurrentLength()+
							") for translation modifier -- should be "+translationModifier.getSegmentNtLength());
				}
				
			}
		}
		List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
		for(LabeledCodon labeledCodon: labeledCodons) {
			uncodedRegions = ReferenceSegment.subtract(uncodedRegions, labeledCodon.getLcRefSegments());
		}
		// all regions of the coding feature location (without translation modifiers) must be part of a labeled codon.
		if(!uncodedRegions.isEmpty()) {
			throw new FeatureLocationException(FeatureLocationException.Code.CODING_FEATURE_LOCATION_HAS_UNCODED_REGIONS, 
					getReferenceSequence().getName(), feature.getName(), uncodedRegions.toString());
		}
		// every labeled codon which overlaps the feature location must overlap entirely.
		List<ReferenceSegment> featureRefSegs = new ArrayList<ReferenceSegment>();
		for(FeatureSegment featureSeg: featureSegs) {
			featureRefSegs.add(featureSeg.asReferenceSegment());
		}
		
		for(LabeledCodon labeledCodon: labeledCodons) {
			if(!ReferenceSegment.covers(featureRefSegs, labeledCodon.getLcRefSegments())) {
				throw new FeatureLocationException(FeatureLocationException.Code.CODING_FEATURE_LOCATION_DOES_NOT_COVER_CODON, 
						getReferenceSequence().getName(), feature.getName(), labeledCodon.getCodonLabel());
			}
		}

		
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

	private List<FeatureSegment> getRotatedReferenceSegments() {
		List<FeatureSegment> featureLocRefSegs = new ArrayList<FeatureSegment>(getSegments());
		featureLocRefSegs.sort(new Comparator<FeatureSegment>() {
			@Override
			public int compare(FeatureSegment o1, FeatureSegment o2) {
				int comp = Integer.compare(o1.getTranscriptionIndex(), o2.getTranscriptionIndex());
				if(comp != 0) {
					return comp;
				}
				return Integer.compare(o1.getRefStart(), o2.getRefStart());
			}
		});
		return featureLocRefSegs;
	}
	
	
	public List<LabeledQueryAminoAcid> getReferenceAminoAcidContent(CommandContext cmdContext) {
		// feature area coordinates.
		List<FeatureSegment> featureLocRefSegs = getRotatedReferenceSegments();
		
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
		boolean revComp = getFeature().reverseComplementTranslation();
		if(this.getSimpleCodingFeature(cmdContext)) {
			// use faster code path in the common case where there are no complications.
			ReferenceSegment singleRefSeg = this.getSegments().get(0).asReferenceSegment();
			List<QueryAlignedSegment> queryToRefFeatureSegs = ReferenceSegment.intersection(queryToRefSegs, Arrays.asList(singleRefSeg), ReferenceSegment.cloneLeftSegMerger());
			List<LabeledQueryAminoAcid> labeledQueryAminoAcids = new ArrayList<LabeledQueryAminoAcid>();
			TIntObjectMap<LabeledCodon> refNtToLabeledCodon = getStartRefNtToLabeledCodon(cmdContext);
			SimpleLabeledCodon labeledCodon = null;
			char[] nextAaNts = new char[3];
			Integer queryNtStart = null;
			Integer queryNtMiddle = null;
			Integer queryNtEnd = null;
			
			for(QueryAlignedSegment queryToRefFeatureSeg: queryToRefFeatureSegs) {
				for(int offset = 0; offset < queryToRefFeatureSeg.getCurrentLength(); offset++) {
					int refNt = queryToRefFeatureSeg.getRefStart()+offset;
					SimpleLabeledCodon foundLabeledCodon = (SimpleLabeledCodon) refNtToLabeledCodon.get(refNt);
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
								String queryNts = new String(nextAaNts);
								String ntsToTranslate = queryNts;
								if(revComp) {
									ntsToTranslate = FastaUtils.reverseComplement(ntsToTranslate);
								}
								AmbigNtTripletInfo ambigNtTripletInfo = translator.translate(ntsToTranslate).get(0);
								LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(labeledCodon, ambigNtTripletInfo);
								labeledQueryAminoAcids.add(new LabeledQueryAminoAcid(labeledAminoAcid, 
										Arrays.asList(queryNtStart, queryNtMiddle, queryNtEnd), queryNts));
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
			// List of reference segments, with each one associated with a labeled codon.
			// May be multiple reference segments per labeled codon
			List<LabeledCodonReferenceSegment> labeledCodonReferenceSegments = this.getLabeledCodonReferenceSegments(cmdContext);
			// have to do this, otherwise the intersection call will not work properly.
			ReferenceSegment.sortByRefStart(labeledCodonReferenceSegments);
			ReferenceSegment.sortByRefStart(queryToRefSegs);
			
			List<LabeledQueryAminoAcid> labeledQueryAminoAcids = new ArrayList<LabeledQueryAminoAcid>();
			
			List<FeatureSegment> rotatedSegsNoModifier = new ArrayList<FeatureSegment>();
			
			for(FeatureSegment featureSegment: getSegments()) {
				String translationModifierName = featureSegment.getTranslationModifierName();
				if(translationModifierName == null) {
					rotatedSegsNoModifier.add(featureSegment);
				} else {
					// handle segment with modifier
					TranslationModifier translationModifier = Module.resolveModulePlugin(cmdContext, TranslationModifier.class, translationModifierName);
					if(translationModifier.getSegmentNtLength() != featureSegment.getCurrentLength()) {
						throw new TranslationModifierException(TranslationModifierException.Code.MODIFICATION_ERROR, "Feature segment length must match translation modifier length");
					}
					List<QueryAlignedSegment> featureSegQueryToRefSegs = 
							ReferenceSegment.intersection(queryToRefSegs, Arrays.asList(featureSegment), ReferenceSegment.cloneLeftSegMerger());
					if(ReferenceSegment.covers(featureSegQueryToRefSegs, Arrays.asList(featureSegment))) {
						TIntIntMap refNtToQueryNt = new TIntIntHashMap();
						List<Character> inputNTs = new LinkedList<Character>();
						for(QueryAlignedSegment qaSeg: featureSegQueryToRefSegs) {
							int queryToReferenceOffset = qaSeg.getQueryToReferenceOffset();
							for(int i = qaSeg.getQueryStart(); i <= qaSeg.getQueryEnd(); i++) {
								inputNTs.add(queryNucleotideContent.nt(cmdContext, i));
								refNtToQueryNt.put(i+queryToReferenceOffset, i);
							}
						}
						translationModifier.applyModifierRules(inputNTs);
						List<LabeledCodonReferenceSegment> featureLcRefSegs = ReferenceSegment.intersection(labeledCodonReferenceSegments, 
								Arrays.asList(featureSegment), ReferenceSegment.cloneLeftSegMerger());

						LinkedHashSet<LabeledCodon> featureLcs = new LinkedHashSet<LabeledCodon>();
						for(LabeledCodonReferenceSegment lcRefSeg: featureLcRefSegs) {
							featureLcs.add(lcRefSeg.getLabeledCodon());
						}
						if(featureLcs.size() != translationModifier.getOutputAminoAcids().size()) {
							// sanity check really.
							throw new TranslationModifierException(TranslationModifierException.Code.MODIFICATION_ERROR, "Unexpected number of labeled codons on segment with translation modifier");
						}
						int startNtIndex = 0;
						for(LabeledCodon labeledCodon: featureLcs) {
							if(!(labeledCodon instanceof ModifiedLabeledCodon)) {
								// sanity check really.
								throw new TranslationModifierException(TranslationModifierException.Code.MODIFICATION_ERROR, "Expected modified labeled codons on segment with translation modifier");
							}
							ModifiedLabeledCodon modifiedLc = (ModifiedLabeledCodon) labeledCodon;
							char[] nts = new char[3];
							for(int i = 0; i < 3; i++) {
								nts[i] = inputNTs.get(startNtIndex+i);
							}
							String queryNts = new String(nts);
							String ntsToTranslate = queryNts;
							if(revComp) {
								ntsToTranslate = FastaUtils.reverseComplement(ntsToTranslate);
							}
							AmbigNtTripletInfo translationInfo = translator.translate(ntsToTranslate).get(0);
							LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(modifiedLc, translationInfo);
							List<Integer> dependentQueryPositions = new ArrayList<Integer>();
							for(Integer dependentRefNtPosition: modifiedLc.getDependentRefNts()) {
								dependentQueryPositions.add(refNtToQueryNt.get(dependentRefNtPosition));
							}
							labeledQueryAminoAcids.add(new LabeledQueryAminoAcid(labeledAminoAcid, dependentQueryPositions, queryNts));
							startNtIndex+=3;
						}
					}
					
					
				}
			}
			
			List<QueryAlignedSegment> featureSegQueryToRefSegs = 
					ReferenceSegment.intersection(queryToRefSegs, rotatedSegsNoModifier, ReferenceSegment.cloneLeftSegMerger());
			// use the intersection function to (a) intersect queryToRefSegs with labeledCodonReferenceSegments
			// (b) produce LabeledCodonQueryAlignedSegment, i.e. qaSegs annotated with labeled codons.
			List<LabeledCodonQueryAlignedSegment> lcQaSegs = 
					ReferenceSegment.intersection(featureSegQueryToRefSegs, labeledCodonReferenceSegments,
							new BiFunction<QueryAlignedSegment, LabeledCodonReferenceSegment, LabeledCodonQueryAlignedSegment>() {
						@Override
						public LabeledCodonQueryAlignedSegment apply(
								QueryAlignedSegment qaSeg,
								LabeledCodonReferenceSegment lcRefSeg) {
							LabeledCodonQueryAlignedSegment lcQaSeg = new LabeledCodonQueryAlignedSegment(lcRefSeg.getLabeledCodon(), 
									qaSeg.getRefStart(), qaSeg.getRefEnd(), 
									qaSeg.getQueryStart(), qaSeg.getQueryEnd());
							// these lines ensure that the merged segment covers only the reference intersection.
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


			labeledCodonToLcQaSegs.keySet().forEach(labeledCodon -> {
				List<LabeledCodonQueryAlignedSegment> codonLcQaSegs = labeledCodonToLcQaSegs.get(labeledCodon);
				if(ReferenceSegment.covers(codonLcQaSegs, labeledCodon.getLcRefSegments())) {
					SimpleLabeledCodon simpleLc = ((SimpleLabeledCodon) labeledCodon);
					char[] nts = new char[3];
					Integer queryNtStart = null;
					Integer queryNtMiddle = null;
					Integer queryNtEnd = null;
					for(LabeledCodonQueryAlignedSegment lcQaSeg : codonLcQaSegs) {
						for(int i = 0; i < lcQaSeg.getCurrentLength(); i++) {
							if(lcQaSeg.getRefStart()+i == simpleLc.getNtStart()) {
								queryNtStart = lcQaSeg.getQueryStart()+i;
								nts[0] = queryNucleotideContent.nt(cmdContext, queryNtStart);
							} else if(lcQaSeg.getRefStart()+i == simpleLc.getNtMiddle()) {
								queryNtMiddle = lcQaSeg.getQueryStart()+i;
								nts[1] = queryNucleotideContent.nt(cmdContext, queryNtMiddle);
							} else if(lcQaSeg.getRefStart()+i == simpleLc.getNtEnd()) {
								queryNtEnd = lcQaSeg.getQueryStart()+i;
								nts[2] = queryNucleotideContent.nt(cmdContext, queryNtEnd);
							} 
						}
					}
					String queryNts = new String(nts);
					String ntsToTranslate = queryNts;
					if(revComp) {
						ntsToTranslate = FastaUtils.reverseComplement(ntsToTranslate);
					}
					AmbigNtTripletInfo ambigNtTripletInfo = translator.translate(ntsToTranslate).get(0);
					LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(simpleLc, ambigNtTripletInfo);
					LabeledQueryAminoAcid lqaa = new LabeledQueryAminoAcid(labeledAminoAcid, Arrays.asList(queryNtStart, queryNtMiddle, queryNtEnd), queryNts);
					labeledQueryAminoAcids.add(lqaa);
				};
			});

			labeledQueryAminoAcids.sort(new Comparator<LabeledQueryAminoAcid>() {
				@Override
				public int compare(LabeledQueryAminoAcid o1, LabeledQueryAminoAcid o2) {
					return Integer.compare(o1.getLabeledAminoAcid().getLabeledCodon().getTranslationIndex(), 
							o2.getLabeledAminoAcid().getLabeledCodon().getTranslationIndex());
				}
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

