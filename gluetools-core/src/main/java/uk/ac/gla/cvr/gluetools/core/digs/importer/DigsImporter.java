package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.config.DatabaseConfiguration;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.digs.importer.DigsImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.DigsObject;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.Extracted;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import freemarker.core.ParseException;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

@PluginClass(elemName=DigsImporter.ELEM_NAME)
public class DigsImporter extends ModulePlugin<DigsImporter> {

	public static final String ELEM_NAME = "digsImporter";
 	
	
	public static String 
		DIGS_DB_JDBC_URL_PROPERTY = "gluetools.core.digs.db.jdbc.url"; 
	public static String 
		DIGS_DB_USERNAME_PROPERTY = "gluetools.core.digs.db.username"; 
	public static String 
		DIGS_DB_PASSWORD_PROPERTY = "gluetools.core.digs.db.password"; 

	public static String 
		DIGS_DOMAIN_RESOURCE = "cayenne-digs-domain.xml";
	public static String 
		DIGS_MAP_RESOURCE = "digs-map.map.xml";
	
	
	public static String SEQUENCE_ID_TEMPLATE = "sequenceIdTemplate";
	
	private Template sequenceIdTemplate;
	
	private Map<String, ImportExtractedFieldRule> extractedFieldToRule = new LinkedHashMap<String, ImportExtractedFieldRule>();
	
	
	public DigsImporter() {
		super();
		addModulePluginCmdClass(ListExtractedCommand.class);
		addModulePluginCmdClass(ImportExtractedCommand.class);
		addModulePluginCmdClass(ListDigsDbsCommand.class);
		addModulePluginCmdClass(SynchroniseFieldsExtractedCommand.class);
		addModulePluginCmdClass(CheckFieldsExtractedCommand.class);
		addModuleDocumentCmdClass(ShowMappingExtractedCommand.class);
		addModuleDocumentCmdClass(UpdateMappingExtractedCommand.class);
		addSimplePropertyName(SEQUENCE_ID_TEMPLATE);
	}

	
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Template defaultTemplate = null;
		try {
			defaultTemplate = PluginUtils.templateFromString("${"+Extracted.BLAST_ID_PROPERTY+"}", pluginConfigContext.getFreemarkerConfiguration());
		} catch(ParseException pe) {
			throw new RuntimeException(pe);
		}
		this.sequenceIdTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, SEQUENCE_ID_TEMPLATE, false))
				.orElse(defaultTemplate);
		this.extractedFieldToRule = initRulesMap(pluginConfigContext, configElem);
		Set<String> glueSequenceFields = new LinkedHashSet<String>();
		for(ImportExtractedFieldRule importExtractedFieldRule: initRulesMap(pluginConfigContext, configElem).values()) {
			if(importExtractedFieldRule.getGlueFieldRequirement() != ImportExtractedFieldRule.GlueFieldRequirement.IGNORE) {
				String sequenceFieldToUse = importExtractedFieldRule.getSequenceFieldToUse();
				if(glueSequenceFields.contains(sequenceFieldToUse)) {
					throw new PluginConfigException(PluginConfigException.Code.CONFIG_CONSTRAINT_VIOLATION, "Multiple rules map to GLUE sequence field "+sequenceFieldToUse);
				}
				glueSequenceFields.add(sequenceFieldToUse);
			}
		}

	}




	public static Map<String, ImportExtractedFieldRule> initRulesMap(
			PluginConfigContext pluginConfigContext, Element configElem) {
		Map<String, ImportExtractedFieldRule> extractedFieldToRule = new LinkedHashMap<String, ImportExtractedFieldRule>();
		List<Element> importExtractedRuleElems = PluginUtils.findConfigElements(configElem, ImportExtractedFieldRule.EXTRACTED_FIELD_RULE);
		List<ImportExtractedFieldRule> importExtractedRules = PluginFactory.createPlugins(pluginConfigContext, 
				ImportExtractedFieldRule.class, importExtractedRuleElems);
		for(String extractedField : Extracted.ALL_PROPERTIES) {
			ImportExtractedFieldRule importExtractedFieldRule = new ImportExtractedFieldRule();
			importExtractedFieldRule.setExtractedField(extractedField);
			extractedFieldToRule.put(extractedField, importExtractedFieldRule);
		}
		for(ImportExtractedFieldRule importExtractedFieldRule: importExtractedRules) {
			extractedFieldToRule.put(importExtractedFieldRule.getExtractedField(), importExtractedFieldRule);
		}
		return extractedFieldToRule;
	}


	public List<ImportExtractedFieldRule> getImportExtractedFieldRules() {
		return new ArrayList<ImportExtractedFieldRule>(extractedFieldToRule.values());
	}


	private <C extends DigsObject> List<C> query(ObjectContext digsObjectContext, Class<C> objClass, SelectQuery query) {
		List<?> queryResult = null;
		try {
			queryResult = digsObjectContext.performQuery(query);
		} catch(CayenneRuntimeException cre) {
			Throwable cause = cre.getCause();
			Expression qualifier = query.getQualifier();
			if(qualifier != null && cause != null && cause instanceof ExpressionException) {
				throw new DigsImporterException(Code.EXPRESSION_ERROR, qualifier.toString(), cause.getMessage());
			} else {
				throw cre;
			}
		}
		return queryResult.stream().map(obj -> { 
			C dataObject = objClass.cast(obj);
			return dataObject;
		}).collect(Collectors.toList());
	}
	

	private List<Extracted> listExtracted(CommandContext cmdContext, String digsDbName, Optional<Expression> whereClause) {
		ServerRuntime digsServerRuntime = null;
		try {
			digsServerRuntime = createDigsServerRuntime(cmdContext, digsDbName);
			ObjectContext digsObjectContext = digsServerRuntime.getContext();
			SelectQuery selectQuery = null;
			if(whereClause.isPresent()) {
				selectQuery = new SelectQuery(Extracted.class, whereClause.get());
			} else {
				selectQuery = new SelectQuery(Extracted.class);
			}
			return query(digsObjectContext, Extracted.class, selectQuery);
		} finally {
			if(digsServerRuntime != null) {
				try { digsServerRuntime.shutdown(); } 
				catch(Exception e) {
					GlueLogger.getGlueLogger().warning("Exception thrown while shutting down DIGS MySQL connection: "+e.getLocalizedMessage());
				}
			}
		}
	}
	
	public CreateResult importHits(CommandContext cmdContext, String digsDbName, String sourceName, Optional<Expression> whereClause) {
		
		ProjectMode projectMode = (ProjectMode) cmdContext.popCommandMode();
		String projectName = projectMode.getProject().getName();
		// run the command in schema-project mode
		try(ModeCloser modeCloser = cmdContext.pushCommandMode("schema-project", projectName)) {
			CheckFieldsExtractedCommand.checkFields(cmdContext, this);
		} finally {
			cmdContext.pushCommandMode(projectMode);
		}
		
		List<Extracted> extracteds = listExtracted(cmdContext, digsDbName, whereClause);
		int numSequences = 0;
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName), true);
		if(source == null) {
			source = GlueDataObject.create(cmdContext, Source.class, Source.pkMap(sourceName), false);
		}
		cmdContext.commit();
		
		for(Extracted extracted: extracteds) {
			String sequenceId = runIdTemplate(sequenceIdTemplate, extracted);
			Sequence sequence = CreateSequenceCommand.createSequence(cmdContext, sourceName, sequenceId, false);
			sequence.setFormat(SequenceFormat.FASTA.name());
			byte[] originalData = FastaUtils.seqIdCompoundsPairToFasta(sequenceId, extracted.getSequence()).getBytes();
			sequence.setOriginalData(originalData);
			sequence.setSource(source);
			extractedFieldToRule.values().forEach(rule -> {
				rule.updateSequence(cmdContext, extracted, sequence, rule.getSequenceFieldToUse());
			});
			numSequences++;
		}
		cmdContext.commit();
		return new CreateResult(Sequence.class, numSequences);
	}

	
	
	private String runIdTemplate(Template template, Extracted extracted) {
		TemplateHashModel variableResolver = new TemplateHashModel() {
			@Override
			public TemplateModel get(String key) {
				Object object = extracted.readNestedProperty(key);
				return object == null ? null : new SimpleScalar(object.toString());
			}
			@Override
			public boolean isEmpty() { return false; }
		};
		StringWriter stringWriter = new StringWriter();
		try {
			template.process(variableResolver, stringWriter);
		} catch (TemplateException te) {
			throw new DigsImporterException(te, 
					DigsImporterException.Code.ID_TEMPLATE_FAILED, te.getLocalizedMessage());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		String templateResult = stringWriter.toString();
		return templateResult;
	}

	
	public static List<String> listDigsDatabases(CommandContext cmdContext) {
		
		String digsDbBaseUrl = getDigsBaseUrl(cmdContext);
		String digsDbUsername = getDigsDbUsername(cmdContext);
		String digsDbPassword = getDigsDbPassword(cmdContext);
		try {
			// ensure MySQL driver loaded
			Class.forName(DatabaseConfiguration.Vendor.MySQL.getJdbcDriverClass()).newInstance();
		} catch(Exception e) {
			throw new DigsImporterException(e, Code.DIGS_DB_ERROR, e.getLocalizedMessage());
		}
		Connection connection = null;
		List<String> digsDbNames = new ArrayList<String>();
		try {
			connection = DriverManager.getConnection(digsDbBaseUrl, digsDbUsername, digsDbPassword);
			DatabaseMetaData dbMetadata = connection.getMetaData();
			ResultSet catalogs = dbMetadata.getCatalogs();
			while (catalogs.next()) {
				digsDbNames.add(catalogs.getString(1));
			}
		} catch(SQLException sqle) {
			throw new DigsImporterException(sqle, Code.DIGS_DB_ERROR, sqle.getLocalizedMessage());
		} finally {
			if(connection != null) {
				try { connection.close(); }
				catch(SQLException sqle) {
					GlueLogger.getGlueLogger().warning("Exception closing direct JDBC connection to DIGS MySQL DB: "+sqle.getLocalizedMessage());
				}
			}
		}
		digsDbNames.removeAll(Arrays.asList("information_schema", "mysql", "performance_schema"));
		return digsDbNames;
	}
	
	private ServerRuntime createDigsServerRuntime(CommandContext cmdContext, String digsDbName) {
		String baseUrl = getDigsBaseUrl(cmdContext);
		String digsDbJdbcUrl = baseUrl+"/"+digsDbName;
		String digsDbUsername = getDigsDbUsername(cmdContext);
		String digsDbPassword = getDigsDbPassword(cmdContext);
		return new ServerRuntime(DIGS_DOMAIN_RESOURCE, 
				CayenneUtils.createCayenneDbConfigModule("20000", DatabaseConfiguration.Vendor.MySQL.getJdbcDriverClass(), 
				digsDbJdbcUrl, Optional.of(digsDbUsername), Optional.of(digsDbPassword)));
	}

	public static String getDigsDbPassword(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String digsDbPassword = propertiesConfiguration.getPropertyValue(DIGS_DB_PASSWORD_PROPERTY);
		if(digsDbPassword == null) {
			throw new DigsImporterException(Code.DIGS_DB_JDBC_PASSWORD_NOT_DEFINED);
		}
		return digsDbPassword;
	}

	public static String getDigsDbUsername(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String digsDbUsername = propertiesConfiguration.getPropertyValue(DIGS_DB_USERNAME_PROPERTY);
		if(digsDbUsername == null) {
			throw new DigsImporterException(Code.DIGS_DB_JDBC_USER_NOT_DEFINED);
		}
		return digsDbUsername;
	}

	public static String getDigsBaseUrl(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String baseUrl = propertiesConfiguration.getPropertyValue(DIGS_DB_JDBC_URL_PROPERTY);
		if(baseUrl == null) {
			throw new DigsImporterException(Code.DIGS_DB_JDBC_URL_NOT_DEFINED);
		}
		return baseUrl;
	}

	public ListExtractedResult listExtracted(CommandContext cmdContext, String digsDbName, Optional<Expression> whereClause, List<String> fieldNames) {
		List<Extracted> extracteds = listExtracted(cmdContext, digsDbName, whereClause);
		return new ListExtractedResult(extracteds, fieldNames);
	}
	
	
}