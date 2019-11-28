package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.FeatureProvider;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.GbFeatureSpecification;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider.SourceInfoProvider;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider.StaticSourceInfoProvider;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.structuredCommentProvider.StructuredCommentProvider;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public abstract class BaseGbSubmissionGeneratorCommand<C, SR, R extends CommandResult> extends ModulePluginCommand<R, GbSubmisisonGenerator> {

	public final static String WHERE_CLAUSE = "whereClause";
	public final static String ALL_SEQUENCES = "allSequences";
	public final static String TEMPLATE_FILE = "templateFile";
	public final static String DATA_DIR = "dataDir";
	public final static String SPECIFIC_SEQUENCE = "specificSequence";
	public final static String SOURCE_NAME = "sourceName";
	public final static String SEQUENCE_ID = "sequenceID";
	
	private Expression whereClause;
	private Boolean allSequences;
	private Boolean specificSequence;
	private String templateFile;
	private String dataDir;
	private String sourceName;
	private String sequenceID;


	private Boolean generateGbf = false;
	private Boolean validate = false;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.allSequences = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, false)).orElse(false);
		this.specificSequence = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, SPECIFIC_SEQUENCE, false)).orElse(false);
		this.sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, false);
		this.sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, false);
		if(! (
				(this.whereClause != null && !this.allSequences && !this.specificSequence) || 
				(this.whereClause == null && this.allSequences && !this.specificSequence) ||
				(this.whereClause == null && !this.allSequences && this.specificSequence)
			)) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Exactly one of <whereClause>, --allSequences or --specificSequence must be specified");
		}
		if(this.specificSequence) {
			if(this.sourceName == null || this.sequenceID == null) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --specificSequence is used then <sourceName> and <sequenceID> must be specified");
			}
		} else {
			if(this.sourceName != null || this.sequenceID != null) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <sourceName> and <sequenceID> parameters may only be used with --specificSequence");
			}
		}
		this.templateFile = PluginUtils.configureStringProperty(configElem, TEMPLATE_FILE, true);
		this.dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
	}
	

	
	@Override
	protected final R execute(CommandContext cmdContext, GbSubmisisonGenerator gbSubmisisonGenerator) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		byte[] templateBytes = consoleCmdContext.loadBytes(templateFile);

		StructuredCommentProvider structuredCommentProvider = gbSubmisisonGenerator.getStructuredCommentProvider();
		byte[] structuredCommentBytes = null;
		if(structuredCommentProvider != null) {
			Map<String, String> structuredComments = structuredCommentProvider.generateStructuredComments();
			if(!structuredComments.isEmpty()) {
				String lineBreakChars = LineFeedStyle.forOS().getLineBreakChars();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintWriter printWriter = new PrintWriter(baos);
				printWriter.write("StructuredCommentPrefix\t##Assembly-Data-START##");
				printWriter.write(lineBreakChars);
				structuredComments.forEach((k, v) -> {
					printWriter.write(k);
					printWriter.write("\t");
					printWriter.write(v);
					printWriter.write(lineBreakChars);
				});
				printWriter.flush();
				structuredCommentBytes = baos.toByteArray();
			}
			
		}
		
		
		File dataDirFile = null;
		if(dataDir != null) {
			dataDirFile = consoleCmdContext.fileStringToFile(dataDir);
			dataDirFile.mkdirs();
			if(!dataDirFile.exists()) {
				throw new Tbl2AsnException(Tbl2AsnException.Code.TBL2ASN_FILE_EXCEPTION, 
						"Unable to create data directory "+dataDirFile.getAbsolutePath());

			}
		}
		
		C context = initContext(consoleCmdContext);
					
		SelectQuery selectQuery;
		if(this.allSequences) {
			selectQuery = new SelectQuery(Sequence.class);
		} else if(this.whereClause != null) {
			selectQuery = new SelectQuery(Sequence.class, this.whereClause);
		} else {
			// specific sequence
			selectQuery = new SelectQuery(Sequence.class, 
					ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, this.sourceName)
					.andExp(ExpressionFactory.matchExp(Sequence.SEQUENCE_ID_PROPERTY, this.sequenceID)));
		}
		
		int totalNumSeqs = GlueDataObject.count(cmdContext, selectQuery);
		if(this.specificSequence && totalNumSeqs == 0) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Sequence "+this.sourceName+"/"+this.sequenceID+" not found");
		}
		
		int batchSize = 250;
		int processed = 0;
		int offset = 0;

		LinkedHashSet<String> generatedIDs = new LinkedHashSet<String>();

		List<SR> intermediateResults = new ArrayList<SR>();

		while(processed < totalNumSeqs) {
			selectQuery.setFetchLimit(batchSize);
			selectQuery.setPageSize(batchSize);
			selectQuery.setFetchOffset(offset);
			GlueLogger.getGlueLogger().finest("Retrieving sequences");
			List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);

			List<Tbl2AsnInput> inputs = new ArrayList<Tbl2AsnInput>();
			
			List<SourceInfoProvider> sourceInfoProviders = gbSubmisisonGenerator.getSourceInfoProviders();
			
			if(!gbSubmisisonGenerator.getSuppressGlueNote()) {
				StaticSourceInfoProvider glueNoteProvider = new StaticSourceInfoProvider();
				glueNoteProvider.setSourceModifier("note");		
				String glueEngineVersion = cmdContext.getGluetoolsEngine().getGluecoreProperties().getProperty("version", null);
				glueNoteProvider.setValue("submission file generated by GLUE v"+glueEngineVersion+" (http://tools.glue.cvr.ac.uk)");
				sourceInfoProviders.add(glueNoteProvider);
			}
			
			List<FeatureProvider> featureProviders = gbSubmisisonGenerator.getFeatureProviders();
			List<String> sourceColumnHeaders = new ArrayList<String>();
			sourceColumnHeaders.add("SeqID");
			sourceInfoProviders.forEach(sip -> sourceColumnHeaders.add(sip.getSourceModifier()));
			
			sequences.forEach(seq -> {
				String id = gbSubmisisonGenerator.generateId(seq);
				if(generatedIDs.contains(id)) {
					throw new Tbl2AsnException(Tbl2AsnException.Code.TBL2ASN_DATA_EXCEPTION, 
							"Duplicate ID string '"+id+"' was generated for different sequences");
				}
				generatedIDs.add(id);
				Map<String, String> sourceInfoMap = new LinkedHashMap<String, String>();
				sourceInfoMap.put("SeqID", id);
				for(SourceInfoProvider sourceInfoProvider: sourceInfoProviders) {
					String sourceInfo = sourceInfoProvider.provideSourceInfo(seq);
					String sourceModifier = sourceInfoProvider.getSourceModifier();
					if(sourceModifier.equals("note")) { // append notes
						if(sourceInfoMap.containsKey("note")) {
							sourceInfo = sourceInfoMap.get("note")+"; "+sourceInfo;
						}
					}
					sourceInfoMap.put(sourceModifier, sourceInfo);
				}
				List<GbFeatureSpecification> featureSpecs = new ArrayList<GbFeatureSpecification>();
				
				for(FeatureProvider featureProvider: featureProviders) {
					GbFeatureSpecification featureSpec = featureProvider.provideFeature(cmdContext, seq);
					if(featureSpec != null) {
						featureSpecs.add(featureSpec);
					}
				}
				
				inputs.add(new Tbl2AsnInput(seq.getSource().getName(), seq.getSequenceID(), id, 
					FastaUtils.ntStringToSequence(seq.getSequenceObject().getNucleotides(cmdContext)), 
					sourceInfoMap, featureSpecs));
			});

			List<Tbl2AsnResult> batchResults = gbSubmisisonGenerator.getTbl2AsnRunner().
					runTbl2Asn(consoleCmdContext, sourceColumnHeaders, inputs, templateBytes, structuredCommentBytes, gbSubmisisonGenerator.getAssemblyGapSpecifier(),
							generateGbf, validate, dataDirFile);
			
			
			batchResults.forEach(tbl2AsnResult -> {
				intermediateResults.add(intermediateResult(context, tbl2AsnResult));
			});
			
			
			offset += batchSize;
			processed += sequences.size();
			GlueLogger.getGlueLogger().finest("Processed "+processed+" of "+totalNumSeqs+" sequences");
			cmdContext.newObjectContext();
		}
		return finalResult(intermediateResults);
	}

	public static class GbSubmissionGeneratorCompleter extends AdvancedCmdCompleter {
		public GbSubmissionGeneratorCompleter() {
			super();
			registerPathLookup("templateFile", false);
			registerPathLookup("dataDir", true);
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
			registerVariableInstantiator("sequenceID", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return AdvancedCmdCompleter.listNames(cmdContext, prefix, Sequence.class, Sequence.SEQUENCE_ID_PROPERTY, 
							ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, bindings.get("sourceName")));
				}
			});
		}
	}
	
	protected abstract C initContext(ConsoleCommandContext consoleCmdContext);
	
	protected abstract SR intermediateResult(C context, Tbl2AsnResult tbl2AsnResult);
	
	protected abstract R finalResult(List<SR> intermediateResults);

	protected void setGenerateGbf(Boolean generateGbf) {
		this.generateGbf = generateGbf;
	}

	protected void setValidate(Boolean validate) {
		this.validate = validate;
	}
}
