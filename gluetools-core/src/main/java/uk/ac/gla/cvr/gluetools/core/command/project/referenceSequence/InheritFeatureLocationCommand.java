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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.InheritFeatureLocationCommand.InheritFeatureLocationResult;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.InheritFeatureLocationException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"inherit", "feature-location"}, 
		docoptUsages={"[-r] [-s] [-t] <alignmentName> [-l <relRefName>] <featureName>"},
		docoptOptions={"-r, --recursive                             Add locations for the feature's descendents",
   				       "-s, --spanGaps                              New locations should span any gaps in the alignment",
				       "-t, --truncateCdn                           For coding features, truncate to codon-aligned",
				       "-l <relRefName>, --relRefName <relRefName>  Related reference within the same alignment"},
		description="Inherit a feature location from another reference sequence via an alignment", 
		furtherHelp="This command adds feature locations to the reference sequence, based on the feature locations "+
		"of another reference sequence within the same specified alignment. A location for the named feature and each of its ancestors will be added, as long as "+
		"no feature location already exists with that name. "+
		"If <relRefName> is not given, it is presumed that the specifed alignment is constrained and the "+
		"constraining reference is used. "+
		"Both reference sequences must be members of the specified alignment; the new location segments are derived from this alignment membership. "+
		"If the recursive option is used, this means that a location will not only be inherited for the named feature, but "+
		"also for any child features of the named feature, their children, etc. ") 
public class InheritFeatureLocationCommand extends ReferenceSequenceModeCommand<InheritFeatureLocationResult>{

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String REL_REF_NAME = "relRefName";
	public static final String SPAN_GAPS = "spanGaps";
	public static final String TRUNCATE_CODON = "truncateCdn";

	private String alignmentName;
	private String featureName;
	private String relRefName;
	private boolean recursive;
	private boolean spanGaps;
	private boolean truncateCdn;
	
	// result column headers;
	private static final String ADDED_FEATURE_NAME = "addedFeatureName"; 
	private static final String NUM_ADDED_SEGMENTS = "numAddedSegments"; 
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.spanGaps = PluginUtils.configureBooleanProperty(configElem, SPAN_GAPS, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.truncateCdn = PluginUtils.configureBooleanProperty(configElem, TRUNCATE_CODON, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, false);
	}


	@Override
	public InheritFeatureLocationResult execute(CommandContext cmdContext) {
		ReferenceSequence targetRefSeq = lookupRefSeq(cmdContext);
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Sequence targetRefSeqSeq = targetRefSeq.getSequence();
		// find this reference sequence's membership of the named alignment.
		AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(alignment.getName(), targetRefSeqSeq.getSource().getName(), targetRefSeqSeq.getSequenceID()), true);
		if(almtMember == null) {
			throw new InheritFeatureLocationException(Code.NOT_MEMBER_OF_ALIGNMENT, targetRefSeq.getName(), alignmentName);
		}
		Feature mainFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		ReferenceSequence relatedRef;
		// query aligned segments with target ref as query and rel ref as reference
		List<QueryAlignedSegment> targetRefToRelRefSegs = 
				almtMember.getAlignedSegments().stream()
				.map(aSeg -> aSeg.asQueryAlignedSegment()).collect(Collectors.toList());
		if(relRefName != null) {
			relatedRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relRefName), false);
			targetRefToRelRefSegs = alignment.translateToRelatedRef(cmdContext, targetRefToRelRefSegs, relatedRef);
		} else {
			relatedRef = alignment.getRefSequence();
		}
		if(relatedRef == null) {
			throw new InheritFeatureLocationException(Code.PARENT_ALIGNMENT_IS_UNCONSTRAINED, alignmentName);
		}
		ReferenceSegment.sortByRefStart(targetRefToRelRefSegs);
		
		List<Feature> features = new ArrayList<Feature>();
		features.add(mainFeature);
		if(recursive) {
			features.addAll(mainFeature.getDescendents());
		};
		Map<String, Integer> featureNameToAddedSegments = new LinkedHashMap<String, Integer>();
		for(Feature feature: features) {
			if(!feature.isInformational()) {
				addFeatureLocs(cmdContext, targetRefSeq, relatedRef, targetRefToRelRefSegs, feature, featureNameToAddedSegments);
			}
		}
		List<Map<String, Object>> resultRowData = new ArrayList<Map<String, Object>>();
		featureNameToAddedSegments.forEach((rowFeatureName, numAddedSegments) -> {
			Map<String, Object> resultRow = new LinkedHashMap<String, Object>();
			resultRow.put(ADDED_FEATURE_NAME, rowFeatureName);
			resultRow.put(NUM_ADDED_SEGMENTS, numAddedSegments);
			resultRowData.add(resultRow);
		});
		return new InheritFeatureLocationResult(resultRowData);
	}
	
	private class RefSegWithModifierName {
		ReferenceSegment refSeg;
		String modifierName;
		public RefSegWithModifierName(ReferenceSegment refSeg, String modifierName) {
			super();
			this.refSeg = refSeg;
			this.modifierName = modifierName;
		}
	}

	private class RefSegWithSpliceIndex {
		ReferenceSegment refSeg;
		Integer spliceIndex;
		public RefSegWithSpliceIndex(ReferenceSegment refSeg, Integer spliceIndex) {
			super();
			this.refSeg = refSeg;
			this.spliceIndex = spliceIndex;
		}
	}

	
	private void addFeatureLocs(CommandContext cmdContext, 
			ReferenceSequence targetRefSeq, 
			ReferenceSequence relatedRefSeq, 
			List<QueryAlignedSegment> targetRefToRelRefSegs,
			Feature feature, Map<String, Integer> featureNameToAddedSegments) {
		String featureName = feature.getName();
		Map<String, String> featureLocPkMap = FeatureLocation.pkMap(targetRefSeq.getName(), featureName);
		FeatureLocation targetRefFeatureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(targetRefSeq.getName(), featureName), true);
		FeatureLocation relRefFeatureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRefSeq.getName(), featureName));

		List<FeatureSegment> relRefFeatureLocSegs = relRefFeatureLoc.getSegments();
		ReferenceSegment.sortByRefStart(relRefFeatureLocSegs);	
		
		if(targetRefFeatureLoc == null && !feature.isInformational()) {
			
			
			// special treatment of feature segments with translation modification.
			List<RefSegWithModifierName> newRefSegsWithModifierNames = new ArrayList<RefSegWithModifierName>();
			List<FeatureSegment> relRefFeatureSegsWithMod = new ArrayList<FeatureSegment>();
			List<FeatureSegment> relRefFeatureSegsWithoutMod = new ArrayList<FeatureSegment>();
			for(FeatureSegment relRefFeatureSeg: relRefFeatureLocSegs) {
				if(relRefFeatureSeg.getTranslationModifierName() == null) {
					relRefFeatureSegsWithoutMod.add(relRefFeatureSeg);
				} else {
					relRefFeatureSegsWithMod.add(relRefFeatureSeg);
				}
			}
			for(FeatureSegment relRefFeatureSegWithMod: relRefFeatureSegsWithMod) {
				List<QueryAlignedSegment> qaSegsWithMod =
						ReferenceSegment.intersection(Arrays.asList(relRefFeatureSegWithMod), targetRefToRelRefSegs, ReferenceSegment.cloneRightSegMerger());
				if(qaSegsWithMod.size() == 0) {
					continue;
				}
				String modifierName = relRefFeatureSegWithMod.getTranslationModifierName();
				if(qaSegsWithMod.size() != 1) {
					throw new InheritFeatureLocationException(Code.TRANSLATION_MODIFICATION_ERROR, 
							"Unable to inherit feature segment "+relRefFeatureSegWithMod+
							" with translation modifier "+modifierName+
							" due to complex homology in that region: "+qaSegsWithMod);
				}
				QueryAlignedSegment qaSegWithMod = qaSegsWithMod.get(0);
				if(qaSegWithMod.getCurrentLength() != relRefFeatureSegWithMod.getCurrentLength()) {
					throw new InheritFeatureLocationException(Code.TRANSLATION_MODIFICATION_ERROR, 
							"Unable to inherit feature segment "+relRefFeatureSegWithMod+
							" with translation modifier "+modifierName+
							" due to mismatched lengths: "+qaSegsWithMod);
				}
				newRefSegsWithModifierNames.add(new RefSegWithModifierName(new ReferenceSegment(qaSegWithMod.getQueryStart(), qaSegWithMod.getQueryEnd()), 
						modifierName));
			}
			
			// intersect the aligned segments of this ref seq in the alignment with the
			// feature location segments of the parent ref seq for this feature.
			// this generates qa segments just for this feature.
			List<QueryAlignedSegment> newFeatureQaSegs =
					ReferenceSegment.intersection(relRefFeatureSegsWithoutMod, targetRefToRelRefSegs, ReferenceSegment.cloneRightSegMerger());

			if(truncateCdn && feature.codesAminoAcids()) {
				// delete anything that falls outside of a codon
				List<LabeledCodon> labeledCodons = relRefFeatureLoc.getLabeledCodons(cmdContext);
				for(LabeledCodon labeledCodon: labeledCodons) {
					List<LabeledCodonReferenceSegment> lcRefSegments = labeledCodon.getLcRefSegments();
					List<QueryAlignedSegment> lcRefIntersection = ReferenceSegment.intersection(newFeatureQaSegs, lcRefSegments, ReferenceSegment.cloneLeftSegMerger());
					if(!lcRefIntersection.isEmpty()) { // some overlap with codon
						if(!ReferenceSegment.covers(lcRefIntersection, lcRefSegments)) { // but doesn't cover it all
 							newFeatureQaSegs = ReferenceSegment.subtract(newFeatureQaSegs, lcRefIntersection);
						}
					}
				}
			}
			
			Integer newStartTranscription = null;
			if(feature.circularBridging()) {
				int refStartTranscription = relRefFeatureLoc.getStartTranscription();
				// partition the intersection into potentially two parts (1) which is post the transcription start point
				// on the genome and (2) which is pre the transcription start point
				List<QueryAlignedSegment> intersection1 = new ArrayList<QueryAlignedSegment>(); 
				List<QueryAlignedSegment> intersection2 = new ArrayList<QueryAlignedSegment>(); 
				
				for(QueryAlignedSegment qaSeg: newFeatureQaSegs) {
					if(qaSeg.getRefStart() >= refStartTranscription) {
						intersection1.add(qaSeg);
					} else {
						intersection2.add(qaSeg);
					}
				}
				intersection1 = applySpanning(intersection1, relRefFeatureSegsWithoutMod);
				intersection2 = applySpanning(intersection2, relRefFeatureSegsWithoutMod);
				newFeatureQaSegs = new ArrayList<QueryAlignedSegment>(); 
				newFeatureQaSegs.addAll(intersection2);
				newFeatureQaSegs.addAll(intersection1);
				newStartTranscription = QueryAlignedSegment.minQueryStart(intersection1);
				if(newStartTranscription == null) {
					newStartTranscription = QueryAlignedSegment.minQueryStart(intersection2);
				}
				
			} else {
				newFeatureQaSegs = applySpanning(newFeatureQaSegs, relRefFeatureSegsWithoutMod);
			}

			if(newFeatureQaSegs.size() == 0) {
				return; // no segments // nothing added.
			}

			// result, but as reference segments.
			List<ReferenceSegment> newFeatureRefSegs = newFeatureQaSegs
					.stream()
					.map(qaseg -> new ReferenceSegment(qaseg.getQueryStart(), qaseg.getQueryEnd()))
					.collect(Collectors.toList());
			
			// add any new ref segs with modifiers into the set.
			for(RefSegWithModifierName newRefSegWithModifierName: newRefSegsWithModifierNames) {
				newFeatureRefSegs.add(newRefSegWithModifierName.refSeg);
			}
			ReferenceSegment.sortByRefStart(newFeatureRefSegs);
			
			// create necessary ancestor feature locations and segments.
			List<Feature> ancestorFeatures = feature.getAncestors();
			for(Feature ancestorFeature: ancestorFeatures) {
				String ancestorFeatureName = ancestorFeature.getName();
				if(!ancestorFeatureName.equals(featureName)) {
					if(!ancestorFeature.isInformational()) {
						Map<String, String> ancestorFLocPkMap = FeatureLocation.pkMap(targetRefSeq.getName(), ancestorFeatureName);
						FeatureLocation ancestorFeatureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, ancestorFLocPkMap, true);
						if(ancestorFeatureLoc == null) {
							ancestorFeatureLoc = GlueDataObject.create(cmdContext, FeatureLocation.class, ancestorFLocPkMap, false);
							ancestorFeatureLoc.setFeature(ancestorFeature);
							ancestorFeatureLoc.setReferenceSequence(targetRefSeq);
							cmdContext.commit();
						}
						// find those areas not already covered
						List<ReferenceSegment> segmentsToAdd = ReferenceSegment.subtract(newFeatureRefSegs, ancestorFeatureLoc.getSegments());
						int numAddedSegments = commitFeatureLocSegments(cmdContext,
								targetRefSeq, ancestorFeatureName, ancestorFeatureLoc,
								segmentsToAdd, newRefSegsWithModifierNames);
						Integer currentAddedSegs = featureNameToAddedSegments.get(ancestorFeatureName);
						if(currentAddedSegs == null) {
							currentAddedSegs = 0;
						}
						currentAddedSegs += numAddedSegments;
						featureNameToAddedSegments.put(ancestorFeatureName, currentAddedSegs);
					}
				}
			}

			
			targetRefFeatureLoc = GlueDataObject.create(cmdContext, FeatureLocation.class, featureLocPkMap, false);
			targetRefFeatureLoc.setFeature(feature);
			targetRefFeatureLoc.setReferenceSequence(targetRefSeq);
			if(newStartTranscription != null) {
				targetRefFeatureLoc.writeProperty(feature.flocStartTranscriptionField(), newStartTranscription);
			}
			cmdContext.commit();

			int numAddedSegments = commitFeatureLocSegments(cmdContext,
					targetRefSeq, featureName, targetRefFeatureLoc,
					newFeatureRefSegs, newRefSegsWithModifierNames);
			Integer currentAddedSegs = featureNameToAddedSegments.get(featureName);
			if(currentAddedSegs == null) {
				currentAddedSegs = 0;
			}
			currentAddedSegs += numAddedSegments;
			featureNameToAddedSegments.put(featureName, currentAddedSegs);
		}
		
	}


	private List<QueryAlignedSegment> applySpanning(List<QueryAlignedSegment> newFeatureQaSegs, List<FeatureSegment> relRefFeatureSegs) {
		if(!spanGaps) {
			return newFeatureQaSegs;
		}
		// only span across qaSegs that relate to feature segs with the same splice index.
		List<QueryAlignedSegment> qaSegsPostSpanning = new ArrayList<QueryAlignedSegment>();
		Map<Integer, List<FeatureSegment>> spliceIndexToFeatureSegs = relRefFeatureSegs.stream().collect(Collectors.groupingBy(FeatureSegment::getSpliceIndex));
		for(List<FeatureSegment> featureSegsForSpliceIndex : spliceIndexToFeatureSegs.values()) {
			List<QueryAlignedSegment> qaSegsForSpliceIndex = 
					ReferenceSegment.intersection(newFeatureQaSegs, featureSegsForSpliceIndex, ReferenceSegment.cloneLeftSegMerger());

			if(qaSegsForSpliceIndex.size() > 0) {
				QueryAlignedSegment firstSeg = qaSegsForSpliceIndex.get(0);
				QueryAlignedSegment lastSeg = qaSegsForSpliceIndex.get(qaSegsForSpliceIndex.size() - 1);
				
				// ref points are pretty irrelevant here, as only the query points are used.
				QueryAlignedSegment spanningSegment = new QueryAlignedSegment(firstSeg.getRefStart(), lastSeg.getRefEnd(), 
						firstSeg.getQueryStart(), lastSeg.getQueryEnd());
				qaSegsPostSpanning.add(spanningSegment);
			}
		}
		return qaSegsPostSpanning;
	}


	private int commitFeatureLocSegments(CommandContext cmdContext,
			ReferenceSequence thisRefSeq, String featureName,
			FeatureLocation currentFeatureLoc,
			List<ReferenceSegment> newFeatureRefSegs, List<RefSegWithModifierName> newRefSegsWithModifierNames) {
		int numAddedSegments = 0;
		// add segments to the new feature location based on the query start/end points of the intersection result.
		for(ReferenceSegment newFeatureRefSeg: newFeatureRefSegs) {
			Integer refStart = newFeatureRefSeg.getRefStart();
			Integer refEnd = newFeatureRefSeg.getRefEnd();
			FeatureSegment featureSegment = GlueDataObject.create(cmdContext, FeatureSegment.class, 
					FeatureSegment.pkMap(thisRefSeq.getName(), featureName, 
							refStart, refEnd), false);
			featureSegment.setFeatureLocation(currentFeatureLoc);
			if(currentFeatureLoc.getFeature().codesAminoAcids()) {
				for(RefSegWithModifierName refSegWithModifierName: newRefSegsWithModifierNames) {
					if(refSegWithModifierName.refSeg.getRefStart().equals(refStart) && refSegWithModifierName.refSeg.getRefEnd().equals(refEnd)) {
						featureSegment.setTranslationModifierName(refSegWithModifierName.modifierName);
					}
				}
			}
			cmdContext.commit();
			numAddedSegments++;
		}
		return numAddedSegments;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			
			
			registerVariableInstantiator("relRefName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String alignmentName = (String) bindings.get("alignmentName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), false);
					if(alignment != null) {
						return alignment.getRelatedRefs().stream()
								.map(ancCR -> new CompletionSuggestion(ancCR.getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("relRefName");
					ReferenceSequence referenceSequence = null;
					if(referenceName != null) {
						referenceSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), true);
					} else {
						String alignmentName = (String) bindings.get("alignmentName");
						Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), false);
						if(alignment != null && alignment.isConstrained()) {
							referenceSequence = alignment.getConstrainingRef();
						}
					}
					if(referenceSequence != null) {
						return referenceSequence.getFeatureLocations().stream()
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			
		}
	}
	
	
	public static class InheritFeatureLocationResult extends TableResult {
		public InheritFeatureLocationResult(List<Map<String, Object>> rowData) {
			super("inheritFeatureLocationResult", Arrays.asList(ADDED_FEATURE_NAME, NUM_ADDED_SEGMENTS), rowData);
		}
	}
	
}
