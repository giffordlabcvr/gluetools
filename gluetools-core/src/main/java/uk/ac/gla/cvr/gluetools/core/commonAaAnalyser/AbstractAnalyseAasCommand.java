package uk.ac.gla.cvr.gluetools.core.commonAaAnalyser;

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
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class AbstractAnalyseAasCommand<R extends CommandResult> extends ModulePluginCommand<R, CommonAaAnalyser> implements ProvidedProjectModeCommand {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";

	private String alignmentName;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private String acRefName;
	private String featureName;
	private Boolean recursive;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}

	protected String getAlignmentName() {
		return alignmentName;
	}

	protected Optional<Expression> getWhereClause() {
		return whereClause;
	}

	protected Boolean getAllMembers() {
		return allMembers;
	}

	protected String getAcRefName() {
		return acRefName;
	}

	protected String getFeatureName() {
		return featureName;
	}

	protected Boolean getRecursive() {
		return recursive;
	}

	public static class AnalyseAasCompleter extends AdvancedCmdCompleter {
		public AnalyseAasCompleter() {
			super();
			registerVariableInstantiator("alignmentName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					List<Alignment> almts = GlueDataObject.query(cmdContext, Alignment.class, 
							new SelectQuery(Alignment.class, ExpressionFactory.noMatchExp(Alignment.REF_SEQUENCE_PROPERTY, null)));
					return almts.stream()
							.map(almt -> new CompletionSuggestion(almt.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("acRefName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String almtName = (String) bindings.get("alignmentName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
					if(alignment != null) {
						return alignment.getAncConstrainingRefs().stream()
							.map(ancCR -> new CompletionSuggestion(ancCR.getName(), true))
							.collect(Collectors.toList());
					}
					return null;
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
								.filter(fLoc -> fLoc.getFeature().codesAminoAcids())
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
		}
		
	}

}
