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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"show", "ancestors"}, 
		description="Show the alignment's ancestors",
		docoptUsages={"[-t <toAlmtName>]"},
		docoptOptions={"-t <toAlmtName>, --toAlmtName <toAlmtName>  Stop at specific alignment"},
		metaTags={}
	) 
public class AlignmentShowAncestorsCommand extends AlignmentModeCommand<AlignmentShowAncestorsCommand.ShowAlignmentAncestorsResult>{

	public static final String TO_ALMT_NAME = "toAlmtName";
	private Optional<String> toAlmtName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		toAlmtName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, TO_ALMT_NAME, false));
	}


	@Override
	public ShowAlignmentAncestorsResult execute(CommandContext cmdContext) {
		
		String thisAlmtName = getAlignmentName();
		Alignment thisAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(thisAlmtName));
		Alignment toAlmt = toAlmtName
				.map(name -> GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(name)))
				.orElse(null);
		List<Alignment> ancestors = new ArrayList<Alignment>();
		Alignment currentAlmt = thisAlmt;
		do {
			ancestors.add(currentAlmt);
			if(toAlmt != null && currentAlmt.getName().equals(toAlmt.getName())) {
				break;
			}
			currentAlmt = currentAlmt.getParent();
		} while(currentAlmt != null);
		return new ShowAlignmentAncestorsResult(ancestors);
	}

	
	public static class ShowAlignmentAncestorsResult extends TableResult {

		public ShowAlignmentAncestorsResult(List<Alignment> ancestorAlignments) {
			super("showAlignmentAncestors", Arrays.asList(Alignment.NAME_PROPERTY, Alignment.REF_SEQ_NAME_PATH), 
					ancestorAlignments.stream()
					.map(almt -> {
						Map<String, Object> map = new LinkedHashMap<String, Object>();
						map.put(Alignment.NAME_PROPERTY, almt.readNestedProperty(Alignment.NAME_PROPERTY));
						map.put(Alignment.REF_SEQ_NAME_PATH, almt.readNestedProperty(Alignment.REF_SEQ_NAME_PATH));
						return map;
					})
					.collect(Collectors.toList()));
		}

	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("toAlmtName", Alignment.class, Alignment.NAME_PROPERTY);
		}
	}

}
