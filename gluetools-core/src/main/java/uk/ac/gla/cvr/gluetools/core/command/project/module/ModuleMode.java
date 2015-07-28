package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class ModuleMode extends CommandMode {

	
	private String moduleName;
	
	public ModuleMode(CommandContext cmdContext, String moduleName) {
		super("mod-"+moduleName, new ModuleModeCommandFactory(cmdContext, moduleName));
		this.moduleName = moduleName;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(ModuleModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "moduleName", moduleName, JsonType.String);
		}
	}

	public String getModuleName() {
		return moduleName;
	}

	
}
