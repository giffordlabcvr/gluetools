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
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

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
					getProjectMode(cmdContext).getProject().getCustomFieldNames(ConfigurableTable.sequence.name())
					.stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
		}
	}

	
	
}
