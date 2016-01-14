package uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.FastaSequenceObject;

/**
 * Options for project setting commands
 *
 */
public enum ProjectSettingOption {

	IGNORE_NT_SEQUENCE_HYPHENS("ignore-nt-sequence-hyphens", "If \"true\", any hyphens in the nucleotide section of submitted or stored sequence files will be ignored", "false", new String[]{"true", "false"}),
	INTERPRET_FASTA_QUESTIONMARK_AS_N("interpret-fasta-questionmark-as-n", "If \"true\", any question mark the nucleotide section of submitted or stored sequence files will be interpreted as an N", "true", new String[]{"true", "false"}),
	TRANSLATE_BEYOND_POSSIBLE_STOP("translate-beyond-possible-stop", "If \"true\" translation to proteins will continue beyond a possible but not definite stop codon such as NNN", "false", new String[]{"true", "false"}),
	TRANSLATE_BEYOND_DEFINITE_STOP("translate-beyond-definite-stop", "If \"true\" translation to proteins will continue beyond a definite stop codon", "false", new String[]{"true", "false"}),
	// Note, the following setting only affects query / member sequences, not reference sequences.
	// If we get a case where reference sequence features are incomplete or not contained within an ORF we could change this.
	TRANSLATE_ORF_DESCENDENTS_DIRECTLY("translate-orf-descendents-directly", "If \"true\", features which descend from ORFs will be translated directly, rather than deriving the translation from that of the ORF", "false", new String[]{"true", "false"}),
	INFER_FEATURE_DISPLAY_ORDER("infer-feature-display-order", "If \"true\", feature display order will be inferred from the order in which the features were created.", "false", new String[]{"true", "false"}),
	EXPORTED_FASTA_EXTENSION("exported-fasta-extension", "The extension format given to exported sequences in FASTA format", FastaSequenceObject.FASTA_DEFAULT_EXTENSION, 
			FastaSequenceObject.FASTA_ACCEPTED_EXTENSIONS);
	
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
	
}
