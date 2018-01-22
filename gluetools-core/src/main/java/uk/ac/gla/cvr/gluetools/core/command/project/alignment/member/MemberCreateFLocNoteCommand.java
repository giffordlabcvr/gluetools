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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","member-floc-note"}, 
	docoptUsages={"<refSeqName> <featureName>"},
	description="Create a new member-featureLoc note", 
	metaTags={CmdMeta.updatesDatabase}
	) 
public class MemberCreateFLocNoteCommand extends MemberModeCommand<CreateResult> {

	public static final String REF_SEQ_NAME = "refSeqName";
	public static final String FEATURE_NAME = "featureName";
	private String refSeqName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, REF_SEQ_NAME, true);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(refSeqName, featureName));
		createFLocNote(cmdContext, almtMember, featureLoc, false);
		cmdContext.commit();
		return new CreateResult(MemberFLocNote.class, 1);
	}

	public static MemberFLocNote createFLocNote(CommandContext cmdContext, 
			AlignmentMember almtMember, FeatureLocation featureLoc, boolean allowExists) {
		MemberFLocNote memberFLocNote = GlueDataObject.create(cmdContext, MemberFLocNote.class, 
				MemberFLocNote.pkMap(
						almtMember.getAlignment().getName(), 
						almtMember.getSequence().getSource().getName(), 
						almtMember.getSequence().getSequenceID(),
						featureLoc.getReferenceSequence().getName(),
						featureLoc.getFeature().getName()), allowExists);
		memberFLocNote.setMember(almtMember);
		memberFLocNote.setFeatureLoc(featureLoc);
		return memberFLocNote;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("refSeqName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					MemberMode memberMode = (MemberMode) cmdContext.peekCommandMode();
					List<ReferenceSequence> relatedRefs = memberMode.getAlignmentMember(cmdContext).getAlignment().getRelatedRefs();
					return relatedRefs.stream().map(rr -> new CompletionSuggestion(rr.getName(), true)).collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String refSeqName = (String) bindings.get("refSeqName");
					if(refSeqName != null) {
						ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName), true);
						if(refSeq != null) {
							return refSeq.getFeatureLocations().stream().map(fl -> new CompletionSuggestion(fl.getFeature().getName(), true)).collect(Collectors.toList());
						}
					}
					return null;
				}
			});
		}
	}
}
