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
package uk.ac.gla.cvr.gluetools.core.treetransformer;

import java.util.List;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import freemarker.template.Template;
import freemarker.template.TemplateModel;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;

@PluginClass(elemName="treeTransformer",
		description="Renames leaf nodes in a Newick file")
public class TreeTransformer extends ModulePlugin<TreeTransformer> {

	public static String LEAF_TO_SEQ_EXTRACTOR_FORMATTER = "leafToSeqExtractorFormatter";
	public static String OUTPUT_LEAF_TEMPLATE = "outputLeafTemplate";
	
	private RegexExtractorFormatter leafToSeqExtractorFormatter;
	
	private Template outputLeafTemplate;

	public TreeTransformer() {
		super();
		addSimplePropertyName(OUTPUT_LEAF_TEMPLATE);
		registerModulePluginCmdClass(TransformTreeCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element extractorElem = PluginUtils.findConfigElement(configElem, LEAF_TO_SEQ_EXTRACTOR_FORMATTER, false);
		if(extractorElem != null) {
			this.leafToSeqExtractorFormatter = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, extractorElem);
		} else {
			this.leafToSeqExtractorFormatter = new RegexExtractorFormatter();
			leafToSeqExtractorFormatter.setOutputTemplate(FreemarkerUtils.templateFromString("sequenceID = '${g0}'", pluginConfigContext.getFreemarkerConfiguration()));
		}
		this.outputLeafTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, OUTPUT_LEAF_TEMPLATE, true);
	}

	public PhyloTree transformTree(CommandContext cmdContext, PhyloTree inputTree) {
		inputTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafName = phyloLeaf.getName();
				String sequenceIdentifingClause = leafToSeqExtractorFormatter.matchAndConvert(leafName);
				SelectQuery selectQuery = new SelectQuery(Sequence.class, CayenneUtils.parseExpression(sequenceIdentifingClause));
				List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
				if(sequences.size() == 0) {
					throw new TreeTransformerException(TreeTransformerException.Code.NO_SEQUENCES_MATCH_QUERY, sequenceIdentifingClause);
				}
				if(sequences.size() > 1) {
					throw new TreeTransformerException(TreeTransformerException.Code.MULTIPLE_SEQUENCES_MATCH_QUERY, sequenceIdentifingClause);
				}
				Sequence sequence = sequences.get(0);
				TemplateModel templateModel = FreemarkerUtils.templateModelForObject(sequence);
				String newName = FreemarkerUtils.processTemplate(outputLeafTemplate, templateModel);
				phyloLeaf.setName(newName);
			}
		});
		return inputTree;
	}
	
	
	
}
