package uk.ac.gla.cvr.gluetools.core.textToQuery;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;

@PluginClass(elemName="textToQueryTransformer")
public class TextToQueryTransformer extends ModulePlugin<TextToQueryTransformer> {

	public static final String EXTRACTOR_FORMATTER = "extractorFormatter";
	public static final String DATA_CLASS = "dataClass";
	public static final String DESCRIPTION = "description";
	
	private DataClassEnum dataClassEnum;
	private String description;
	private RegexExtractorFormatter extractorFormatter;

	public enum DataClassEnum {
		ReferenceSequence(ReferenceSequence.class),
		Sequence(Sequence.class),
		Feature(Feature.class),
		FeatureLocation(FeatureLocation.class),
		Alignment(Alignment.class),
		Variation(Variation.class);
		
		@SuppressWarnings("rawtypes")
		private DataClass dataClass;
		
		private <D extends GlueDataObject> DataClassEnum(Class<D> theClass) {
			this.dataClass = new DataClass<D>(theClass);
		}
	}
	
	private static class DataClass<D extends GlueDataObject> {
		private Class<D> theClass;
		
		private DataClass(Class<D> theClass) {
			this.theClass = theClass;
		}
		
		public List<D> query(CommandContext cmdContext, Expression expression) {
			SelectQuery selectQuery = new SelectQuery(theClass, expression);
			return GlueDataObject.query(cmdContext, theClass, selectQuery);
		}
	}

	
	
	public TextToQueryTransformer() {
		super();
		addSimplePropertyName(DATA_CLASS);
		addModulePluginCmdClass(TextToQueryCommand.class);
	}


	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		dataClassEnum = PluginUtils.configureEnumProperty(DataClassEnum.class, configElem, DATA_CLASS, true);
		description = PluginUtils.configureStringProperty(configElem, DESCRIPTION, false);
		Element extractorFormatterElem = PluginUtils.findConfigElement(configElem, EXTRACTOR_FORMATTER, true);
		extractorFormatter = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, extractorFormatterElem);
	}


	@SuppressWarnings("unchecked")
	public ListResult textToQuery(CommandContext cmdContext, String text) {
		String expressionString = extractorFormatter.matchAndConvert(text);
		if(expressionString == null) {
			throw new TextToQueryException(TextToQueryException.Code.EXTRACTOR_FORMATTER_FAILED, description, text);
		}
		Expression expression = CayenneUtils.parseExpression(expressionString);
		@SuppressWarnings("rawtypes")
		List query = dataClassEnum.dataClass.query(cmdContext, expression);
		return new ListResult(dataClassEnum.dataClass.theClass, query);
	}
	
	
	public static TextToQueryTransformer lookupTextToQueryTransformer(CommandContext cmdContext,
			String moduleName, DataClassEnum dataClassEnum) {
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
		ModulePlugin<?> modulePlugin = module.getModulePlugin(cmdContext.getGluetoolsEngine());
		if(!(modulePlugin instanceof TextToQueryTransformer)) {
			throw new TextToQueryException(TextToQueryException.Code.MODULE_IS_NOT_TEXT_TO_QUERY_TRANSFORMER, moduleName);
		}
		TextToQueryTransformer textToQueryTransformer = (TextToQueryTransformer) modulePlugin;
		if(textToQueryTransformer.dataClassEnum != dataClassEnum) {
			throw new TextToQueryException(TextToQueryException.Code.MODULE_QUERY_OBJECT_TYPE_IS_INCORRECT, moduleName, textToQueryTransformer.dataClassEnum, dataClassEnum);
		}
		return textToQueryTransformer;
	}

	
}
