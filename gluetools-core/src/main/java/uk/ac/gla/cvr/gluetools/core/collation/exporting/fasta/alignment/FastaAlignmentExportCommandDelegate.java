package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class FastaAlignmentExportCommandDelegate {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String PREVIEW = "preview";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String FILE_NAME = "fileName";
	
	private String fileName;
	private String alignmentName;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private String acRefName;
	private String featureName;
	private Boolean recursive;
	private Boolean preview;
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem, boolean featureRequired) {
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, featureRequired);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, featureRequired);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if(acRefName != null && featureName == null || acRefName == null && featureName != null) {
			usageError2();
		}
		if(fileName == null && !preview || fileName != null && preview) {
			usageError3();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}

	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <acRefName> and <featureName> must be specified or neither");
	}

	private void usageError3() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or <preview> must be specified, but not both");
	}

	
	
	public String getFileName() {
		return fileName;
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
		return acRefName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public Boolean getRecursive() {
		return recursive;
	}

	public Boolean getPreview() {
		return preview;
	}



	public static class ExportCompleter extends AdvancedCmdCompleter {
		public ExportCompleter() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("acRefName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String alignmentName = (String) bindings.get("alignmentName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), true);
					if(alignment != null) {
						return(alignment.getAncConstrainingRefs()
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
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String acRefName = (String) bindings.get("acRefName");
					ReferenceSequence acRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(acRefName), true);
					if(acRef != null) {
						return(acRef.getFeatureLocations()
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

	
}