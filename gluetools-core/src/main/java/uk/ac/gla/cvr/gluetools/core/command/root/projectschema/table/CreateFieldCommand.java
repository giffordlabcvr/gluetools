package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create", "field"}, 
	docoptUsages={"<fieldName> <type> [<maxLength>]"},
	description="Create a new field in the table",
	metaTags={CmdMeta.updatesDatabase},
	furtherHelp="The field name must be a valid database identifier, e.g. my_field_1") 
public class CreateFieldCommand extends TableModeCommand<CreateResult> {

	public static final String MAX_LENGTH = "maxLength";
	public static final String TYPE = "type";
	public static final String FIELD_NAME = "fieldName";

	private String fieldName;
	private FieldType type;
	private Integer maxLength;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureIdentifierProperty(configElem, FIELD_NAME, true);
		type = PluginUtils.configureEnumProperty(FieldType.class, configElem, TYPE, true);
		maxLength = PluginUtils.configureIntProperty(configElem, MAX_LENGTH, false);
		if(type == FieldType.VARCHAR && maxLength == null) {
			maxLength = 50;
		}
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		Field field = GlueDataObject.create(cmdContext, Field.class, Field.pkMap(getProjectName(), getTableName(), fieldName), false);
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(getProjectName()));
		field.setProject(project);
		field.setType(type.name());
		field.setTable(getTableName());
		field.setMaxLength(maxLength);
		ModelBuilder.addTableColumnToModel(cmdContext.getGluetoolsEngine(), project, field);
		cmdContext.commit();
		return new CreateResult(Field.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("type", FieldType.class);
		}
	}
	
	
}
