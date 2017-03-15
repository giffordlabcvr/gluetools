package uk.ac.gla.cvr.gluetools.core.treetransformer;

import java.util.List;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

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
import freemarker.template.Template;
import freemarker.template.TemplateModel;

@PluginClass(elemName="treeTransformer")
public class TreeTransformer extends ModulePlugin<TreeTransformer> {

	public static String LEAF_TO_SEQ_EXTRACTOR_FORMATTER = "leafToSeqExtractorFormatter";
	public static String OUTPUT_LEAF_TEMPLATE = "outputLeafTemplate";
	
	private RegexExtractorFormatter leafToSeqExtractorFormatter;
	
	private Template outputLeafTemplate;

	public TreeTransformer() {
		super();
		addSimplePropertyName(OUTPUT_LEAF_TEMPLATE);
		addModulePluginCmdClass(TransformTreeCommand.class);
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
