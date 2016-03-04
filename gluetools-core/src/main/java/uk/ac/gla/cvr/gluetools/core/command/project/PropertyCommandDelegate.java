package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public class PropertyCommandDelegate {

	public static final String PROPERTY = "property";
	public static final String FIELD_NAME = "fieldName";
	public static final String FIELD_VALUE = "fieldValue";
	public static final String NO_COMMIT = "noCommit";
	
	private String property;
	private String fieldName;
	private String fieldValue;
	private Boolean noCommit;
	
	public void configureSet(PluginConfigContext pluginConfigContext, Element configElem) {
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		fieldValue = PluginUtils.configureStringProperty(configElem, FIELD_VALUE, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureUnset(PluginConfigContext pluginConfigContext, Element configElem) {
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureShowProperty(PluginConfigContext pluginConfigContext, Element configElem) {
		property = PluginUtils.configureStringProperty(configElem, PROPERTY, true);
	}

	public void configureListProperty(PluginConfigContext pluginConfigContext, Element configElem) {
	}

	
	public UpdateResult executeSet(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		ConfigurableTable cTable = configurableObjectMode.getConfigurableTable();
		Class<? extends GlueDataObject> dataObjectClass = cTable.getDataObjectClass();
		project.checkModifiableFieldNames(cTable, Collections.singletonList(fieldName));
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		Object oldValue = configurableObject.readProperty(fieldName);
		FieldType fieldType = project.getModifiableFieldType(cTable, fieldName);
		Object newValue = fieldType.getFieldTranslator().valueFromString(fieldValue);
		if(oldValue != null && newValue != null && oldValue.equals(newValue)) {
			return new UpdateResult(dataObjectClass, 0);
		}
		if(oldValue == null && newValue == null) {
			return new UpdateResult(dataObjectClass, 0);
		}
		configurableObject.writeProperty(fieldName, newValue);
		if(!noCommit) {
			cmdContext.commit();
		}
		return new UpdateResult(dataObjectClass, 1);
	}

	
	public UpdateResult executeUnset(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		ConfigurableTable cTable = configurableObjectMode.getConfigurableTable();
		Class<? extends GlueDataObject> dataObjectClass = cTable.getDataObjectClass();
		project.checkCustomFieldNames(cTable, Collections.singletonList(fieldName));
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		Object oldValue = configurableObject.readProperty(fieldName);
		if(oldValue == null) {
			return new UpdateResult(dataObjectClass, 0);
		}
		configurableObject.writeProperty(fieldName, null);
		if(!noCommit) {
			cmdContext.commit();
		}
		return new UpdateResult(dataObjectClass, 1);
	}

	public PropertyValueResult executeShowProperty(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		ConfigurableTable cTable = configurableObjectMode.getConfigurableTable();
		project.checkListableProperties(cTable, Collections.singletonList(property));
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		Object value = configurableObject.readNestedProperty(property);
		if(value == null) {
			return new PropertyValueResult(property, null);
		} else {
			return new PropertyValueResult(property, value.toString());
		}
	}

	public ListPropertyResult executeListProperty(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		ConfigurableTable cTable = configurableObjectMode.getConfigurableTable();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		return new ListPropertyResult(project.getListableProperties(cTable), configurableObject);
	}

	
	public static class ModifiableFieldNameCompleter extends AdvancedCmdCompleter {
		public ModifiableFieldNameCompleter() {
			super();
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {

					ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
					Project project = configurableObjectMode.getProject();
					ConfigurableTable cTable = configurableObjectMode.getConfigurableTable();
					List<String> listableFieldNames = project.getModifiableFieldNames(cTable);
					return listableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
				}
			});
		}
	}

	public static class ListablePropertyCompleter extends AdvancedCmdCompleter {
		public ListablePropertyCompleter() {
			super();
			registerVariableInstantiator("property", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {

					ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
					Project project = configurableObjectMode.getProject();
					ConfigurableTable cTable = configurableObjectMode.getConfigurableTable();
					List<String> listableFieldNames = project.getListableProperties(cTable);
					return listableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
				}
			});
		}
	}




}
