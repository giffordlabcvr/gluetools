package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public class PropertyCommandDelegate {

	public static final String PROPERTY = "property";
	public static final String FIELD_NAME = "fieldName";
	public static final String FIELD_VALUE = "fieldValue";
	public static final String LINK_NAME = "linkName";
	public static final String TARGET_ID = "targetId";
	public static final String NO_COMMIT = "noCommit";
	
	private String property;
	private String fieldName;
	private String fieldValue;
	private String linkName;
	private String targetId;
	private Boolean noCommit;
	
	public void configureSetField(PluginConfigContext pluginConfigContext, Element configElem) {
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		fieldValue = PluginUtils.configureStringProperty(configElem, FIELD_VALUE, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureUnsetField(PluginConfigContext pluginConfigContext, Element configElem) {
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureSetLink(PluginConfigContext pluginConfigContext, Element configElem) {
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
		targetId = PluginUtils.configureStringProperty(configElem, TARGET_ID, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureUnsetLink(PluginConfigContext pluginConfigContext, Element configElem) {
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}
	
	public void configureShowProperty(PluginConfigContext pluginConfigContext, Element configElem) {
		property = PluginUtils.configureStringProperty(configElem, PROPERTY, true);
	}

	public void configureListProperty(PluginConfigContext pluginConfigContext, Element configElem) {
	}

	
	public UpdateResult executeSetField(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String tableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		FieldType fieldType = project.getModifiableFieldType(tableName, fieldName);
		Object newValue = fieldType.getFieldTranslator().valueFromString(fieldValue);
		return executeSetField(cmdContext, project, tableName, configurableObject, 
				fieldName, newValue, noCommit);
	}

	public static UpdateResult executeSetField(CommandContext cmdContext, Project project,
			String tableName, GlueDataObject configurableObject, 
			String fieldName, Object newValue, boolean noCommit) {
		Class<? extends GlueDataObject> dataObjectClass = project.getDataObjectClass(tableName);
		project.checkModifiableFieldNames(tableName, Collections.singletonList(fieldName));
		Object oldValue = configurableObject.readProperty(fieldName);
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

	
	public UpdateResult executeUnsetField(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String tableName = configurableObjectMode.getTableName();
		Class<? extends GlueDataObject> dataObjectClass = project.getDataObjectClass(tableName);
		project.checkModifiableFieldNames(tableName, Collections.singletonList(fieldName));
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
		String tableName = configurableObjectMode.getTableName();
		project.checkListableProperties(tableName, Collections.singletonList(property));
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
		String tableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		return new ListPropertyResult(project.getListableProperties(tableName), configurableObject);
	}

	
	public UpdateResult executeSetLink(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String tableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		Link link = project.getLink(tableName, linkName);
		return executeSetLink(cmdContext, project, tableName, configurableObject, 
				link, targetId, noCommit);
	}

	public static UpdateResult executeSetLink(CommandContext cmdContext, Project project,
			String tableName, GlueDataObject configurableObject, 
			Link link, String targetId, boolean noCommit) {
		// TODO: check multiplicity is ONE_TO_ONE
		Class<? extends GlueDataObject> dataObjectClass = project.getDataObjectClass(tableName);
		
		// TODO: check update is required.
		
		Class<? extends GlueDataObject> destObjectClass = project.getDataObjectClass(link.getDestTableName());
		CustomTableObject target = (CustomTableObject) GlueDataObject.lookup(cmdContext, destObjectClass, CustomTableObject.pkMap(targetId));
		configurableObject.writeProperty(link.getSrcLinkName(), target);
		target.writeProperty(link.getDestLinkName(), configurableObject);
		
		if(!noCommit) {
			cmdContext.commit();
		}
		return new UpdateResult(dataObjectClass, 1);
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
					String tableName = configurableObjectMode.getTableName();
					List<String> listableFieldNames = project.getModifiableFieldNames(tableName);
					return listableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
				}
			});
		}
	}

	public static class LinkNameCompleter extends AdvancedCmdCompleter {
		public LinkNameCompleter() {
			super();
			registerVariableInstantiator("linkName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {

					ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
					Project project = configurableObjectMode.getProject();
					String tableName = configurableObjectMode.getTableName();
					List<Link> linksForWhichSource = project.getLinksForWhichSource(tableName);
					return linksForWhichSource.stream().map(n -> new CompletionSuggestion(n.getSrcLinkName(), true)).collect(Collectors.toList());
				}
			});
		}
	}

	
	public static class LinkNameAndIdCompleter extends LinkNameCompleter {
		public LinkNameAndIdCompleter() {
			super();
			registerVariableInstantiator("targetId", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String srcLinkName = (String) bindings.get("linkName");
					if(srcLinkName != null) {
						ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
						Project project = configurableObjectMode.getProject();
						String tableName = configurableObjectMode.getTableName();
						Link link = project.getLink(tableName, srcLinkName);
						if(link != null) {
							String destTableName = link.getDestTableName();
							return listNames(cmdContext, prefix, project.getDataObjectClass(destTableName), CustomTableObject.ID_PROPERTY);
						}
					}
					return new ArrayList<CompletionSuggestion>();
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
					String tableName = configurableObjectMode.getTableName();
					List<String> listableFieldNames = project.getListableProperties(tableName);
					return listableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
				}
			});
		}
	}




}
