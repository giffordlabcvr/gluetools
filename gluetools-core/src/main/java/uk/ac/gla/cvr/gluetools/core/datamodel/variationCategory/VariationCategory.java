package uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._VariationCategory;

@GlueDataClass(defaultListedProperties = {_VariationCategory.NAME_PROPERTY, VariationCategory.PARENT_NAME_PATH, _VariationCategory.DESCRIPTION_PROPERTY, 
		_VariationCategory.NOTIFIABILITY_PROPERTY})
public class VariationCategory extends _VariationCategory {

	public static final String PARENT_NAME_PATH = _VariationCategory.PARENT_PROPERTY+"."+_VariationCategory.NAME_PROPERTY;
	public static final String INHERITED_NOTIFIABILITY = "inheritedNotifiability";

	public enum NotifiabilityLevel {
		NOTIFIABLE,
		NOT_NOTIFIABLE
	}
	
	public NotifiabilityLevel getNotifiabilityLevel() {
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
	public Map<String, String> pkMap() {
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

	@Override
	public void generateGlueConfig(int indent, StringBuffer glueConfigBuf, GlueConfigContext glueConfigContext) {
		indent(glueConfigBuf, indent).append("set notifiability "+getNotifiability()).append("\n");
		VariationCategory parent = getParent();
		if(parent != null) { indent(glueConfigBuf, indent).append("set parent "+parent.getName()).append("\n"); }
	}
	
	
	public static List<VariationCategory> getTopologicallySortedVcats(CommandContext cmdContext) {
		List<VariationCategory> allVcats = GlueDataObject.query(cmdContext, VariationCategory.class, new SelectQuery(VariationCategory.class));
		List<VariationCategory> startVcats = new ArrayList<VariationCategory>();
		for(VariationCategory vcat: allVcats) {
			if(vcat.getParent() == null) {
				startVcats.add(vcat);
			}
		}
		LinkedList<VariationCategory> ordered = new LinkedList<VariationCategory>();
		LinkedList<VariationCategory> queue = new LinkedList<VariationCategory>();
		queue.addAll(startVcats);
		while(!queue.isEmpty()) {
			VariationCategory vcat = queue.removeFirst();
			ordered.add(vcat);
			queue.addAll(0, vcat.getChildren());
		}
		return ordered;
	}

	public NotifiabilityLevel getInheritedNotifiability() {
		List<VariationCategory> ancestors = getAncestors();
		NotifiabilityLevel current = getNotifiabilityLevel();
		for(int i = 1; i < ancestors.size(); i++) {
			NotifiabilityLevel ancestorNotifiability = ancestors.get(i).getNotifiabilityLevel();
			if(ancestorNotifiability.ordinal() < current.ordinal()) {
				current = ancestorNotifiability;
			}
		}
		return current;
	}
	
}
