package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;



import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;



import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

// TODO -- make source optional, and then list all in a project

@PluginClass(elemName="list-sequences")
@CommandClass(description="List sequences in the project or in a given source", 
	docoptUsages={"[-s <sourceName>] [<fieldName> ...]"},
	docoptOptions={"-s <sourceName>, --sourceName <sourceName>  Specify a particular source"}) 
public class ListSequencesCommand extends ProjectModeCommand {

	private String sourceName;
	private List<String> fieldNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		fieldNames = PluginUtils.configureStringsProperty(configElem, "fieldName", 0, null);
		if(fieldNames.isEmpty()) {
			if(sourceName == null) {
				fieldNames = null; // default fields
			} else {
				fieldNames.add(_Sequence.SEQUENCE_ID_PROPERTY);
			}
		}
	}
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp;
		if(sourceName != null) {
			exp = ExpressionFactory.matchExp(_Sequence.SOURCE_PROPERTY, sourceName);
		} else {
			exp = ExpressionFactory.matchExp(_Sequence.SOURCE_PROPERTY+"."+
					_Source.PROJECT_PROPERTY, getProjectName());
		}
		List<String> validFieldNamesList = super.getValidSequenceFieldNames(cmdContext);
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		fieldNames.forEach(f-> {
			if(!validFieldNames.contains(f)) {
				throw new SequenceException(Code.INVALID_FIELD, f, validFieldNamesList);
			}
		});
		return CommandUtils.runListCommand(cmdContext, Sequence.class, new SelectQuery(Sequence.class, exp), 
				fieldNames);
	}

}
