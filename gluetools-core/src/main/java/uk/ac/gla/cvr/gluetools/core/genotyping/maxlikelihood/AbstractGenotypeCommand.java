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
package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class AbstractGenotypeCommand extends ModulePluginCommand<GenotypeCommandResult, MaxLikelihoodGenotyper> {

	public static final String DETAIL_LEVEL = "detailLevel";
	
	public enum DetailLevel {
		LOW,
		MEDIUM,
		HIGH
	}
	
	private DetailLevel detailLevel = DetailLevel.LOW;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.detailLevel = Optional.ofNullable(PluginUtils.configureEnumProperty(DetailLevel.class, configElem, DETAIL_LEVEL, false)).orElse(detailLevel);
	}

	protected GenotypeCommandResult formResult(MaxLikelihoodGenotyper maxLikelihoodGenotyper, Map<String, QueryGenotypingResult> genotypeResults) {
		return new GenotypeCommandResult(maxLikelihoodGenotyper.getCladeCategories(), detailLevel, new ArrayList<QueryGenotypingResult>(genotypeResults.values()));
	}


}
