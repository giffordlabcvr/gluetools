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
package uk.ac.gla.cvr.gluetools.core.command.configurableobject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.LinkUpdateContext.UpdateType;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ListPropertyResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.PropertyValueResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.Keyword;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ModePathElement;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldException;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.LinkException;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.LinkException.Code;
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
	public static final String PROPERTY_NAME = "propertyName";
	
	
	private String property;
	private String fieldName;
	private String fieldValue;
	private String linkName;
	private String targetPath;
	private Boolean noCommit;
	public List<String> propertyNames;
	
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

	public void configureAddLinkTarget(PluginConfigContext pluginConfigContext, Element configElem) {
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
		targetPath = PluginUtils.configureStringProperty(configElem, TARGET_PATH, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureRemoveLinkTarget(PluginConfigContext pluginConfigContext, Element configElem) {
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
		targetPath = PluginUtils.configureStringProperty(configElem, TARGET_PATH, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureUnsetLinkTarget(PluginConfigContext pluginConfigContext, Element configElem) {
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureClearLinkTarget(PluginConfigContext pluginConfigContext, Element configElem) {
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}

	public void configureShowProperty(PluginConfigContext pluginConfigContext, Element configElem) {
		property = PluginUtils.configureStringProperty(configElem, PROPERTY, true);
	}

	public void configureListProperty(PluginConfigContext pluginConfigContext, Element configElem) {
	}

	public void configureListLinkTarget(PluginConfigContext pluginConfigContext, Element configElem) {
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
		
		propertyNames = PluginUtils.configureStringsProperty(configElem, PROPERTY_NAME);
	}

	
	public UpdateResult executeSetField(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String tableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		FieldType fieldType = project.getModifiableFieldType(tableName, fieldName);
		if(fieldType == null) {
			throw new FieldException(FieldException.Code.NO_SUCH_FIELD, tableName, fieldName);
		}
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
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		return executeUnsetField(cmdContext, project,
				tableName, configurableObject, fieldName, noCommit);
	}

	public static UpdateResult executeUnsetField(CommandContext cmdContext, Project project,
			String tableName, GlueDataObject configurableObject, String fieldName, Boolean noCommit) {
		Class<? extends GlueDataObject> dataObjectClass = project.getDataObjectClass(tableName);
		project.checkModifiableFieldNames(tableName, Collections.singletonList(fieldName));
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
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		Object value = ListResult.generateResultValue(cmdContext, configurableObject, property);
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
		return new ListPropertyResult(cmdContext, project.getListableProperties(tableName), configurableObject);
	}

	@SuppressWarnings("unchecked")
	public <D extends GlueDataObject> ListResult executeListLinkTarget(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String tableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);

		Link link = resolveLink(project, tableName, linkName);
		String otherTableName;
		if(linkName.equals(link.getSrcLinkName()) && tableName.equals(link.getSrcTableName())) {
			otherTableName = link.getDestTableName();
			if(!link.isToMany())
				throw new LinkException(Code.LINK_MULTIPLICITY_ERROR, tableName, linkName, 
						"Cannot use 'list link-target' on source object of link with multiplicity "+link.getMultiplicity()+
						": use 'show property' instead");
		} else {
			otherTableName = link.getSrcTableName();
			if(!link.isFromMany())
				throw new LinkException(Code.LINK_MULTIPLICITY_ERROR, tableName, linkName, 
						"Cannot use 'list link-target' on destination object of link with multiplicity "+link.getMultiplicity()+
						": use 'show property' instead");
		}
		Class<D> theClass = (Class<D>) project.getDataObjectClass(otherTableName);
		Collection<D> targets = (Collection<D>) configurableObject.readNestedProperty(linkName);
		
		if(propertyNames == null || propertyNames.isEmpty()) {
			if(CustomTableObject.class.isAssignableFrom(theClass)) {
				return new ListResult(cmdContext, theClass, new LinkedList<D>(targets), Arrays.asList(CustomTableObject.ID_PROPERTY));
			} else {
				return new ListResult(cmdContext, theClass, new LinkedList<D>(targets));
			}
		} else {
			return new ListResult(cmdContext, theClass, new LinkedList<D>(targets), propertyNames);
		}
		
		
		
	}

	private Link resolveLink(Project project, String tableName, String linkName) {
		Link link = project.getLink(tableName, linkName);
		if(link == null) {
			throw new LinkException(Code.NO_SUCH_LINK, tableName, linkName);
		}
		return link;
	}

	
	public UpdateResult executeSetLinkTarget(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String modeTableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		LinkUpdateContext linkUpdateContext = new LinkUpdateContext(project, modeTableName, linkName);
		return executeLinkTargetUpdate(cmdContext, project, configurableObject, noCommit, targetPath, 
				linkUpdateContext, UpdateType.SET);
	}

	public UpdateResult executeUnsetLinkTarget(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String modeTableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		LinkUpdateContext linkUpdateContext = new LinkUpdateContext(project, modeTableName, linkName);
		return executeLinkTargetUpdate(cmdContext, project, configurableObject, noCommit, targetPath, 
				linkUpdateContext, UpdateType.UNSET);
	}

	public UpdateResult executeAddLinkTarget(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String modeTableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		LinkUpdateContext linkUpdateContext = new LinkUpdateContext(project, modeTableName, linkName);
		return executeLinkTargetUpdate(cmdContext, project, configurableObject, noCommit, targetPath, 
				linkUpdateContext, UpdateType.ADD);
	}

	public UpdateResult executeRemoveLinkTarget(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String modeTableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		LinkUpdateContext linkUpdateContext = new LinkUpdateContext(project, modeTableName, linkName);
		return executeLinkTargetUpdate(cmdContext, project, configurableObject, noCommit, targetPath, 
				linkUpdateContext, UpdateType.REMOVE);
	}

	public UpdateResult executeClearLinkTarget(CommandContext cmdContext) {
		ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
		Project project = configurableObjectMode.getProject();
		String modeTableName = configurableObjectMode.getTableName();
		GlueDataObject configurableObject = configurableObjectMode.getConfigurableObject(cmdContext);
		LinkUpdateContext linkUpdateContext = new LinkUpdateContext(project, modeTableName, linkName);
		return executeLinkTargetUpdate(cmdContext, project, configurableObject, noCommit, targetPath, 
				linkUpdateContext, UpdateType.CLEAR);
	}

	
	
	public static UpdateResult executeLinkTargetUpdate(CommandContext cmdContext, Project project,
			GlueDataObject thisObject, boolean noCommit, String targetPath,
			LinkUpdateContext linkUpdateContext, UpdateType updateType) {
		
		updateType.checkMultiplicity(linkUpdateContext);
		GlueDataObject otherObject = null;
		if(targetPath != null) {
			Class<? extends GlueDataObject> otherObjectClass = project.getDataObjectClass(linkUpdateContext.getOtherTableName());
			Map<String,String> otherPkMap = project.targetPathToPkMap(linkUpdateContext.getOtherTableName(), targetPath);
			otherObject = GlueDataObject.lookup(cmdContext, otherObjectClass, otherPkMap);
		}

		Class<? extends GlueDataObject> thisObjectClass = project.getDataObjectClass(linkUpdateContext.getThisTableName());
		if(updateType.updateRequired(linkUpdateContext, thisObject, otherObject)) {
			updateType.execute(linkUpdateContext, thisObject, otherObject);
			
			if(!noCommit) {
				cmdContext.commit();
			}
			return new UpdateResult(thisObjectClass, 1);
		} else {
			return new UpdateResult(thisObjectClass, 0);
		}
	}

	public static class ModifiableFieldNameCompleter extends AdvancedCmdCompleter {
		public ModifiableFieldNameCompleter() {
			super();
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
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

	public static abstract class LinkNameCompleter extends AdvancedCmdCompleter {
		private boolean requireToOne;
		private boolean requireToMany;
		public LinkNameCompleter(boolean requireToOne, boolean requireToMany) {
			super();
			this.requireToOne = requireToOne;
			this.requireToMany = requireToMany;
			registerVariableInstantiator("linkName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {

					ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
					Project project = configurableObjectMode.getProject();
					String tableName = configurableObjectMode.getTableName();
					List<Link> linksForWhichSource = project.getLinksForWhichSource(tableName);
					List<CompletionSuggestion> suggestions = linksForWhichSource
							.stream()
							.filter(l -> l.isToOne() || !LinkNameCompleter.this.requireToOne)
							.filter(l -> l.isToMany() || !LinkNameCompleter.this.requireToMany)
							.map(n -> new CompletionSuggestion(n.getSrcLinkName(), true))
							.collect(Collectors.toList());
					List<Link> linksForWhichDestination = project.getLinksForWhichDestination(tableName);
					suggestions.addAll(linksForWhichDestination.stream()
							.filter(l -> l.isFromOne() || !LinkNameCompleter.this.requireToOne)
							.filter(l -> l.isFromMany() || !LinkNameCompleter.this.requireToMany)
							.map(n -> new CompletionSuggestion(n.getDestLinkName(), true))
							.collect(Collectors.toList()));
					return suggestions;
				}
			});
		}
	}

	public static class ToOneLinkNameCompleter extends LinkNameCompleter {
		public ToOneLinkNameCompleter() {
			super(true, false);
		}
	}

	public static class ToManyLinkNameCompleter extends LinkNameCompleter {
		public ToManyLinkNameCompleter() {
			super(false, false);
		}
	}

	public abstract static class LinkNameAndTargetPathCompleter extends LinkNameCompleter {
		public LinkNameAndTargetPathCompleter(boolean requireToOne, boolean requireToMany) {
			super(requireToOne, requireToMany);
			registerVariableInstantiator("targetPath", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
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
	
	public static class ToOneLinkNameAndTargetPathCompleter extends LinkNameAndTargetPathCompleter {
		public ToOneLinkNameAndTargetPathCompleter() {
			super(true, false);
		}
	}

	public static class ToManyLinkNameAndTargetPathCompleter extends LinkNameAndTargetPathCompleter {
		public ToManyLinkNameAndTargetPathCompleter() {
			super(false, true);
		}
	}

	private static List<CompletionSuggestion> completeTargetPath(ConsoleCommandContext cmdContext, Project project, String tableName, String prefix) {
		String lastSuggestion = prefix;
		while(true) {
			List<CompletionSuggestion> intermediateSuggs = completeTargetPathAux(cmdContext, project, tableName, lastSuggestion);
			if(intermediateSuggs.size() != 1) {
				return intermediateSuggs;
			}
			CompletionSuggestion onlySuggestion = intermediateSuggs.get(0);
			if(onlySuggestion.isCompleted()) {
				return intermediateSuggs;
			}
			String nextSuggestion = onlySuggestion.getSuggestedWord();
			if(lastSuggestion != null && nextSuggestion.equals(lastSuggestion)) {
				return intermediateSuggs;
			}
			lastSuggestion = nextSuggestion;
		}
	}

	@SuppressWarnings("rawtypes")
	private static List<CompletionSuggestion> completeTargetPathAux(ConsoleCommandContext cmdContext, Project project, String tableName, String prefix) {
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
					commandFactory.getCommandWordSuggestions(cmdContext, lookupBasis, words.get(words.size()-1), true, false, false);
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
				public List<CompletionSuggestion> instantiate(
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
