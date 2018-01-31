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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public class FastaAlignmentExportCommandDelegate {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String LABELLED_CODON = "labelledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";
	public static final String NT_REGION = "ntRegion";
	public static final String NT_START = "ntStart";
	public static final String NT_END = "ntEnd";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String EXCLUDE_EMPTY_ROWS = "excludeEmptyRows";
	public static final String SELECTOR_NAME = "selectorName";
	public static final String LINE_FEED_STYLE = "lineFeedStyle";

	
	private String alignmentName;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private String relRefName;
	private String featureName;
	private String lcStart;
	private String lcEnd;
	private Boolean recursive;
	private Boolean labelledCodon;
	private Boolean ntRegion;
	private Integer ntStart;
	private Integer ntEnd;
	private Boolean excludeEmptyRows;
	private String selectorName;
	private LineFeedStyle lineFeedStyle;
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem, boolean featureRequired) {
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, featureRequired);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, featureRequired);
		labelledCodon = PluginUtils.configureBooleanProperty(configElem, LABELLED_CODON, true);
		lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		ntRegion = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, NT_REGION, false)).orElse(false);
		ntStart = PluginUtils.configureIntProperty(configElem, NT_START, false);
		ntEnd = PluginUtils.configureIntProperty(configElem, NT_END, false);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		excludeEmptyRows = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_EMPTY_ROWS, false)).orElse(Boolean.FALSE);
		selectorName = PluginUtils.configureStringProperty(configElem, SELECTOR_NAME, false);
		lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if(selectorName != null && ( relRefName != null || featureName != null )) {
			usageError1a();
		}
		if(relRefName != null && featureName == null || relRefName == null && featureName != null) {
			usageError2();
		}
		if(selectorName != null && ( ntRegion || labelledCodon )) {
			usageError3a();
		}
		if(labelledCodon && (lcStart == null || lcEnd == null)) {
			usageError4();
		}
		if(ntRegion && labelledCodon) {
			usageError5();
		}
		if(ntRegion && (ntStart == null || ntEnd == null)) {
			usageError6();
		}
		
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}

	private void usageError1a() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If <selectorName> is used then <relRefName> and <featureName> may not be used");
	}
	
	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <relRefName> and <featureName> must be specified or neither");
	}

	private void usageError3a() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If <selectorModuleName> is used then nither --ntRegion or --labelledCodon may be specified");
	}

	private void usageError4() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --labelledCodon is used, both <lcStart> and <lcEnd> must be specified");
	}
 	private void usageError5() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either --ntRegion or --labelledCodon may be specified, but not both");
	}
 	private void usageError6() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --ntRegion is used, both <ntStart> and <ntEnd> must be specified");
	}

	public String getAlignmentName() {
		return alignmentName;
	}

	public Optional<Expression> getWhereClause() {
		return whereClause;
	}

	public Boolean getAllMembers() {
		return allMembers;
	}

	public String getAcRefName() {
		return relRefName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public Boolean getRecursive() {
		return recursive;
	}

	public String getLcStart() {
		return lcStart;
	}

	public String getLcEnd() {
		return lcEnd;
	}

	public Boolean getLabelledCodon() {
		return labelledCodon;
	}

	public Boolean getExcludeEmptyRows() {
		return excludeEmptyRows;
	}
	
	public Boolean getNtRegion() {
		return ntRegion;
	}

	public Integer getNtStart() {
		return ntStart;
	}

	public Integer getNtEnd() {
		return ntEnd;
	}

	public LineFeedStyle getLineFeedStyle() {
		return lineFeedStyle;
	}

	public static class ExportCompleter extends AdvancedCmdCompleter {
		public ExportCompleter() {
			super();
			registerEnumLookup("lineFeedStyle", LineFeedStyle.class);
			registerModuleNameLookup("selectorName", "alignmentColumnsSelector");
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("relRefName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String alignmentName = (String) bindings.get("alignmentName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), true);
					if(alignment != null) {
						return(alignment.getRelatedRefs()
								.stream()
								.map(ref -> new CompletionSuggestion(ref.getName(), true)))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String relRefName = (String) bindings.get("relRefName");
					ReferenceSequence relRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relRefName), true);
					if(relRef != null) {
						return(relRef.getFeatureLocations()
								.stream()
								.filter(fLoc -> filterFeatureLocation(fLoc))
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true)))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerPathLookup("fileName", false);
		}
		
		protected boolean filterFeatureLocation(FeatureLocation fLoc) {
			return true;
		}
	}

	public IAlignmentColumnsSelector getAlignmentColumnsSelector(CommandContext cmdContext) {
		if(selectorName != null) {
			return Module.resolveModulePlugin(cmdContext, AlignmentColumnsSelector.class, selectorName);
		} else if(relRefName != null && featureName != null) {
			return new SimpleAlignmentColumnsSelector(relRefName, featureName, ntStart, ntEnd, lcStart, lcEnd);
		} else {
			return null;
		}
	}
	
	
}
