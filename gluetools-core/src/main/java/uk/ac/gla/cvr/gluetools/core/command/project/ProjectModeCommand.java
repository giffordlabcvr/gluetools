package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;

public abstract class ProjectModeCommand<R extends CommandResult> extends Command<R> {

	
	protected static ProjectMode getProjectMode(CommandContext cmdContext) {
		ProjectMode projectMode = (ProjectMode) cmdContext.peekCommandMode();
		return projectMode;
	}

	
	@SuppressWarnings("rawtypes")
	public abstract static class ModuleNameCompleter extends AdvancedCmdCompleter {
		public ModuleNameCompleter() {
			super();
			registerDataObjectNameLookup("moduleName", Module.class, Module.NAME_PROPERTY);
		}
	}

	@SuppressWarnings("rawtypes")
	public abstract static class AlignmentNameCompleter extends AdvancedCmdCompleter {
		public AlignmentNameCompleter() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
		}
	}

	@SuppressWarnings("rawtypes")
	public abstract static class GroupNameCompleter extends AdvancedCmdCompleter {
		public GroupNameCompleter() {
			super();
			registerDataObjectNameLookup("groupName", SequenceGroup.class, SequenceGroup.NAME_PROPERTY);
		}
	}

	
	@SuppressWarnings("rawtypes")
	public abstract static class FeatureNameCompleter extends AdvancedCmdCompleter {
		public FeatureNameCompleter() {
			super();
			registerDataObjectNameLookup("featureName", Feature.class, Feature.NAME_PROPERTY);
		}
	}

	@SuppressWarnings("rawtypes")
	public abstract static class VariationCategoryNameCompleter extends AdvancedCmdCompleter {
		public VariationCategoryNameCompleter() {
			super();
			registerDataObjectNameLookup("vcatName", VariationCategory.class, VariationCategory.NAME_PROPERTY);
		}
	}

	
	@SuppressWarnings("rawtypes")
	public abstract static class RefSeqNameCompleter extends AdvancedCmdCompleter {
		public RefSeqNameCompleter() {
			super();
			registerDataObjectNameLookup("refSeqName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
		}
	}

	public abstract static class SequenceFieldNameCompleter extends AdvancedCmdCompleter {
		public SequenceFieldNameCompleter() {
			super();
			registerVariableInstantiator("fieldName", new SequenceFieldInstantiator());
		}
	}
	
	public static class SequenceFieldInstantiator extends AdvancedCmdCompleter.VariableInstantiator {
		@Override
		@SuppressWarnings("rawtypes")
		protected List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return 
					getProjectMode(cmdContext).getProject().getCustomFieldNames(ConfigurableTable.SEQUENCE)
					.stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
		}
	}

	
	
}
