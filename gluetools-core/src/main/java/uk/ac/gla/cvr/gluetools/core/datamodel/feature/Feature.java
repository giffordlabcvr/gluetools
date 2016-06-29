package uk.ac.gla.cvr.gluetools.core.datamodel.feature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.CodonLabeler;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.HasDisplayName;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag.Type;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;

@GlueDataClass(
		defaultListedProperties = { _Feature.NAME_PROPERTY, 
				Feature.PARENT_NAME_PATH, _Feature.DESCRIPTION_PROPERTY }, 
		listableBuiltInProperties = { _Feature.NAME_PROPERTY, _Feature.DISPLAY_NAME_PROPERTY,
				Feature.PARENT_NAME_PATH, _Feature.DESCRIPTION_PROPERTY }, 
		modifiableBuiltInProperties = { _Feature.DESCRIPTION_PROPERTY, _Feature.DISPLAY_NAME_PROPERTY } )
public class Feature extends _Feature implements HasDisplayName {

	public static final String PARENT_NAME_PATH = _Feature.PARENT_PROPERTY+"."+_Feature.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getName());
	}

	@Override
	public void setParent(Feature parent) {
		if(parent != null) {
			Set<String> loopFeatureNames = new LinkedHashSet<String>();
			loopFeatureNames.add(this.getName());
			Feature current = parent;
			while(current != null) {
				String currentName = current.getName();
				if(loopFeatureNames.contains(currentName)) {
					List<String> loopNames = new ArrayList<String>(loopFeatureNames);
					loopNames.add(currentName);
					throw new FeatureException(FeatureException.Code.PARENT_RELATIONSHIP_LOOP, loopNames);
				} else {
					loopFeatureNames.add(currentName);
					current = current.getParent();
				}
			}
		}
		super.setParent(parent);
	}

	public Set<Type> getMetatagTypes() {
		return getFeatureMetatags().stream().map(m -> m.getType()).collect(Collectors.toSet());
	}

	// "Next ancestor" is defined as the first distinct ancestor we encounter while traversing the tree, which is not informational
	public Feature getNextAncestor() {
		Feature parent = getParent();
		if(parent == null) {
			return null;
		} else if(!parent.getMetatagTypes().contains(FeatureMetatag.Type.INFORMATIONAL)) {
			return parent;
		} else {
			return parent.getNextAncestor();
		}
	}

	public int getDepthInTree() {
		Feature parent = getParent();
		if(parent == null) {
			return 0;
		} else {
			return 1+parent.getDepthInTree();
		}
		
	}
	
	public void validate(CommandContext cmdContext) {
	}

	
	public boolean hasOwnCodonNumbering() {
		return getMetatagTypes().contains(FeatureMetatag.Type.OWN_CODON_NUMBERING);
	}

	public boolean isInformational() {
		return getMetatagTypes().contains(FeatureMetatag.Type.INFORMATIONAL);
	}

	public boolean codesAminoAcids() {
		return getMetatagTypes().contains(FeatureMetatag.Type.CODES_AMINO_ACIDS);
	}

	
	public Integer getDisplayOrder() {
		Optional<FeatureMetatag> displayOrderMetatag = getMetatag(FeatureMetatag.Type.DISPLAY_ORDER);
		if(displayOrderMetatag.isPresent()) {
			return Integer.parseInt(displayOrderMetatag.get().getValue());
		} else {
			return null;
		}
	}

	public Optional<FeatureMetatag> getMetatag(Type metatagType) {
		return getFeatureMetatags().stream().filter(mt -> mt.getName().equals(metatagType.name())).findFirst();
	}
	
	public boolean isDescendentOf(Feature ancestorFeature) {
		Feature parent = getParent();
		if(parent == null) {
			return false;
		}
		if(parent.getName().equals(ancestorFeature.getName())) {
			return true;
		}
		return parent.isDescendentOf(ancestorFeature);
	}

	public List<Feature> getDescendents() {
		List<Feature> descendents = new ArrayList<Feature>();
		List<Feature> children = getChildren();
		for(Feature childFeature: children) {
			descendents.add(childFeature);
			descendents.addAll(childFeature.getDescendents());
		}
		return descendents;
	}

	// includes this feature
	public List<Feature> getAncestors() {
		List<Feature> ancestors = new ArrayList<Feature>();
		Feature current = this;
		while(current != null) {
			ancestors.add(0, current);
			current = current.getParent();
		}
		return ancestors;
	}

	public List<Integer> getDisplayOrderKeyList() {
		return getAncestors().stream()
			.map(ancFeat -> { Integer ord = ancFeat.getDisplayOrder(); return ord == null ? Integer.MAX_VALUE : ord;})
			.collect(Collectors.toList());
	}
	
	public static int compareDisplayOrderKeyLists(List<Integer> list1, List<Integer> list2) {
		if(list1.isEmpty()) {
			return -1;
		}
		if(list2.isEmpty()) {
			return 1;
		}
		Integer list1Head = list1.get(0);
		Integer list2Head = list2.get(0);
		if(list1Head < list2Head) {
			return -1;
		}
		if(list2Head < list1Head) {
			return 1;
		}
		return compareDisplayOrderKeyLists(list1.subList(1, list1.size()), list2.subList(1, list2.size()));
	}

	public void checkCodesAminoAcids() {
		if(!codesAminoAcids()) {
			throw new FeatureException(FeatureException.Code.FEATURE_DOES_NOT_CODE_AMINO_ACIDS, getName());
		}
		
	}

	public CodonLabeler getCodonLabelerModule(CommandContext cmdContext) {
		FeatureMetatag codonLabelerModuleName = getMetatag(Type.CODON_LABELER_MODULE).orElse(null);
		if(codonLabelerModuleName != null) {
			String labelerModuleName = codonLabelerModuleName.getValue();
			return Module.resolveModulePlugin(cmdContext, CodonLabeler.class, labelerModuleName);
		}
		return null;
	}
	
}
