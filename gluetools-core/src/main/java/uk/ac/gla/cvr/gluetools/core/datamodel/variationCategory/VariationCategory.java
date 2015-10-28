package uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._VariationCategory;

@GlueDataClass(defaultListColumns = {_VariationCategory.NAME_PROPERTY, VariationCategory.PARENT_NAME_PATH, _VariationCategory.DESCRIPTION_PROPERTY})
public class VariationCategory extends _VariationCategory {

	public static final String PARENT_NAME_PATH = _VariationCategory.PARENT_PROPERTY+"."+_VariationCategory.NAME_PROPERTY;

	public enum NotifiabilityLevel {
		NOTIFIABLE,
		NOT_NOTIFIABLE
	}
	
	private NotifiabilityLevel notifiabilityLevel;

	public NotifiabilityLevel getNotifiabilityLevel() {
		if(notifiabilityLevel == null) {
			notifiabilityLevel = buildNotifiabilityLevel();
		}
		return notifiabilityLevel;
	}
	
	private NotifiabilityLevel buildNotifiabilityLevel() {
		return NotifiabilityLevel.valueOf(getNotifiability());
	}

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
	public void setParent(VariationCategory parent) {
		if(parent != null) {
			Set<String> loopVariationCategoryNames = new LinkedHashSet<String>();
			loopVariationCategoryNames.add(this.getName());
			VariationCategory current = parent;
			while(current != null) {
				String currentName = current.getName();
				if(loopVariationCategoryNames.contains(currentName)) {
					List<String> loopNames = new ArrayList<String>(loopVariationCategoryNames);
					loopNames.add(currentName);
					throw new VariationCategoryException(VariationCategoryException.Code.PARENT_RELATIONSHIP_LOOP, loopNames);
				} else {
					loopVariationCategoryNames.add(currentName);
					current = current.getParent();
				}
			}
		}
		super.setParent(parent);
	}


	public List<VariationCategory> getDescendents() {
		List<VariationCategory> descendents = new ArrayList<VariationCategory>();
		List<VariationCategory> children = getChildren();
		for(VariationCategory childVariationCategory: children) {
			descendents.add(childVariationCategory);
			descendents.addAll(childVariationCategory.getDescendents());
		}
		return descendents;
	}

	public List<VariationCategory> getAncestors() {
		List<VariationCategory> ancestors = new ArrayList<VariationCategory>();
		VariationCategory current = this;
		do {
			ancestors.add(current);
			current = current.getParent();
		} while(current != null);
		return ancestors;
	}
	
	
}
