package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import java.util.List;

import org.w3c.dom.Element;

import freemarker.template.Template;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.Tbl2AsnException;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.Tbl2AsnException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;

public class QualifierKeyValueTemplate implements Plugin {

	public static final String KEY = "key";
	public static final String VALUE_TEMPLATE = "valueTemplate";
	public static final String INCLUDE_ON_SEQUENCE_ID = "includeOnSequenceID";
	public static final String EXCLUDE_ON_SEQUENCE_ID = "excludeOnSequenceID";
	
	private String key;
	private Template valueTemplate;
	
	private List<String> sequenceIDsToIncludeOn;
	private List<String> sequenceIDsToExcludeOn;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.key = PluginUtils.configureStringProperty(configElem, KEY, true);
		this.valueTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, VALUE_TEMPLATE, true);
		this.sequenceIDsToIncludeOn = PluginUtils.configureStringsProperty(configElem, INCLUDE_ON_SEQUENCE_ID);
		if(this.sequenceIDsToIncludeOn.isEmpty()) {
			this.sequenceIDsToIncludeOn = null;
		}
		this.sequenceIDsToExcludeOn = PluginUtils.configureStringsProperty(configElem, EXCLUDE_ON_SEQUENCE_ID);
		if(this.sequenceIDsToExcludeOn.isEmpty()) {
			this.sequenceIDsToExcludeOn = null;
		}
	}

	public String getKey() {
		return key;
	}
	
	public String generateValueFromFeatureLocation(CommandContext cmdContext, FeatureLocation featureLocation) {
		Object templateModel = FreemarkerUtils.templateModelForObject(featureLocation);
		return FreemarkerUtils.processTemplate(valueTemplate, templateModel);
	}
	
	public boolean includeForSequenceID(String sequenceID) {
		if(sequenceIDsToIncludeOn != null && sequenceIDsToExcludeOn != null) {
			if(sequenceIDsToIncludeOn.contains(sequenceID) && sequenceIDsToExcludeOn.contains(sequenceID)) {
				throw new Tbl2AsnException(Code.TBL2ASN_CONFIG_EXCEPTION, "Sequence ID '"+sequenceID+"' is both included and excluded for key "+key);
			}
		}
		if(sequenceIDsToIncludeOn != null) {
			if(sequenceIDsToIncludeOn.contains(sequenceID)) {
				return true;
			} else {
				return false;
			}
		}
		if(sequenceIDsToExcludeOn != null) {
			if(sequenceIDsToExcludeOn.contains(sequenceID)) {
				return false;
			} else {
				return true;
			}
		}
		return true;
	}
	
}
