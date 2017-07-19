package uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting;

import java.util.Arrays;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.FastaSequenceObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.utils.VersionUtils;
import uk.ac.gla.cvr.gluetools.utils.VersionUtilsException;
import uk.ac.gla.cvr.gluetools.utils.VersionUtilsException.Code;

/**
 * Options for project setting commands
 *
 */
public enum ProjectSettingOption {

	IGNORE_NT_SEQUENCE_HYPHENS("ignore-nt-sequence-hyphens", "If \"true\", any hyphens in the nucleotide section of submitted or stored sequence files will be ignored", "false", new String[]{"true", "false"}),
	INTERPRET_FASTA_QUESTIONMARK_AS_N("interpret-fasta-questionmark-as-n", "If \"true\", any question mark the nucleotide section of submitted or stored sequence files will be interpreted as an N", "true", new String[]{"true", "false"}),
	TRANSLATE_BEYOND_POSSIBLE_STOP("translate-beyond-possible-stop", "If \"true\" translation to proteins will continue beyond a possible but not definite stop codon such as NNN", "false", new String[]{"true", "false"}),
	TRANSLATE_BEYOND_DEFINITE_STOP("translate-beyond-definite-stop", "If \"true\" translation to proteins will continue beyond a definite stop codon", "false", new String[]{"true", "false"}),
	INFER_FEATURE_DISPLAY_ORDER("infer-feature-display-order", "If \"true\", feature display order will be inferred from the order in which the features were created.", "false", new String[]{"true", "false"}),
	MIN_ENGINE_VERSION("min-engine-version", "Minimum GLUE engine version required to build the project", null, null) {
		@Override
		public void onSet(CommandContext cmdContext, String oldVersion, String projectMinVersion) {
			if(oldVersion != null) {
				throw new VersionUtilsException(Code.PROJECT_MIN_VERSION_ALREADY_SET, oldVersion);
			}
			if(projectMinVersion == null) {
				return;
			}
			VersionUtils.parseVersionString(projectMinVersion);
			VersionUtils.checkMinVersion(cmdContext, projectMinVersion);
			super.onSet(cmdContext, oldVersion, projectMinVersion);
		}
	},
	MAX_ENGINE_VERSION("max-engine-version", "Maximum GLUE engine version required to build the project", null, null) {
		@Override
		public void onSet(CommandContext cmdContext, String oldVersion, String projectMaxVersion) {
			if(oldVersion != null) {
				throw new VersionUtilsException(Code.PROJECT_MAX_VERSION_ALREADY_SET, oldVersion);
			}
			if(projectMaxVersion == null) {
				return;
			}
			VersionUtils.parseVersionString(projectMaxVersion);
			VersionUtils.checkMaxVersion(cmdContext, projectMaxVersion);
			super.onSet(cmdContext, oldVersion, projectMaxVersion);
		}
	},
	EXPORTED_FASTA_EXTENSION("exported-fasta-extension", "The extension format given to exported sequences in FASTA format", FastaSequenceObject.FASTA_DEFAULT_EXTENSION, 
			FastaSequenceObject.FASTA_ACCEPTED_EXTENSIONS),
	ALIGNMENT_PHYLOGENY_FORMAT("alignment-phylogeny-format", "The format used to store phylogenies against Alignment nodes", 
			PhyloFormat.GLUE_JSON.name(), 
			Arrays.asList(PhyloFormat.values()).stream().map(pf -> pf.name()).collect(Collectors.toList()).toArray(new String[]{})),
	PROJECT_VERSION("project-version", "Version number for this project, typically used to version the core project", null, null),
	EXTENSION_VERSION("extension-version", "Version number for the project extension", null, null),
	EXTENSION_DESCRIPTION("extension-description", "Describes the project extension if there is one", null, null),
	EXTENSION_BUILD_DATE("extension-build-date", "Date of the extension project build", null, null),
	EXTENSION_BUILD_ID("extension-build-id", "ID for the extension project build", null, null);
	
	private final String name;
	private final String description;
	private final String defaultValue;
	private final String[] allowedValues;
	
	private ProjectSettingOption(String name, String description, String defaultValue, String[] allowedValues) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.allowedValues = allowedValues;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String[] getAllowedValues() {
		return allowedValues;
	}
	
	public void onSet(CommandContext cmdContext, String oldValue, String newValue) {
		
	}

	
}
