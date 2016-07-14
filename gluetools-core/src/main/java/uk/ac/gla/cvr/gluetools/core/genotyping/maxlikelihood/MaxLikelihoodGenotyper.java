package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.genotyping.GenotypingResult;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.MaxLikelihoodGenotyperException.Code;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.mafft.add.MafftAddRunner;
import uk.ac.gla.cvr.gluetools.programs.raxml.epa.RaxmlEpaRunner;

@PluginClass(elemName="maxLikelihoodGenotyper")
public class MaxLikelihoodGenotyper extends ModulePlugin<MaxLikelihoodGenotyper> {

	public static final String ROOT_ALIGNMENT_NAME = "rootAlignmentName";
	public static final String PHYLO_MEMBER_WHERE_CLAUSE = "phyloMemberWhereClause";
	
	private String rootAlignmentName;
	private Expression phyloMemberWhereClause;
	
	private MafftAddRunner mafftAddRunner = new MafftAddRunner();
	private RaxmlEpaRunner raxmlEpaRunner = new RaxmlEpaRunner();
	
	public MaxLikelihoodGenotyper() {
		super();
		addModulePluginCmdClass(GenotypeCommand.class);
		addSimplePropertyName(ROOT_ALIGNMENT_NAME);
		addSimplePropertyName(PHYLO_MEMBER_WHERE_CLAUSE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.rootAlignmentName = PluginUtils.configureStringProperty(configElem, ROOT_ALIGNMENT_NAME, true);
		this.phyloMemberWhereClause = PluginUtils.configureCayenneExpressionProperty(configElem, PHYLO_MEMBER_WHERE_CLAUSE, false);
		
		Element mafftAddRunnerElem = PluginUtils.findConfigElement(configElem, "mafftAddRunner");
		if(mafftAddRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, mafftAddRunnerElem, mafftAddRunner);
		}
		Element raxmlEpaRunnerElem = PluginUtils.findConfigElement(configElem, "raxmlEpaRunner");
		if(raxmlEpaRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, raxmlEpaRunnerElem, raxmlEpaRunner);
		}
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		// check rootAlignment exists and is constrained.
		Alignment rootAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(rootAlignmentName), true);
		if(rootAlignment == null) {
			throw new MaxLikelihoodGenotyperException(Code.CONFIG_ERROR, "No such alignment \""+rootAlignmentName+"\"");
		}
		if(!rootAlignment.isConstrained()) {
			throw new MaxLikelihoodGenotyperException(Code.CONFIG_ERROR, "Alignment \""+rootAlignmentName+"\" is unconstrained");
		}
	}

	public List<GenotypingResult> genotype(Map<String, DNASequence> querySequenceMap) {
		return null;
	}
	
	
	
}
