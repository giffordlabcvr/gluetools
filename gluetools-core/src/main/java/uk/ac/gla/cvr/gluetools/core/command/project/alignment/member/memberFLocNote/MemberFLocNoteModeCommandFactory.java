package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.memberFLocNote;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class MemberFLocNoteModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<MemberFLocNoteModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(MemberFLocNoteModeCommandFactory.class, MemberFLocNoteModeCommandFactory::new);

	private MemberFLocNoteModeCommandFactory() {
	}	
	
	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);
		
		registerCommandClass(ExitCommand.class);
	}
	

}
