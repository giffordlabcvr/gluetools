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
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class MemberModeCommand<R extends CommandResult> extends AlignmentModeCommand<R> {


	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}

	protected String getSourceName() {
		return sourceName;
	}

	protected String getSequenceID() {
		return sequenceID;
	}


	protected MemberMode getMemberMode(CommandContext cmdContext) {
		MemberMode memberMode = (MemberMode) cmdContext.peekCommandMode();
		return memberMode;
	}

	protected Sequence lookupSequence(CommandContext cmdContext) {
		Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class,
				Sequence.pkMap(getSourceName(), getSequenceID()));
		return sequence;
	}

	protected AlignmentMember lookupMember(CommandContext cmdContext) {
		AlignmentMember alignmentMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class,
				AlignmentMember.pkMap(getAlignmentName(), getSourceName(), getSequenceID()));
		return alignmentMember;
	}

}
