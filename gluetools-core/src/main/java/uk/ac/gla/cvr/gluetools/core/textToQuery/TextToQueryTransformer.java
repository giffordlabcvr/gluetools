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
package uk.ac.gla.cvr.gluetools.core.textToQuery;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.ValueExtractor;
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

@PluginClass(elemName="textToQueryTransformer",
		description="Transforms data extracted from a text string into a GLUE object query")
public class TextToQueryTransformer extends ModulePlugin<TextToQueryTransformer> {

	public static final String VALUE_CONVERTER = "valueConverter";
	public static final String DATA_CLASS = "dataClass";
	public static final String DESCRIPTION = "description";
	
	private DataClassEnum dataClassEnum;
	private String description;
	private RegexExtractorFormatter mainExtractor;
	private List<RegexExtractorFormatter> valueConverters;
	

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
		registerModulePluginCmdClass(TextToQueryCommand.class);
	}


	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		dataClassEnum = PluginUtils.configureEnumProperty(DataClassEnum.class, configElem, DATA_CLASS, true);
		description = PluginUtils.configureStringProperty(configElem, DESCRIPTION, false);
		this.valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter"));
		this.mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem);
	}


	@SuppressWarnings("unchecked")
	public ListResult textToQuery(CommandContext cmdContext, String text) {
		String expressionString = ValueExtractor.extractAndConvert(text, mainExtractor, valueConverters);
		if(expressionString == null) {
			throw new TextToQueryException(TextToQueryException.Code.EXTRACTOR_FORMATTER_FAILED, description, text);
		}
		Expression expression = CayenneUtils.parseExpression(expressionString);
		@SuppressWarnings("rawtypes")
		List query = dataClassEnum.dataClass.query(cmdContext, expression);
		return new ListResult(cmdContext, dataClassEnum.dataClass.theClass, query);
	}
	
	
	public static TextToQueryTransformer lookupTextToQueryTransformer(CommandContext cmdContext,
			String moduleName, DataClassEnum dataClassEnum) {
		TextToQueryTransformer textToQueryTransformer = 
				Module.resolveModulePlugin(cmdContext, TextToQueryTransformer.class, moduleName);
		if(textToQueryTransformer.dataClassEnum != dataClassEnum) {
			throw new TextToQueryException(TextToQueryException.Code.MODULE_QUERY_OBJECT_TYPE_IS_INCORRECT, moduleName, textToQueryTransformer.dataClassEnum, dataClassEnum);
		}
		return textToQueryTransformer;
	}

	
}
