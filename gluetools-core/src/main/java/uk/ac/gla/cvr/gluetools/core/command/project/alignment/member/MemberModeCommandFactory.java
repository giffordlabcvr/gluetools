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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ReturnToProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class MemberModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<MemberModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(MemberModeCommandFactory.class, MemberModeCommandFactory::new);

	private MemberModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		setCmdGroup(new CommandGroup("segments", "Commands for managing aligned segments", 49, false));
		registerCommandClass(MemberAddSegmentCommand.class);
		registerCommandClass(MemberRemoveSegmentCommand.class);
		registerCommandClass(MemberListSegmentCommand.class);
		registerCommandClass(MemberTranslateSegmentCommand.class);
		registerCommandClass(MemberShowFeatureSegmentsCommand.class);
		registerCommandClass(MemberShowFeatureCoverageCommand.class);
		
		setCmdGroup(new CommandGroup("analysis", "Commands performing basic analysis based on the stored homologies", 50, false));
		registerCommandClass(MemberShowStatisticsCommand.class);
		registerCommandClass(MemberAminoAcidCommand.class);
		registerCommandClass(MemberCountAminoAcidCommand.class);
		registerCommandClass(MemberVariationScanCommand.class);

		setCmdGroup(new CommandGroup("member-floc-notes", "Commands for managing member-feature-location notes", 51, false));
		registerCommandClass(MemberCreateFLocNoteCommand.class);
		registerCommandClass(MemberDeleteFLocNoteCommand.class);
		registerCommandClass(MemberListFLocNoteCommand.class);
		
		setCmdGroup(CommandGroup.RENDERING);
		registerCommandClass(RenderObjectCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);
		
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(ExitCommand.class);
		registerCommandClass(ReturnToProjectModeCommand.class);

		registerCommandClass(MemberFLocNoteCommand.class);
	}
	

}
