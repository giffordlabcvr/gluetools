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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","alignment"}, 
	docoptUsages={"<alignmentName> [-r <refSeqName> [-p <parentName>] ] "},
	docoptOptions={
		"-r <refSeqName>, --refSeqName <refSeqName>  Constraining reference sequence",
		"-p <parentName>, --parentName <parentName>  Parent alignment"},
	description="Create a new alignment, optionally constrained to a reference sequence", 
	metaTags={CmdMeta.updatesDatabase},
	furtherHelp="An alignment is container for a proposed homology between segments of certain sequences. "+
	"Alignments may be defined with a reference sequence, in which case they are constrained alignments. "+
	"Constrained alignments propose pairwise homologies between the reference and zero or more member sequences. "+
	"The reference coordinates of constrained alignment members refer to positions on the reference sequence. "+
	"Where used, the reference sequence must be specified when the alignment is created. "+
	"As long as a reference sequence constrains an alignment, the reference sequence may not be deleted."+
	"Constrained alignments may optionally also have a parent alignment defined. "+
	"Unconstrained alignments do not have a reference sequence defined. Unconstrained alignments may propose "+
	"homologies between any of their members. The reference coordinates of unconstrained alignments do not refer to locations "+
	"on any sequence: these are used as a neutral coordinate system which can flexibly accommodate any homology."
	) 
public class CreateAlignmentCommand extends ProjectModeCommand<CreateResult> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REF_SEQ_NAME = "refSeqName";
	public static final String PARENT_NAME = "parentName";
	
	private String alignmentName;
	private String refSeqName;
	private String parentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		refSeqName = PluginUtils.configureStringProperty(configElem, REF_SEQ_NAME, false);
		parentName = PluginUtils.configureStringProperty(configElem, PARENT_NAME, false);
		if(parentName != null && refSeqName == null) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, 
				"Only constrained alignments may have a parent specified.");
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		ReferenceSequence refSequence = null;
		Alignment parentAlignment = null;
		if(refSeqName != null) {
			refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
					ReferenceSequence.pkMap(refSeqName));
			if(parentName != null) {
				parentAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, 
						Alignment.pkMap(parentName));
			}
		}
		createAlignment(cmdContext, alignmentName, refSequence, parentAlignment);
		cmdContext.commit();
		return new CreateResult(Alignment.class, 1);
	}

	public static Alignment createAlignment(CommandContext cmdContext, String almtName, ReferenceSequence refSequence, Alignment parentAlignment) {
		Alignment alignment = GlueDataObject.create(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
		if(refSequence != null) {
			alignment.setRefSequence(refSequence);
			if(parentAlignment != null) {
				alignment.setParent(parentAlignment);
			}
		}
		return alignment;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("refSeqName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("parentName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					String thisAlmtName = (String) bindings.get("alignmentName");
					return listNames(cmdContext, prefix, Alignment.class, Alignment.NAME_PROPERTY, 
							ExpressionFactory.noMatchExp(Alignment.NAME_PROPERTY, thisAlmtName));
				}
			});
		}


	}
}
