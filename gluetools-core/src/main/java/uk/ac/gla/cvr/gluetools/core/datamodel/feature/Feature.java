package uk.ac.gla.cvr.gluetools.core.datamodel.feature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag.Type;

@GlueDataClass(defaultListColumns = {_Feature.NAME_PROPERTY, Feature.PARENT_NAME_PATH, _Feature.DESCRIPTION_PROPERTY})
public class Feature extends _Feature {

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
	protected Map<String, String> pkMap() {
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

	public Feature getOrfAncestor() {
		if(getMetatagTypes().contains(FeatureMetatag.Type.OPEN_READING_FRAME)) {
			return this;
		}
		Feature parent = getParent();
		if(parent == null) {
			return null;
		} else {
			return parent.getOrfAncestor();
		}
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
		if(hasOwnCodonNumbering()) {
			Feature orfAncestor = getOrfAncestor();
			if(orfAncestor == null) {
				throw new FeatureException(FeatureException.Code.FEATURE_WITH_OWN_CODON_NUMBERING_NOT_IN_ORF, 
						getName());
			}
		}
	}

	
	public boolean hasOwnCodonNumbering() {
		return getMetatagTypes().contains(FeatureMetatag.Type.OWN_CODON_NUMBERING);
	}

	public boolean isOpenReadingFrame() {
		return getMetatagTypes().contains(FeatureMetatag.Type.OPEN_READING_FRAME);
	}

	

	
	
}
