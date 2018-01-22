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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class AlignmentModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<AlignmentModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(AlignmentModeCommandFactory.class, AlignmentModeCommandFactory::new);

	private AlignmentModeCommandFactory() {
	}	
	
	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		setCmdGroup(new CommandGroup("members", "Commands for managing alignment members", 49, false));
		registerCommandClass(AlignmentAddMemberCommand.class);
		registerCommandClass(AlignmentRemoveMemberCommand.class);
		registerCommandClass(AlignmentListMemberCommand.class);
		registerCommandClass(AlignmentWebListMemberCommand.class);
		registerCommandClass(AlignmentCountMemberCommand.class);

		setCmdGroup(new CommandGroup("members", "Commands for managing alignment member segments", 49, false));
		registerCommandClass(AlignmentDeriveSegmentsCommand.class);

		setCmdGroup(new CommandGroup("properties", "Commands for querying properties of the alignment", 49, false));
		registerCommandClass(AlignmentShowReferenceSequenceCommand.class);


		setCmdGroup(new CommandGroup("parent", "Commands for managing alignment parent-child relationships", 50, false));
		registerCommandClass(AlignmentSetParentCommand.class);
		registerCommandClass(AlignmentUnsetParentCommand.class);
		registerCommandClass(AlignmentShowParentCommand.class);
		registerCommandClass(AlignmentListChildrenCommand.class);
		registerCommandClass(AlignmentListDescendentCommand.class);
		registerCommandClass(AlignmentShowAncestorsCommand.class);
		registerCommandClass(AlignmentDescendentTreeCommand.class);
		registerCommandClass(AlignmentExtractChildCommand.class);
		registerCommandClass(AlignmentDemoteMemberCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		setCmdGroup(new CommandGroup("analysis", "Commands performing basic analysis based on the stored homologies", 51, false));
		registerCommandClass(AlignmentAminoAcidFrequencyCommand.class);
		registerCommandClass(AlignmentAminoAcidStringsCommand.class);
		registerCommandClass(AlignmentVariationFrequencyCommand.class);
		registerCommandClass(AlignmentVariationMemberScanCommand.class);
		registerCommandClass(AlignmentShowStatisticsCommand.class);
		registerCommandClass(AlignmentShowMemberFeatureCoverageCommand.class);
		registerCommandClass(AlignmentShowFeaturePresenceCommand.class);
		registerCommandClass(AlignmentScoreCoverageCommand.class);
		
		setCmdGroup(CommandGroup.RENDERING);
		registerCommandClass(RenderObjectCommand.class);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(MemberCommand.class);
		registerCommandClass(ExitCommand.class);
	}
	

}
