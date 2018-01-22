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
package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsCommandModeDocumentation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"webdocs", "document-command-mode"}, 
	docoptUsages={"<absoluteModePathID>"},
	metaTags={CmdMeta.webApiOnly, CmdMeta.suppressDocs}, 
	description = "")
public class WebdocsDocumentCommandModeCommand extends WebdocsCommand<PojoCommandResult<WebdocsCommandModeDocumentation>> {

	
	public static final String ABSOLUTE_MODE_PATH_ID = "absoluteModePathID";
	
	private String absoluteModePathID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.absoluteModePathID = PluginUtils.configureStringProperty(configElem, ABSOLUTE_MODE_PATH_ID, true);
	}

	
	@Override
	public PojoCommandResult<WebdocsCommandModeDocumentation> execute(CommandContext cmdContext) {
		return new PojoCommandResult<WebdocsCommandModeDocumentation>(WebdocsCommandModeDocumentation.create(absoluteModePathID));
	}

}
