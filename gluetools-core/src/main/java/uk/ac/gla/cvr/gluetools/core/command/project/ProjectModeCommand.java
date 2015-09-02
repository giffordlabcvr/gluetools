package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
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
	public abstract static class ModuleNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				return CommandUtils.runListCommand(cmdContext, Module.class, new SelectQuery(Module.class)).
						getColumnValues(Module.NAME_PROPERTY);
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}

	@SuppressWarnings("rawtypes")
	public abstract static class AlignmentNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				ObjectContext objContext = cmdContext.getObjectContext();
				List<Alignment> alignments = GlueDataObject.query(objContext, Alignment.class, new SelectQuery(Alignment.class));
				return alignments.stream().map(Alignment::getName).collect(Collectors.toList());
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}

	@SuppressWarnings("rawtypes")
	public abstract static class FeatureNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				ObjectContext objContext = cmdContext.getObjectContext();
				List<Feature> feature = GlueDataObject.query(objContext, Feature.class, new SelectQuery(Feature.class));
				return feature.stream().map(Feature::getName).collect(Collectors.toList());
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}

	
	@SuppressWarnings("rawtypes")
	public abstract static class RefSeqNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				return CommandUtils.runListCommand(cmdContext, ReferenceSequence.class, new SelectQuery(ReferenceSequence.class)).
						getColumnValues(ReferenceSequence.NAME_PROPERTY);
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}

}
