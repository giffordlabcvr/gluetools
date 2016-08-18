package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.Keyword;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ModePathElement;
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
	public static final String TARGET_PATH = "targetPath";
	public static final String NO_COMMIT = "noCommit";
	
	private String property;
	private String fieldName;
	private String fieldValue;
	private String linkName;
	private String targetPath;
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

	public void configureSetLinkTarget(PluginConfigContext pluginConfigContext, Element configElem) {
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
		targetPath = PluginUtils.configureStringProperty(configElem, TARGET_PATH, true);
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

	
	public UpdateResult executeSetLinkTarget(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String modeTableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		return executeSetLinkTarget(cmdContext, project, configurableObject, modeTableName, 
				linkName, targetPath, noCommit);
	}

	public static UpdateResult executeSetLinkTarget(CommandContext cmdContext, Project project,
			GlueDataObject thisObject, String thisTableName, 
			String linkName, String targetPath, boolean noCommit) {
		boolean isSrcLink = false;
		Link link = project.getSrcTableLink(thisTableName, linkName);
		if(link != null) {
			isSrcLink = true;
		} else {
			link = project.getDestTableLink(thisTableName, linkName);
		}
		String otherTableName;
		String thisLinkName, otherLinkName;
		if(isSrcLink) {
			thisLinkName = link.getSrcLinkName();
			otherLinkName = link.getDestLinkName();
			otherTableName = link.getDestTableName();
		} else {
			thisLinkName = link.getDestLinkName();
			otherLinkName = link.getSrcLinkName();
			otherTableName = link.getSrcTableName();
		}
		
		// TODO: check multiplicity is ONE_TO_ONE
		Class<? extends GlueDataObject> otherObjectClass = project.getDataObjectClass(otherTableName);
		
		// TODO: check update is required.
		
		Map<String,String> otherPkMap = project.targetPathToPkMap(otherTableName, targetPath);
		
		GlueDataObject otherObject = GlueDataObject.lookup(cmdContext, otherObjectClass, otherPkMap);
		thisObject.writeProperty(thisLinkName, otherObject);
		otherObject.writeProperty(otherLinkName, thisObject);
		
		if(!noCommit) {
			cmdContext.commit();
		}
		Class<? extends GlueDataObject> thisObjectClass = project.getDataObjectClass(thisTableName);
		return new UpdateResult(thisObjectClass, 1);
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
					List<CompletionSuggestion> suggestions = linksForWhichSource.stream().map(n -> new CompletionSuggestion(n.getSrcLinkName(), true)).collect(Collectors.toList());
					List<Link> linksForWhichDestination = project.getLinksForWhichDestination(tableName);
					suggestions.addAll(linksForWhichDestination.stream().map(n -> new CompletionSuggestion(n.getDestLinkName(), true)).collect(Collectors.toList()));
					return suggestions;
				}
			});
		}
	}

	
	public static class LinkNameAndTargetPathCompleter extends LinkNameCompleter {
		public LinkNameAndTargetPathCompleter() {
			super();
			registerVariableInstantiator("targetPath", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String linkName = (String) bindings.get("linkName");
					if(linkName != null) {
						ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
						Project project = configurableObjectMode.getProject();
						String tableName = configurableObjectMode.getTableName();
						Link link = project.getSrcTableLink(tableName, linkName);
						if(link != null) {
							return completeTargetPath(cmdContext, project, link.getDestTableName(), prefix);
						}
						link = project.getDestTableLink(tableName, linkName);
						if(link != null) {
							return completeTargetPath(cmdContext, project, link.getSrcTableName(), prefix);
						}
					}
					return new ArrayList<CompletionSuggestion>();
				}
			});
		}
	}

	@SuppressWarnings("rawtypes")
	private static List<CompletionSuggestion> completeTargetPath(ConsoleCommandContext cmdContext, Project project, String tableName, String prefix) {
		List<CompletionSuggestion> suggestions = new ArrayList<CompletionSuggestion>();
		ModePathElement[] modePath = project.getModePathForTable(tableName);
		String[] prefixBits = prefix.split("/", -1);
		// complete keyword if possible.
		if(prefixBits.length <= modePath.length) {
			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < prefixBits.length; i++) {
				if(i > 0) {
					buf.append("/");
				}
				if(modePath[i] instanceof Keyword) {
					String keyword = ((Keyword) modePath[i]).getKeyword();
					if(i < prefixBits.length - 1) {
						if(prefixBits[i].equals(keyword)) {
							buf.append(prefixBits[i]);
						} else {
							return suggestions; // breaking the correct form
						}
					} else { // last prefix bit
						if(keyword.startsWith(prefixBits[i])) {
							buf.append(keyword);
							buf.append("/");
							suggestions.add(new CompletionSuggestion(buf.toString(), false));
							return suggestions; 
						} else {
							return suggestions; // breaking the correct form
						}
					}
				} else {
					buf.append(prefixBits[i]);
				}
			}
		} else {
			return suggestions; // prefix has too many bits
		}
		// should only get to here if prefix matches form and completion is not a keyword
		LinkedList<CommandMode<?>> modesToRestore = new LinkedList<CommandMode<?>>();
		try {
			StringBuffer buf = new StringBuffer();
			// return to project mode
			while(!(cmdContext.peekCommandMode() instanceof ProjectMode)) {
				modesToRestore.push(cmdContext.popCommandMode());
			}
			LinkedList<String> words = new LinkedList<String>(Arrays.asList(prefixBits));
			while(true) {
				int numWordsUsed;
				try {
					numWordsUsed = cmdContext.pushCommandModeReturnNumWordsUsed(words.toArray(new String[]{}));
				} catch(DataModelException dme) {
					break;
				}
				if(numWordsUsed == 0) {
					break;
				} else {
					for(int i = 0; i < numWordsUsed; i++) { 
						String word = words.removeFirst();
						if(buf.length() > 0) {
							buf.append("/");
						}
						buf.append(word);
					}
				}
			}
			if(words.isEmpty()) {
				suggestions.add(new CompletionSuggestion(buf.toString(), true));
				return suggestions;
			}
			CommandFactory commandFactory = cmdContext.peekCommandMode().getCommandFactory();
			List<String> lookupBasis = words.subList(0, words.size()-1);
			for(String word: lookupBasis) {
				if(buf.length() > 0) {
					buf.append("/");
				}
				buf.append(word);
			}
			List<CompletionSuggestion> lastCmdSuggestions = 
					commandFactory.getCommandWordSuggestions(cmdContext, lookupBasis, words.get(words.size()-1), true, false);
			for(CompletionSuggestion lastCmdSuggestion: lastCmdSuggestions) {
				String suggPrefix = buf.toString()+"/";
				String fullSuggestedWord = suggPrefix+lastCmdSuggestion.getSuggestedWord();
				boolean completed = false;
				if(fullSuggestedWord.split("/").length == modePath.length) {
					completed = true;
				} else if(lastCmdSuggestion.isCompleted()) {
					fullSuggestedWord = fullSuggestedWord+"/";
				}
				suggestions.add(new CompletionSuggestion(fullSuggestedWord, completed));
			}
			return suggestions;
		} finally {
			while(!(cmdContext.peekCommandMode() instanceof ProjectMode)) {
				cmdContext.popCommandMode();
			}
			while(!modesToRestore.isEmpty()) {
				cmdContext.pushCommandMode(modesToRestore.pop());
			}
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
