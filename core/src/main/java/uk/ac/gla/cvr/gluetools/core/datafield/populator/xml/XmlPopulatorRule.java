package uk.ac.gla.cvr.gluetools.core.datafield.populator.xml;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.StringConverterRule;

public class XmlPopulatorRule {

	private String xPathExpression;
	private String dataFieldName;
	private String extractorRegex;
	private List<StringConverterRule> stringConverterRules;
}
