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
package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.populating.ValueExtractor;
import uk.ac.gla.cvr.gluetools.core.collation.populating.customRowCreator.CustomRowCreator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.customRowCreator.CustomTableUpdate;
import uk.ac.gla.cvr.gluetools.core.collation.populating.joinTableLinkUpdater.JoinTableLinkException;
import uk.ac.gla.cvr.gluetools.core.collation.populating.joinTableLinkUpdater.JoinTableLinkException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.populating.joinTableLinkUpdater.JoinTableLinkUpdate;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

/**
 * Sequence must link many-to-one to the named join custom table.
 * Join table must link must link one-to-many to the named destination custom table.
 * Extracted value must equal the ID of an object in the destination table.
 * 
 * Creates a row in the join table using sequence ID + destination ID as the join table ID.
 * Adds sequence as link target to join object. Then adds destination object as link target to join object.
 */

@PluginClass(elemName="joinTableLink")
public class XmlJoinTableLinkRule extends XmlPopulatorRule implements Plugin, ValueExtractor {
		
		private String joinTableName;
		private String destTableName;
		private Pattern nullRegex;
		private RegexExtractorFormatter mainExtractor = null;
		private List<RegexExtractorFormatter> valueConverters;

		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
			this.joinTableName = PluginUtils.configureString(configElem, "@joinTableName", true);
			this.destTableName = PluginUtils.configureString(configElem, "@destTableName", true);
			this.valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
					PluginUtils.findConfigElements(configElem, "valueConverter"));
			this.mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem);
			this.nullRegex = Optional.ofNullable(
					PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
					orElse(Pattern.compile(DEFAULT_NULL_REGEX));

		}
		
		public void execute(XmlPopulatorContext xmlPopulatorContext, Node node) {
			if(!(xmlPopulatorContext instanceof XmlPopulatorJoinTableLinkUpdateContext)) {
				return;
			}
			XmlPopulatorJoinTableLinkUpdateContext xmlPopulatorJoinTableLinkUpdateContext = 
					((XmlPopulatorJoinTableLinkUpdateContext) xmlPopulatorContext);
			String selectedText;
			try {
				selectedText = GlueXmlUtils.getNodeText(node);
			} catch (Exception e) {
				throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
			}
			if(selectedText != null) {
				String valueExtractorResult = ValueExtractor.extractValue(this, selectedText);
				if(valueExtractorResult != null) {
					String destRowId = valueExtractorResult;
					Sequence sequence = xmlPopulatorContext.getSequence();
					String newJoinRowId = sequence.getSource().getName()+":"+sequence.getSequenceID()+":"+destRowId;
					JoinTableLinkUpdate joinTableLinkUpdate = new JoinTableLinkUpdate(joinTableName, destTableName, newJoinRowId, destRowId);
					xmlPopulatorJoinTableLinkUpdateContext.getJoinTableLinkUpdates().add(joinTableLinkUpdate);
				}
			}
		}

		@Override
		public RegexExtractorFormatter getMainExtractor() {
			return mainExtractor;
		}

		@Override
		public List<RegexExtractorFormatter> getValueConverters() {
			return valueConverters;
		}

		@Override
		public Pattern getNullRegex() {
			return nullRegex;
		}

		@Override
		public void validate(CommandContext cmdContext) {
			Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
			project.checkCustomTableName(joinTableName);
			project.checkCustomTableName(destTableName);
			
			Link seqJoinLink = project.getLink("sequence", joinTableName);
			if(seqJoinLink == null) {
				throw new JoinTableLinkException(Code.CONFIG_ERROR, "Sequence table must have link named '"+joinTableName+"'");
			}
			if(!seqJoinLink.getDestTableName().equals(joinTableName)) {
				throw new JoinTableLinkException(Code.CONFIG_ERROR, "Link from sequence named '"+joinTableName+"' must have destination '"+joinTableName+"'");
			}
			if(! (seqJoinLink.isFromOne() && seqJoinLink.isToMany()) ) {
				throw new JoinTableLinkException(Code.CONFIG_ERROR, "Link from sequence to "+joinTableName+" should be ONE_TO_MANY");
			}
			Link destJoinLink = project.getLink(destTableName, joinTableName);
			if(destJoinLink == null) {
				throw new JoinTableLinkException(Code.CONFIG_ERROR, "Table '"+destTableName+"' must have link named '"+joinTableName+"'");
			}
			if(!destJoinLink.getDestTableName().equals(joinTableName)) {
				throw new JoinTableLinkException(Code.CONFIG_ERROR, "Link from "+destTableName+" named '"+joinTableName+"' must have destination '"+joinTableName+"'");
			}
			if(! (destJoinLink.isFromOne() && destJoinLink.isToMany()) ) {
				throw new JoinTableLinkException(Code.CONFIG_ERROR, "Link from "+destTableName+" to join table should be ONE_TO_MANY");
			}
		}
		
		
}
