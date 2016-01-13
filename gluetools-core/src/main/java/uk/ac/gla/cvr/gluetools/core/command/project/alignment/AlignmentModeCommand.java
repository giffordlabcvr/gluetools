package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
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
			List<Alignment> refSeqAlmts = referenceSequence.getAlignments();
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
	
	
}
