package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
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
		return GlueDataObject.lookup(cmdContext.getObjectContext(), Alignment.class, 
				Alignment.pkMap(getAlignmentName()));
	}
	
	protected static abstract class AlignmentModeCompleter extends CommandCompleter {
		protected Alignment getAlignment(ConsoleCommandContext cmdContext) {
			AlignmentMode almtMode = (AlignmentMode) cmdContext.peekCommandMode();
			Alignment almt = GlueDataObject.lookup(cmdContext.getObjectContext(), 
					Alignment.class, Alignment.pkMap(almtMode.getAlignmentName()));
			return almt;
		}

		protected List<String> getMemberSources(ConsoleCommandContext cmdContext) {
			Alignment almt = getAlignment(cmdContext);
			return almt.getMembers().stream()
					.map(memb -> memb.getSequence().getSource().getName())
					.collect(Collectors.toList());
		}

		protected List<String> getMemberSequenceIDs(String sourceName, Alignment almt) {
			return almt.getMembers().stream()
					.filter(memb -> memb.getSequence().getSource().getName().equals(sourceName))
					.map(memb -> memb.getSequence().getSequenceID())
					.collect(Collectors.toList());
		}
		

	}
	
	
}
