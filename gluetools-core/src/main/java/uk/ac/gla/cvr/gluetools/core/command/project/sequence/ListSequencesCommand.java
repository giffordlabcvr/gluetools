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
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"list", "sequences"},
	docoptUsages={"[-s <sourceName>] [-q <sequenceID>] [<fieldName> ...]"},
	docoptOptions={
		"-s <sourceName>, --sourceName <sourceName>  Specify a particular source",
		"-q <sequenceID>, --sequenceID <sequenceID>  Specify a particular sequenceID"},
	description="List sequences, based on source or sequence ID"
) 
public class ListSequencesCommand extends ProjectModeCommand {

	private String sourceName;
	private String sequenceID;
	private List<String> fieldNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", false);
		fieldNames = PluginUtils.configureStringsProperty(configElem, "fieldName");
		if(fieldNames.isEmpty()) {
			fieldNames = null; // default fields
		}
	}
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.expTrue();
		if(sourceName != null) {
			exp = exp.andExp(ExpressionFactory.matchExp(_Sequence.SOURCE_PROPERTY, sourceName));
		}
		if(sequenceID != null) {
			exp = exp.andExp(ExpressionFactory.matchExp(_Sequence.SEQUENCE_ID_PROPERTY, sequenceID));
		}
		SelectQuery selectQuery = new SelectQuery(Sequence.class, exp);
		List<String> validFieldNamesList = super.getValidSequenceFieldNames(cmdContext);
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validFieldNames.contains(f)) {
					throw new SequenceException(Code.INVALID_FIELD, f, validFieldNamesList);
				}
			});
		}
		return CommandUtils.runListCommand(cmdContext, Sequence.class, selectQuery, 
				fieldNames);
	}

}
