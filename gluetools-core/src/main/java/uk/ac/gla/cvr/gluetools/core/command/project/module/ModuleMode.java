package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

@CommandModeClass(commandFactoryClass = ModuleModeCommandFactory.class)
public class ModuleMode extends CommandMode {

	
	private String moduleName;
	
	// dynamic command factory
	public ModuleMode(CommandContext cmdContext, String moduleName) {
		super("module/"+moduleName+"/", new ModuleModeCommandFactory(cmdContext, moduleName));
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
