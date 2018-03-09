/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ReturnToProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class VariationModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<VariationModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(VariationModeCommandFactory.class, VariationModeCommandFactory::new);

	private VariationModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		
		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		setCmdGroup(CommandGroup.VALIDATION);
		registerCommandClass(VariationValidateCommand.class);

		setCmdGroup(new CommandGroup("var-metatags", "Commands for managing variation metatags", 50, false));
		registerCommandClass(VariationSetMetatagCommand.class);
		registerCommandClass(VariationUnsetMetatagCommand.class);
		registerCommandClass(VariationListMetatagCommand.class);

		
		setCmdGroup(new CommandGroup("var-almt-notes", "Commands for managing variation-alignment notes", 51, false));
		registerCommandClass(VariationCreateAlmtNoteCommand.class);
		registerCommandClass(VariationDeleteAlmtNoteCommand.class);
		registerCommandClass(VariationListAlmtNoteCommand.class);
		
		setCmdGroup(CommandGroup.RENDERING);
		registerCommandClass(RenderObjectCommand.class);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(VariationAlmtNoteCommand.class);
		registerCommandClass(ExitCommand.class);
		registerCommandClass(ReturnToProjectModeCommand.class);


		setCmdGroup(null);
		registerCommandClass(VariationGenerateGlueConfigCommand.class);

	}
	

}
