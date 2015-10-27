package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
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

@CommandClass(
		commandWords={"inherit", "feature-location"}, 
		docoptUsages={"[-r] [-s] <alignmentName> <featureName>"},
		docoptOptions={"-r, --recursive  Add locations for the feature's descendents",
					   "-s, --spanGaps  New locations should span any gaps in the alignment"},
		description="Inherit a feature location from parent reference", 
		furtherHelp="This command adds feature locations to the reference sequence, based on the feature locations "+
		"of a given alignment's reference sequence. A location for the named feature and each of its ancestors will be added, as long as "+
		"no feature location already exists with that name. "+
		"The reference sequence must be a member of the specified alignment; the new location segments are derived from this alignment membership. "+
		"If the recursive option is used, this means that a location will not only be inherited for the named feature, but "+
		"also for any child features of the named feature, their children, etc. ") 
public class InheritFeatureLocationCommand extends ReferenceSequenceModeCommand<InheritFeatureLocationResult>{

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String SPAN_GAPS = "spanGaps";

	private String alignmentName;
	private String featureName;
	private boolean recursive;
	private boolean spanGaps;
	
	// result column headers;
	private static final String ADDED_FEATURE_NAME = "addedFeatureName"; 
	private static final String NUM_ADDED_SEGMENTS = "numAddedSegments"; 
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.spanGaps = PluginUtils.configureBooleanProperty(configElem, SPAN_GAPS, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
	}


	@Override
	public InheritFeatureLocationResult execute(CommandContext cmdContext) {
		ReferenceSequence thisRefSeq = lookupRefSeq(cmdContext);
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Sequence thisRefSeqSeq = thisRefSeq.getSequence();
		// find this reference sequence's membership of the parent alignment.
		AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(alignment.getName(), thisRefSeqSeq.getSource().getName(), thisRefSeqSeq.getSequenceID()), true);
		if(almtMember == null) {
			throw new InheritFeatureLocationException(Code.NOT_MEMBER_OF_ALIGNMENT, thisRefSeq.getName(), alignmentName);
		}
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		ReferenceSequence parentRefSeq = alignment.getRefSequence();
		if(parentRefSeq == null) {
			throw new InheritFeatureLocationException(Code.PARENT_ALIGNMENT_IS_UNCONSTRAINED, alignmentName);
		}
		ReferenceFeatureTreeResult parentRefFeatureTree = parentRefSeq.getFeatureTree(cmdContext, feature, recursive);
		
		
		List<Map<String, Object>> resultRowData = new ArrayList<Map<String, Object>>();
		for(ReferenceFeatureTreeResult featureTreeResult : parentRefFeatureTree.getChildTrees()) {
			addFeatureLocs(cmdContext, thisRefSeq, parentRefSeq, almtMember, featureTreeResult, resultRowData);
		}
		return new InheritFeatureLocationResult(resultRowData);
	}
	
	
	private void addFeatureLocs(CommandContext cmdContext, 
			ReferenceSequence thisRefSeq, 
			ReferenceSequence parentRefSeq, 
			AlignmentMember almtMember,
			ReferenceFeatureTreeResult featureTreeResult, List<Map<String, Object>> resultRowData) {
		String featureName = featureTreeResult.getFeatureName();
		
		Feature currentFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		Map<String, String> featureLocPkMap = FeatureLocation.pkMap(thisRefSeq.getName(), featureName);
		FeatureLocation currentFeatureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, featureLocPkMap, true);
		if(currentFeatureLoc == null && !currentFeature.isInformational()) {
			currentFeatureLoc = GlueDataObject.create(cmdContext, FeatureLocation.class,  featureLocPkMap, false);
			currentFeatureLoc.setFeature(currentFeature);
			currentFeatureLoc.setReferenceSequence(thisRefSeq);
			cmdContext.commit();
			
			// intersect the aligned segments of this ref seq in the parent alignment with the
			// feature location segments of the parent ref seq for this feature.
			List<ReferenceSegment> parentFeatureLocSegs = featureTreeResult.getReferenceSegments();
			
			List<AlignedSegment> refParentAlignedSegments = almtMember.getAlignedSegments();

			// this generates aligned segments just for this feature.
			List<QueryAlignedSegment> intersection = 
					ReferenceSegment.intersection(parentFeatureLocSegs, refParentAlignedSegments, new SegMerger());
			
			if(spanGaps && intersection.size() > 1) {
				QueryAlignedSegment firstSeg = intersection.get(0);
				QueryAlignedSegment lastSeg = intersection.get(intersection.size() - 1);
				
				// ref points are pretty irrelevant here, as only the query points are used.
				QueryAlignedSegment spanningSegment = new QueryAlignedSegment(firstSeg.getRefStart(), lastSeg.getRefEnd(), 
						firstSeg.getQueryStart(), lastSeg.getQueryEnd());
				intersection = Collections.singletonList(spanningSegment);
			}
			
			int numAddedSegments = 0;
			// add segments to the new feature location based on the query start/end points of the intersection result.
			for(QueryAlignedSegment intersectSeg: intersection) {
				FeatureSegment featureSegment = GlueDataObject.create(cmdContext, FeatureSegment.class, 
						FeatureSegment.pkMap(thisRefSeq.getName(), featureName, 
								intersectSeg.getQueryStart(), intersectSeg.getQueryEnd()), false);
				featureSegment.setFeatureLocation(currentFeatureLoc);
				cmdContext.commit();
				numAddedSegments++;
			}
			Map<String, Object> resultRow = new LinkedHashMap<String, Object>();
			resultRow.put(ADDED_FEATURE_NAME, featureName);
			resultRow.put(NUM_ADDED_SEGMENTS, new Integer(numAddedSegments));
			resultRowData.add(resultRow);
		}
		for(ReferenceFeatureTreeResult childTreeResult : featureTreeResult.getChildTrees()) {
			addFeatureLocs(cmdContext, thisRefSeq, parentRefSeq, almtMember, childTreeResult, resultRowData);
		}		
		
	}

	private static class SegMerger implements BiFunction<ReferenceSegment, AlignedSegment, QueryAlignedSegment> {

		@Override
		public QueryAlignedSegment apply(ReferenceSegment parentFeatureLocSeg, AlignedSegment refParentAlignedSeg) {
			
			int refStart = Math.max(parentFeatureLocSeg.getRefStart(), refParentAlignedSeg.getRefStart());
			int refEnd = Math.min(parentFeatureLocSeg.getRefEnd(), refParentAlignedSeg.getRefEnd());
			int queryStart = refStart + refParentAlignedSeg.getReferenceToQueryOffset();
			int queryEnd = refEnd + refParentAlignedSeg.getReferenceToQueryOffset();
			return new QueryAlignedSegment(refStart, refEnd, queryStart, queryEnd);
		}
		
	}
	
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("featureName", 
					new VariableInstantiator() {
						@Override
						@SuppressWarnings("rawtypes")
						protected List<CompletionSuggestion> instantiate(
								ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
								Map<String, Object> bindings, String prefix) {
							String alignmentName = (String) bindings.get("alignmentName");
							Alignment almt = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), true);
							if(almt != null) {
								ReferenceSequence refSequence = almt.getRefSequence();
								if(refSequence != null) {
									return refSequence.getFeatureLocations().stream()
											.map(fl -> new CompletionSuggestion(fl.getFeature().getName(), true))
											.collect(Collectors.toList());
								}
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
