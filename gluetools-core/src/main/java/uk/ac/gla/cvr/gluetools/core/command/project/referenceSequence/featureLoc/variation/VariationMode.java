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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.RenderableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.VariationCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@CommandModeClass(commandFactoryClass = VariationModeCommandFactory.class)
public class VariationMode extends CommandMode<VariationCommand> implements ConfigurableObjectMode, RenderableObjectMode {

	
	private String refSeqName;
	private String featureName;
	private String variationName;
	private Project project;
	
	public VariationMode(Project project, VariationCommand command, String refSeqName, String featureName, String variationName) {
		super(command, variationName);
		this.refSeqName = refSeqName;
		this.featureName = featureName;
		this.variationName = variationName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(VariationModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "variationName", variationName);
		}
	}

	public String getVariationName() {
		return variationName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public String getRefSeqName() {
		return refSeqName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	
	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupVariation(cmdContext);
	}

	@Override
	public GlueDataObject getRenderableObject(CommandContext cmdContext) {
		return lookupVariation(cmdContext);
	}

	protected Variation lookupVariation(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Variation.class, Variation.pkMap(getRefSeqName(), getFeatureName(), getVariationName()));
	}

	@Override
	public String getTableName() {
		return ConfigurableTable.variation.name();
	}

	
}
