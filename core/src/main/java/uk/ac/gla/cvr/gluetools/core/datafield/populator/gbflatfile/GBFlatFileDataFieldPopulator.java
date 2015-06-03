package uk.ac.gla.cvr.gluetools.core.datafield.populator.gbflatfile;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulator;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorException;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;

public class GBFlatFileDataFieldPopulator implements DataFieldPopulator, Plugin {

	private List<XmlPopulatorRule> rules;
	
	@Override
	public void populate(CollatedSequence sequence) throws DataFieldPopulatorException {
		
		
	}

	@Override
	public void configure(Element configElem) throws PluginConfigException {
		
	}

}
