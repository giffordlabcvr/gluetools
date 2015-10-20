package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

public abstract class ProjectModeCommand<R extends CommandResult> extends Command<R> {

	
	protected ProjectMode getProjectMode(CommandContext cmdContext) {
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
	public abstract static class FeatureNameCompleter extends AdvancedCmdCompleter {
		public FeatureNameCompleter() {
			super();
			registerDataObjectNameLookup("featureName", Feature.class, Feature.NAME_PROPERTY);
		}
	}

	
	@SuppressWarnings("rawtypes")
	public abstract static class RefSeqNameCompleter extends AdvancedCmdCompleter {
		public RefSeqNameCompleter() {
			super();
			registerDataObjectNameLookup("refSeqName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
		}
	}

}
