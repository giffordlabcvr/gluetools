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
package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.GenotypingDocumentResult;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.GenotypingTableResult;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.QueryGenotypingResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class AbstractGenotypeCommand<P extends BaseGenotyper<P>> extends ModulePluginCommand<CommandResult, P> {

	public static final String DETAIL_LEVEL = "detailLevel";
	public static final String DOCUMENT_RESULT = "documentResult";
	
	public enum DetailLevel {
		LOW,
		MEDIUM,
		HIGH
	}
	
	private DetailLevel detailLevel = DetailLevel.LOW;
	private Boolean documentResult;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.detailLevel = PluginUtils.configureEnumProperty(DetailLevel.class, configElem, DETAIL_LEVEL, false);
		this.documentResult = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DOCUMENT_RESULT, false)).orElse(false);
		if(detailLevel != null && documentResult) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either --detailLevel or --documentResult may be specified but not both.");
		}
	}

	protected CommandResult formResult(BaseGenotyper<?> maxLikelihoodGenotyper, Map<String, QueryGenotypingResult> genotypeResults) {
		if(documentResult) {
			return new GenotypingDocumentResult(new ArrayList<QueryGenotypingResult>(genotypeResults.values()));
		} else {
			return new GenotypingTableResult(maxLikelihoodGenotyper.getCladeCategories(), detailLevel, new ArrayList<QueryGenotypingResult>(genotypeResults.values()));
		}
	}


}
