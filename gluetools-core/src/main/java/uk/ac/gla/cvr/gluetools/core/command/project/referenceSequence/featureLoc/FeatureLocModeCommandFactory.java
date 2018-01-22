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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class FeatureLocModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<FeatureLocModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(FeatureLocModeCommandFactory.class, FeatureLocModeCommandFactory::new);

	private FeatureLocModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		
		setCmdGroup(new CommandGroup("segments", "Commands for managing feature segments", 50, false));
		registerCommandClass(AddFeatureSegmentCommand.class);
		registerCommandClass(RemoveFeatureSegmentCommand.class);
		registerCommandClass(ListFeatureSegmentCommand.class);
		
		setCmdGroup(new CommandGroup("variations", "Commands for managing variations", 51, false));
		registerCommandClass(CreateVariationCommand.class);
		registerCommandClass(DeleteVariationCommand.class);
		registerCommandClass(FeatureLocListVariationCommand.class);
		
		setCmdGroup(new CommandGroup("aminos", "Commands for querying amino-acids and labeled codons", 52, false));
		registerCommandClass(FeatureLocListLabeledCodonsCommand.class);
		registerCommandClass(FeatureLocAminoAcidCommand.class);
		registerCommandClass(FeatureLocCountAminoAcidCommand.class);

		setCmdGroup(CommandGroup.VALIDATION);
		registerCommandClass(FeatureLocValidateCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(VariationCommand.class);
		registerCommandClass(ExitCommand.class);

		setCmdGroup(null);
		registerCommandClass(FeatureLocGenerateGlueConfigCommand.class);
}
	

}
