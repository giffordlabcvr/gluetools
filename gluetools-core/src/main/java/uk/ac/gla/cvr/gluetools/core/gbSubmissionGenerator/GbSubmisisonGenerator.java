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
package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import freemarker.template.Template;
import freemarker.template.TemplateModel;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.FeatureProvider;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.FeatureProviderFactory;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider.SourceInfoProvider;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider.SourceInfoProviderFactory;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.structuredCommentProvider.StructuredCommentProvider;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


@PluginClass(elemName="gbSubmisisonGenerator",
description="Generates .sqn Genbank submission files from stored GLUE sequences")
public class GbSubmisisonGenerator extends ModulePlugin<GbSubmisisonGenerator> {

	public static final String TBL2ASN_RUNNER = "tbl2AsnRunner";
	public static final String FEATURE_PROVIDERS = "featureProviders";
	public static final String SOURCE_INFO_PROVIDERS = "sourceInfoProviders";
	public static final String SUPPRESS_GLUE_NOTE = "suppressGlueNote";
	public static final String ID_TEMPLATE = "idTemplate";
	public static final String STRUCTURED_COMMENT_PROVIDER = "structuredCommentProvider";
	
	private Tbl2AsnRunner tbl2AsnRunner = new Tbl2AsnRunner();
	
	private List<SourceInfoProvider> sourceInfoProviders = new ArrayList<SourceInfoProvider>();
	private List<FeatureProvider> featureProviders = new ArrayList<FeatureProvider>();
	private StructuredCommentProvider structuredCommentProvider = null;
	
	private Template idTemplate;
	private boolean suppressGlueNote;
	
	public static final String DEFAULT_ID_TEMPLATE = "${sequenceID}";
	
	
	public GbSubmisisonGenerator() {
		super();
		registerModulePluginCmdClass(GenerateSqnCommand.class);
		addSimplePropertyName(ID_TEMPLATE);
		addSimplePropertyName(SUPPRESS_GLUE_NOTE);

	}

	public StructuredCommentProvider getStructuredCommentProvider() {
		return structuredCommentProvider;
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		
		Element structuredCommentProviderElem = PluginUtils.findConfigElement(configElem, STRUCTURED_COMMENT_PROVIDER);
		if(structuredCommentProviderElem != null) {
			this.structuredCommentProvider = PluginFactory.createPlugin(pluginConfigContext, StructuredCommentProvider.class, structuredCommentProviderElem);
		}
		this.idTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, ID_TEMPLATE, false))
				.orElse(FreemarkerUtils.templateFromString(DEFAULT_ID_TEMPLATE, pluginConfigContext.getFreemarkerConfiguration()));
		this.suppressGlueNote = Optional.ofNullable(PluginUtils
				.configureBooleanProperty(configElem, SUPPRESS_GLUE_NOTE, false)).orElse(false);

		Element tbl2AsnRunnerElem = PluginUtils.findConfigElement(configElem, TBL2ASN_RUNNER);
		if(tbl2AsnRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, tbl2AsnRunnerElem, tbl2AsnRunner);
		}
		Element sourceInfoProvidersElem = PluginUtils.findConfigElement(configElem, SOURCE_INFO_PROVIDERS);
		if(sourceInfoProvidersElem != null) {
			SourceInfoProviderFactory sourceInfoProviderFactory = PluginFactory.get(SourceInfoProviderFactory.creator);
			String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(sourceInfoProviderFactory.getElementNames());
			List<Element> sourceInfoProviderElems = PluginUtils.findConfigElements(sourceInfoProvidersElem, alternateElemsXPath);
			this.sourceInfoProviders.addAll(sourceInfoProviderFactory.createFromElements(pluginConfigContext, sourceInfoProviderElems));
		}
		Element featureProvidersElem = PluginUtils.findConfigElement(configElem, FEATURE_PROVIDERS);
		if(featureProvidersElem != null) {
			FeatureProviderFactory featureProviderFactory = PluginFactory.get(FeatureProviderFactory.creator);
			String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(featureProviderFactory.getElementNames());
			List<Element> featureProviderElems = PluginUtils.findConfigElements(featureProvidersElem, alternateElemsXPath);
			this.featureProviders.addAll(featureProviderFactory.createFromElements(pluginConfigContext, featureProviderElems));
		}
	}
	
	public String generateId(Sequence sequence) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(sequence);
		return FreemarkerUtils.processTemplate(idTemplate, templateModel);
	}

	public Tbl2AsnRunner getTbl2AsnRunner() {
		return tbl2AsnRunner;
	}

	
	public boolean getSuppressGlueNote() {
		return suppressGlueNote;
	}

	public List<SourceInfoProvider> getSourceInfoProviders() {
		return sourceInfoProviders;
	}

	public List<FeatureProvider> getFeatureProviders() {
		return featureProviders;
	}

}
