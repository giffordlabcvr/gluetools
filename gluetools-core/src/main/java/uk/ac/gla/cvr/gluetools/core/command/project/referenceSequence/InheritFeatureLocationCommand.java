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
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

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
		ReferenceSequence thisRefSeq = lookupRefSeq(cmdContext);
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Sequence thisRefSeqSeq = thisRefSeq.getSequence();
		// find this reference sequence's membership of the named alignment.
		AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(alignment.getName(), thisRefSeqSeq.getSource().getName(), thisRefSeqSeq.getSequenceID()), true);
		if(almtMember == null) {
			throw new InheritFeatureLocationException(Code.NOT_MEMBER_OF_ALIGNMENT, thisRefSeq.getName(), alignmentName);
		}
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		ReferenceSequence relatedRef;
		// query aligned segments with this ref as query and rel ref as reference
		List<QueryAlignedSegment> relRefAlignedSegments = 
				almtMember.getAlignedSegments().stream()
				.map(aSeg -> aSeg.asQueryAlignedSegment()).collect(Collectors.toList());
		if(relRefName != null) {
			relatedRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relRefName), false);
			relRefAlignedSegments = alignment.translateToRelatedRef(cmdContext, relRefAlignedSegments, relatedRef);
		} else {
			relatedRef = alignment.getRefSequence();
		}
		if(relatedRef == null) {
			throw new InheritFeatureLocationException(Code.PARENT_ALIGNMENT_IS_UNCONSTRAINED, alignmentName);
		}
		ReferenceFeatureTreeResult relatedRefFeatureTree = relatedRef.getFeatureTree(cmdContext, feature, recursive, false);
		
		
		List<Map<String, Object>> resultRowData = new ArrayList<Map<String, Object>>();
		for(ReferenceFeatureTreeResult featureTreeResult : relatedRefFeatureTree.getChildTrees()) {
			addFeatureLocs(cmdContext, thisRefSeq, relatedRef, relRefAlignedSegments, featureTreeResult, resultRowData);
		}
		return new InheritFeatureLocationResult(resultRowData);
	}
	
	
	private void addFeatureLocs(CommandContext cmdContext, 
			ReferenceSequence thisRefSeq, 
			ReferenceSequence relatedRefSeq, 
			List<QueryAlignedSegment> relRefAlignedSegments,
			ReferenceFeatureTreeResult featureTreeResult, List<Map<String, Object>> resultRowData) {
		String featureName = featureTreeResult.getFeatureName();
		
		Feature currentFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		Map<String, String> featureLocPkMap = FeatureLocation.pkMap(thisRefSeq.getName(), featureName);
		FeatureLocation currentFeatureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, featureLocPkMap, true);
		if(currentFeatureLoc == null && !currentFeature.isInformational()) {
			// intersect the aligned segments of this ref seq in the parent alignment with the
			// feature location segments of the parent ref seq for this feature.
			List<ReferenceSegment> relRefFeatureLocSegs = featureTreeResult.getReferenceSegments();
			
			ReferenceSegment.sortByRefStart(relRefAlignedSegments);
			
			// this generates aligned segments just for this feature.
			List<QueryAlignedSegment> intersection = 
					ReferenceSegment.intersection(relRefFeatureLocSegs, relRefAlignedSegments, ReferenceSegment.cloneRightSegMerger());
			
			if(spanGaps && intersection.size() > 1) {
				QueryAlignedSegment firstSeg = intersection.get(0);
				QueryAlignedSegment lastSeg = intersection.get(intersection.size() - 1);
				
				// ref points are pretty irrelevant here, as only the query points are used.
				QueryAlignedSegment spanningSegment = new QueryAlignedSegment(firstSeg.getRefStart(), lastSeg.getRefEnd(), 
						firstSeg.getQueryStart(), lastSeg.getQueryEnd());
				intersection = Collections.singletonList(spanningSegment);
			}
			
			if(intersection.size() == 0) {
				return; // no segments // nothing added.
			}

			List<QueryAlignedSegment> newFeatureQaSegs = intersection;

			if(truncateCdn && currentFeature.codesAminoAcids()) {
				// assumes parent feature loc is codon aligned
				Integer parentCodon1Start = ReferenceSegment.minRefStart(relRefFeatureLocSegs);
				Integer thisCodon1Start = newFeatureQaSegs.get(0).getReferenceToQueryOffset() + parentCodon1Start;
				newFeatureQaSegs = TranslationUtils.truncateToCodonAligned(thisCodon1Start, newFeatureQaSegs);
			}

			
			currentFeatureLoc = GlueDataObject.create(cmdContext, FeatureLocation.class,  featureLocPkMap, false);
			currentFeatureLoc.setFeature(currentFeature);
			currentFeatureLoc.setReferenceSequence(thisRefSeq);
			cmdContext.commit();

			
			int numAddedSegments = commitFeatureLocSegments(cmdContext,
					thisRefSeq, featureName, currentFeatureLoc,
					newFeatureQaSegs);



			
			Map<String, Object> resultRow = new LinkedHashMap<String, Object>();
			resultRow.put(ADDED_FEATURE_NAME, featureName);
			resultRow.put(NUM_ADDED_SEGMENTS, new Integer(numAddedSegments));
			resultRowData.add(resultRow);
		}
		for(ReferenceFeatureTreeResult childTreeResult : featureTreeResult.getChildTrees()) {
			addFeatureLocs(cmdContext, thisRefSeq, relatedRefSeq, relRefAlignedSegments, childTreeResult, resultRowData);
		}		
		
	}


	private int commitFeatureLocSegments(CommandContext cmdContext,
			ReferenceSequence thisRefSeq, String featureName,
			FeatureLocation currentFeatureLoc,
			List<QueryAlignedSegment> newFeatureQaSegs) {
		int numAddedSegments = 0;
		// add segments to the new feature location based on the query start/end points of the intersection result.
		for(QueryAlignedSegment newFeatureQaSeg: newFeatureQaSegs) {
			FeatureSegment featureSegment = GlueDataObject.create(cmdContext, FeatureSegment.class, 
					FeatureSegment.pkMap(thisRefSeq.getName(), featureName, 
							newFeatureQaSeg.getQueryStart(), newFeatureQaSeg.getQueryEnd()), false);
			featureSegment.setFeatureLocation(currentFeatureLoc);
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
