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

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.varAlmtNote.VarAlmtNoteMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.varAlmtNote.VarAlmtNoteModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
	commandWords={"var-almt-note"},
	docoptUsages={"<alignmentName>"},
	description="Enter command mode for a variation-alignment note") 
@EnterModeCommandClass(commandFactoryClass = VarAlmtNoteModeCommandFactory.class, 
	modeDescription = "Variation-Alignment note mode")
public class VariationAlmtNoteCommand extends VariationModeCommand<OkResult>  {

	public static final String ALIGNMENT_NAME = "alignmentName";
	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		GlueDataObject.lookup(cmdContext, VarAlmtNote.class, VarAlmtNote.pkMap(
				alignmentName, getRefSeqName(), getFeatureName(), getVariationName()));
		cmdContext
			.pushCommandMode(new VarAlmtNoteMode(getVariationMode(cmdContext).getProject(), 
					this,
					alignmentName, getRefSeqName(), getFeatureName(), getVariationName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("alignmentName", new QualifiedDataObjectNameInstantiator(
					VarAlmtNote.class, VarAlmtNote.ALIGNMENT_NAME_PATH) {
						@SuppressWarnings("rawtypes")
						@Override
						protected void qualifyResults(CommandMode cmdMode,
								Map<String, Object> bindings,
								Map<String, Object> qualifierValues) {
							VariationMode varMode = (VariationMode) cmdMode;
							qualifierValues.put(VarAlmtNote.REF_SEQ_NAME_PATH, varMode.getRefSeqName());
							qualifierValues.put(VarAlmtNote.FEATURE_NAME_PATH, varMode.getFeatureName());
							qualifierValues.put(VarAlmtNote.VARIATION_NAME_PATH, varMode.getVariationName());
						}
				
			});
		}
	}
	

}
