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
package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"add", "region-selector"}, 
		description = "Add a region selector", 
		docoptUsages={"-f <featureName> (-a [ -l <lcStart> <lcEnd> ] | -n [ -t <ntStart> <ntEnd> ] )"},
		docoptOptions={
				"-f <featureName>, --featureName <featureName>  Feature to select from", 
				"-a, --aminoAcid                                Add an amino-acid region selector", 
				"-n, --nucleotide                               Add a nucleotide region selector", 
				"-l, --labelledCodon                            Limit to labelled codon region", 
				"-t, --ntRegion                                 Limit to nucleotide region",},
		metaTags = {}, 
		furtherHelp = "The setting will be applied to all variations which the module generates"
)
public class AddRegionSelectorCommand extends ModuleDocumentCommand<OkResult> implements ModuleUpdateDocumentCommand {

	private String featureName;
	private Boolean aminoAcid;
	private Boolean labelledCodon;
	private String lcStart;
	private String lcEnd;
	private Boolean nucleotide;
	private Boolean ntRegion;
	private Integer ntStart;
	private Integer ntEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
		aminoAcid = PluginUtils.configureBooleanProperty(configElem, "aminoAcid", true);
		labelledCodon = PluginUtils.configureBooleanProperty(configElem, "labelledCodon", true);
		lcStart = PluginUtils.configureStringProperty(configElem, "lcStart", false);
		lcEnd = PluginUtils.configureStringProperty(configElem, "lcEnd", false);
		nucleotide = PluginUtils.configureBooleanProperty(configElem, "nucleotide", true);
		ntRegion = PluginUtils.configureBooleanProperty(configElem, "ntRegion", true);
		ntStart = PluginUtils.configureIntProperty(configElem, "ntStart", false);
		ntEnd = PluginUtils.configureIntProperty(configElem, "ntEnd", false);
		
		if(! ( 
				(aminoAcid && !labelledCodon && lcStart == null && lcEnd == null &&
				!nucleotide && !ntRegion && ntStart == null && ntEnd == null) || 

				(aminoAcid && labelledCodon && lcStart != null && lcEnd != null &&
				!nucleotide && !ntRegion && ntStart == null && ntEnd == null) || 

				(!aminoAcid && !labelledCodon && lcStart == null && lcEnd == null &&
				nucleotide && !ntRegion && ntStart == null && ntEnd == null) || 

				(!aminoAcid && !labelledCodon && lcStart == null && lcEnd == null &&
				nucleotide && ntRegion && ntStart != null && ntEnd != null)
				
				) ) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Invalid arguments");
		}
	}

	@Override
	protected OkResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		Element regionSelectorElem;
		if(this.aminoAcid) {
			regionSelectorElem = GlueXmlUtils.appendElement(modulePluginDoc.getDocumentElement(), "aminoAcidRegionSelector");
		} else if(this.nucleotide) {
			regionSelectorElem = GlueXmlUtils.appendElement(modulePluginDoc.getDocumentElement(), "nucleotideRegionSelector");
		} else {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unknown selector type");
		}
		GlueXmlUtils.appendElementWithText(regionSelectorElem, "featureName", this.featureName);
		if(this.aminoAcid && this.labelledCodon) {
			GlueXmlUtils.appendElementWithText(regionSelectorElem, "startCodon", this.lcStart);
			GlueXmlUtils.appendElementWithText(regionSelectorElem, "endCodon", this.lcEnd);
		}
		if(this.nucleotide && this.ntRegion) {
			GlueXmlUtils.appendElementWithText(regionSelectorElem, "startNt", Integer.toString(this.ntStart));
			GlueXmlUtils.appendElementWithText(regionSelectorElem, "endNt", Integer.toString(this.ntEnd));
		}
		return new OkResult();
	}
	
	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("featureName", Feature.class, Feature.NAME_PROPERTY);		}
	}

}
