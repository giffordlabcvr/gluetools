package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginGroup;

public class ModulePluginGroup extends PluginGroup {

	public static final ModulePluginGroup 
		OTHER = new ModulePluginGroup("other", "Other module types", 100);

	public ModulePluginGroup(String id, String description, int orderingKey) {
		super(id, description, orderingKey);
	}

}
