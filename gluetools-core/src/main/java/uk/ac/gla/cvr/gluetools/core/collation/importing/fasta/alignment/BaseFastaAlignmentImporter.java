package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentAddMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;
import freemarker.core.ParseException;
import freemarker.template.Template;

public abstract class BaseFastaAlignmentImporter<I extends BaseFastaAlignmentImporter<I>> extends ModulePlugin<I> {

	// Boolean. If true, existing alignment members are updated. 
	// If false, no member matching the implicit where clause may exist and if one does, an exception is thrown.
	// Either way, a new member is created if necessary.
	public static final String UPDATE_EXISTING_MEMBERS = "updateExistingMembers";
	
	// Boolean. If true, an existing alignment is updated. 
	// If false, no alignment with the specified name may exist and if one does, an exception is thrown.
	// Either way, a new alignment is created if necessary.
	public static final String UPDATE_EXISTING_ALIGNMENT = "updateExistingAlignment";

	// regex extractor / formatter which transforms the incoming alignment IDs
	// into a where-clause to look up the relevant alignment member.
	// if absent, it is expected to exactly match the sequenceID.
	public static final String ID_CLAUSE_EXTRACTOR_FORMATTER = "idClauseExtractorFormatter";
	// Boolean. If true, ignore alignment rows where the ID does not match the regex of the above extractor / formatter.
	// If false, throw an exception in this case.
	// (default: false)
	public static final String IGNORE_REGEX_MATCH_FAILURES = "ignoreRegexMatchFailures";
	// Boolean. If true, ignore alignment rows no where no sequence could be found.
	// If false, throw an exception in this case.
	// (default: false)
	public static final String IGNORE_MISSING_SEQUENCES = "ignoreMissingSequences";

	
	
	
	
	private RegexExtractorFormatter idClauseExtractorFormatter = null;
	
	private Boolean ignoreRegexMatchFailures;
	private Boolean ignoreMissingSequences;
	private Boolean updateExistingMembers;
	private Boolean updateExistingAlignment;
	
	public BaseFastaAlignmentImporter() {
		super();
		addSimplePropertyName(IGNORE_REGEX_MATCH_FAILURES);
		addSimplePropertyName(IGNORE_MISSING_SEQUENCES);
		addSimplePropertyName(UPDATE_EXISTING_MEMBERS);
		addSimplePropertyName(UPDATE_EXISTING_ALIGNMENT); 
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		ignoreRegexMatchFailures = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, IGNORE_REGEX_MATCH_FAILURES, false))
				.orElse(false);
		ignoreMissingSequences = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, IGNORE_MISSING_SEQUENCES, false))
				.orElse(false);
		updateExistingMembers = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, UPDATE_EXISTING_MEMBERS, false))
				.orElse(false);
		updateExistingAlignment = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, UPDATE_EXISTING_ALIGNMENT, false))
				.orElse(false);
		
		Element extractorElem = PluginUtils.findConfigElement(configElem, ID_CLAUSE_EXTRACTOR_FORMATTER);
		if(extractorElem != null) {
			idClauseExtractorFormatter = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, extractorElem);
		} else {
			idClauseExtractorFormatter = new RegexExtractorFormatter();
		}
		// not sure why this is necessary, surely empty patterns behaviour should work.
		if(idClauseExtractorFormatter.getMatchPatterns().isEmpty()) {
			idClauseExtractorFormatter.setMatchPatterns(Arrays.asList(Pattern.compile("(.*)")));
		}
		if(idClauseExtractorFormatter.getOutputTemplate() == null) {
			Template defaultTemplate;
			try {
				defaultTemplate = PluginUtils.templateFromString("sequenceID = '${g1}'", pluginConfigContext.getFreemarkerConfiguration());
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			idClauseExtractorFormatter.setOutputTemplate(defaultTemplate);
		}
	}
	
	

	protected AlignmentMember ensureAlignmentMember(ConsoleCommandContext cmdContext, Alignment alignment, Sequence foundSequence) {
		if(updateExistingMembers) {
			Map<String, String> pkMap = AlignmentMember.pkMap(alignment.getName(), 
					foundSequence.getSource().getName(), foundSequence.getSequenceID());
			AlignmentMember existing = GlueDataObject.lookup(cmdContext, AlignmentMember.class, pkMap, true);
			if(existing != null) {
				return existing;
			}
		} 
		return AlignmentAddMemberCommand.addMember(cmdContext, alignment, foundSequence);
	}


	protected Alignment initAlignment(CommandContext cmdContext, String alignmentName) {
		Alignment alignment = GlueDataObject.create(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), updateExistingAlignment);

		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence != null) {
			throw new FastaAlignmentImporterException(Code.ALIGNMENT_IS_CONSTRAINED, alignmentName, refSequence.getName());
		}
		return alignment;
	}


	protected Sequence findSequence(CommandContext cmdContext, String fastaID, String sourceName) {
		String whereClauseString = idClauseExtractorFormatter.matchAndConvert(fastaID);
		if(whereClauseString == null) {
			if(ignoreRegexMatchFailures) {
				return null;
			}
			throw new FastaAlignmentImporterException(Code.NO_FASTA_ID_REGEX_MATCH, fastaID);
		}
		Expression whereClauseExp = null;
		try {
			whereClauseExp = CayenneUtils.expressionFromString(whereClauseString);
		} catch(ExpressionException ee) {
			throw new FastaAlignmentImporterException(Code.INVALID_WHERE_CLAUSE, fastaID, whereClauseString);
		}
		if(sourceName != null) {
			whereClauseExp = ExpressionFactory
					.matchExp(Sequence.SOURCE_NAME_PATH, sourceName)
					.andExp(whereClauseExp);
		}
		List<Sequence> foundSequences = GlueDataObject.query(cmdContext, Sequence.class, new SelectQuery(Sequence.class, whereClauseExp));
		if(foundSequences.isEmpty()) {
			if(ignoreMissingSequences) {
				log("No sequences found matching "+whereClauseExp);
				return null;
			}
			throw new FastaAlignmentImporterException(Code.NO_SEQUENCE_FOUND, fastaID, whereClauseString);
		}
		if(foundSequences.size() > 1) {
			throw new FastaAlignmentImporterException(Code.MULTIPLE_SEQUENCES_FOUND, fastaID, whereClauseString);
		}
		Sequence foundSequence = foundSequences.get(0);
		return foundSequence;
	}


	protected static class FastaAlignmentImporterResult extends TableResult {
		public FastaAlignmentImporterResult(List<Map<String, Object>> rowData) {
			super("fastaAlignmentImporterResult", Arrays.asList("fastaID", "sourceName", "sequenceID", "numSegmentsAdded", "almtRowCoverage", "correctCalls"), rowData);
		}
	}

}
