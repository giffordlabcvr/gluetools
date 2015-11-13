package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;

public class AlignmentAnalysisCommandCompleter extends AdvancedCmdCompleter {
	public AlignmentAnalysisCommandCompleter() {
		super();
		registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
		registerVariableInstantiator("referenceName", new VariableInstantiator() {
			@SuppressWarnings("rawtypes")
			@Override
			protected List<CompletionSuggestion> instantiate(
					ConsoleCommandContext cmdContext,
					Class<? extends Command> cmdClass, Map<String, Object> bindings,
					String prefix) {
				String alignmentName = (String) bindings.get("alignmentName");
				Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), true);
				if(alignment != null) {
					if(alignment.getRefSequence() == null) {
						return null;
					}
					List<Alignment> ancestors = alignment.getAncestors();
					List<CompletionSuggestion> suggestions = new ArrayList<CompletionSuggestion>();
					for(Alignment ancestor: ancestors) {
						ReferenceSequence refSeq = ancestor.getRefSequence();
						if(refSeq != null) {
							suggestions.add(new CompletionSuggestion(refSeq.getName(), true));
						}
					}
					return suggestions;
				}
				return null;
			}
		});
		registerVariableInstantiator("featureName", new VariableInstantiator() {
			@SuppressWarnings("rawtypes")
			@Override
			protected List<CompletionSuggestion> instantiate(
					ConsoleCommandContext cmdContext,
					Class<? extends Command> cmdClass, Map<String, Object> bindings,
					String prefix) {
				String referenceName = (String) bindings.get("referenceName");
				ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), true);
				if(refSeq != null) {
					List<FeatureLocation> featureLocations = refSeq.getFeatureLocations();
					List<CompletionSuggestion> suggestions = new ArrayList<CompletionSuggestion>();
					for(FeatureLocation featureLoc: featureLocations) {
						Feature feature = featureLoc.getFeature();
						if(!feature.isInformational() && feature.getOrfAncestor() != null) {
							suggestions.add(new CompletionSuggestion(feature.getName(), true));
						}
					}
					return suggestions;
				}
				return null;
			}
		});
		
	}
	
	public static class VcatNameCompleter extends VariableInstantiator {

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" }) 
		protected List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, Map<String, Object> bindings,
				String prefix) {
			Set<String> currentSet = new LinkedHashSet<String>();
			Object current =  bindings.get("vcatName");
			if(current != null && current instanceof List) {
				currentSet.addAll((List<String>) current);
			}
			if(current != null && current instanceof String) {
				currentSet.add((String) current);
			}
			List<CompletionSuggestion> allVCatNames = AdvancedCmdCompleter.listNames(cmdContext, prefix, VariationCategory.class, VariationCategory.NAME_PROPERTY);
			return allVCatNames.stream().filter(cs -> !currentSet.contains(cs.getSuggestedWord())).collect(Collectors.toList());
		}
		
	}
	
}