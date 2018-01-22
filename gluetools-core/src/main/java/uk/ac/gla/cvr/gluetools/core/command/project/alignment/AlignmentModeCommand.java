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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class AlignmentModeCommand<R extends CommandResult> extends Command<R> {

	public static final String ALIGNMENT_NAME = "alignmentName";


	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
	}

	protected String getAlignmentName() {
		return alignmentName;
	}

	protected static AlignmentMode getAlignmentMode(CommandContext cmdContext) {
		return (AlignmentMode) cmdContext.peekCommandMode();
	}

	protected Alignment lookupAlignment(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Alignment.class, 
				Alignment.pkMap(getAlignmentName()));
	}
	
	protected List<AlignmentMember> lookupMembers(CommandContext cmdContext, Optional<Expression> whereClause, Boolean allMembers,
			Optional<String> sourceName, Optional<String> sequenceID) {
				Alignment alignment = lookupAlignment(cmdContext);
				List<AlignmentMember> selectedMembers;
				if(whereClause.isPresent()) {
					Expression whereClauseExp = whereClause.get();
					whereClauseExp = whereClauseExp.andExp(ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName()));
					selectedMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, whereClauseExp));
				} else {
					if(allMembers) {
						List<AlignmentMember> members = alignment.getMembers();
						selectedMembers = new ArrayList<AlignmentMember>(members);
					} else {
						AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
								AlignmentMember.pkMap(getAlignmentName(), sourceName.get(), sequenceID.get()), true);
						if(almtMember != null) {
							selectedMembers = Arrays.asList(almtMember);
						} else {
							selectedMembers = Collections.emptyList();
						}
					}
				}
				return selectedMembers;
			}

	
	protected static boolean isReferenceOfSomeChild(Alignment parentAlmt, AlignmentMember almtMember) {
		List<ReferenceSequence> referenceSequences = almtMember.getSequence().getReferenceSequences();
		for(ReferenceSequence referenceSequence: referenceSequences) {
			List<Alignment> refSeqAlmts = referenceSequence.getAlignmentsWhereRefSequence();
			for(Alignment refSeqAlmt: refSeqAlmts) {
				Alignment parent = refSeqAlmt.getParent();
				if(parent != null && parent.getName().equals(parentAlmt.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	
	protected static abstract class AlignmentMemberCompleter extends AdvancedCmdCompleter {
		public AlignmentMemberCompleter() {
			super();
			registerVariableInstantiator("sourceName", 
					new QualifiedDataObjectNameInstantiator(AlignmentMember.class, AlignmentMember.SOURCE_NAME_PATH) {
						@SuppressWarnings("rawtypes")
						@Override
						protected void qualifyResults(CommandMode cmdMode,
								Map<String, Object> bindings,
								Map<String, Object> qualifierValues) {
							qualifierValues.put(AlignmentMember.ALIGNMENT_NAME_PATH, ((AlignmentMode) cmdMode).getAlignmentName());
						}
			});
			registerVariableInstantiator("sequenceID", 
					new QualifiedDataObjectNameInstantiator(AlignmentMember.class, AlignmentMember.SEQUENCE_ID_PATH) {
						@SuppressWarnings("rawtypes")
						@Override
						protected void qualifyResults(CommandMode cmdMode,
								Map<String, Object> bindings,
								Map<String, Object> qualifierValues) {
							qualifierValues.put(AlignmentMember.ALIGNMENT_NAME_PATH, ((AlignmentMode) cmdMode).getAlignmentName());
							qualifierValues.put(AlignmentMember.SOURCE_NAME_PATH, bindings.get("sourceName"));
						}
			});
		}

	}
	
	
	
	public static abstract class FeatureOfAncConstrainingRefCompleter extends AdvancedCmdCompleter {
		public FeatureOfAncConstrainingRefCompleter() {
			super();
			registerVariableInstantiator("acRefName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					InsideAlignmentMode insideAlignmentMode = (InsideAlignmentMode) cmdContext.peekCommandMode();
					String almtName = insideAlignmentMode.getAlignmentName();
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
					return alignment.getAncConstrainingRefs().stream()
							.map(ancCR -> new CompletionSuggestion(ancCR.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("acRefName");
					ReferenceSequence referenceSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), true);
					if(referenceSequence != null) {
						return referenceSequence.getFeatureLocations().stream()
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
		}
	}


	
	
}
