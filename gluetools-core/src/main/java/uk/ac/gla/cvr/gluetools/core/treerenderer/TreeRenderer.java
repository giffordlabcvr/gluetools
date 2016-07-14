 

package uk.ac.gla.cvr.gluetools.core.treerenderer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;

@PluginClass(elemName="treeRenderer")
public class TreeRenderer extends ModulePlugin<TreeRenderer> {

	public static final String ALMT_BRANCH_LENGTH_PROPERTY = "almtBranchLengthProperty";
	public static final String MEMBER_BRANCH_LENGTH_PROPERTY = "memberBranchLengthProperty";
	public static final String ALMT_NAME_TEMPLATE = "almtNameTemplate";
	public static final String MEMBER_NAME_TEMPLATE = "memberNameTemplate";
	public static final String OMIT_SINGLE_CHILD_INTERNALS = "omitSingleChildInternals";
	public static final String FORCE_BIFURCATING = "forceBifurcating";
	
	// field on alignments to be used for branch length
	private String almtBranchLengthProperty;
	// field on members to be used for branch length
	private String memberBranchLengthProperty;

	private Template alignmentNameTemplate;
	private Template memberNameTemplate;
	
	private Boolean omitSingleChildInternals;
	private Boolean forceBifurcating;

	public TreeRenderer() {
		super();
		addSimplePropertyName(ALMT_BRANCH_LENGTH_PROPERTY);
		addSimplePropertyName(MEMBER_BRANCH_LENGTH_PROPERTY);
		addSimplePropertyName(ALMT_NAME_TEMPLATE);
		addSimplePropertyName(MEMBER_NAME_TEMPLATE);
		addSimplePropertyName(FORCE_BIFURCATING);
		addSimplePropertyName(OMIT_SINGLE_CHILD_INTERNALS);
		addModulePluginCmdClass(RenderTreeNewickCommand.class);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtBranchLengthProperty = PluginUtils.configureStringProperty(configElem, ALMT_BRANCH_LENGTH_PROPERTY, false);
		this.memberBranchLengthProperty = PluginUtils.configureStringProperty(configElem, ALMT_BRANCH_LENGTH_PROPERTY, false);
		this.alignmentNameTemplate = FreemarkerUtils.templateFromString(
				Optional.ofNullable(PluginUtils.configureStringProperty(configElem, ALMT_NAME_TEMPLATE, false)).orElse("alignment/${name}"), 
				pluginConfigContext.getFreemarkerConfiguration());
		this.memberNameTemplate = FreemarkerUtils.templateFromString(
				Optional.ofNullable(PluginUtils.configureStringProperty(configElem, MEMBER_NAME_TEMPLATE, false)).orElse("member/${sequence.source.name}/${sequence.sequenceID}"), 
				pluginConfigContext.getFreemarkerConfiguration());
		this.omitSingleChildInternals = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, OMIT_SINGLE_CHILD_INTERNALS, false)).orElse(false);
		this.forceBifurcating = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, FORCE_BIFURCATING, false)).orElse(false);
		// possibly disallow branch length properties if omitSingleChildInternals / forceBifurcating are used.
	}
	
	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		InsideProjectMode insideProjectMode = ((InsideProjectMode) cmdContext.peekCommandMode());
		Project project = insideProjectMode.getProject();
		if(almtBranchLengthProperty != null) {
			project.checkProperty(ConfigurableTable.alignment, almtBranchLengthProperty, FieldType.DOUBLE, false);
		}
		if(memberBranchLengthProperty != null) {
			project.checkProperty(ConfigurableTable.alignment_member, memberBranchLengthProperty, FieldType.DOUBLE, false);
		}
	}



	public PhyloTree phyloTreeFromAlignment(CommandContext cmdContext, Alignment alignment, Optional<Expression> memberWhereClause) {
		List<AlignmentMember> allMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, true, true, memberWhereClause);
		Map<String, List<AlignmentMember>> almtNameToMembers = new LinkedHashMap<String, List<AlignmentMember>>();
		allMembers.forEach(almtMember -> {
			almtNameToMembers.
				computeIfAbsent(almtMember.getAlignment().getName(), name -> new ArrayList<AlignmentMember>())
				.add(almtMember);
		});
		PhyloTree phyloTree = new PhyloTree();
		phyloTree.setRoot(buildAlmtPhyloSubtree(cmdContext, alignment, almtNameToMembers));
		return phyloTree;
	}
	
	private PhyloSubtree buildAlmtPhyloSubtree(CommandContext cmdContext, Alignment alignment, Map<String, List<AlignmentMember>> almtNameToMembers) {
		PhyloSubtree almtPhyloSubtree;
		List<AlignmentMember> members = almtNameToMembers.get(alignment.getName());
		List<Alignment> childAlignments = alignment.getChildren();
		if( members == null && childAlignments.isEmpty()) {
			almtPhyloSubtree = new PhyloLeaf();
		} else {
			PhyloInternal almtPhyloInternal = new PhyloInternal();
			almtPhyloSubtree = almtPhyloInternal;
			if(members != null) {
				for(AlignmentMember member: members) {
					almtPhyloInternal.addBranch(buildMemberPhyloBranch(member));
				}
			}
			for(Alignment childAlignment: childAlignments) {
				almtPhyloInternal.addBranch(buildAlmtPhyloBranch(cmdContext, childAlignment, almtNameToMembers));
			}
			if(forceBifurcating && almtPhyloInternal.getBranches().size() > 2) {
				// clearly branch lengths will be wrong in this scenario
				List<PhyloBranch> oldBranches = almtPhyloInternal.getBranches();
				List<PhyloBranch> newBranches = new ArrayList<PhyloBranch>(oldBranches.subList(0, 2));
				almtPhyloInternal.setBranches(newBranches);
				for(PhyloBranch oldBranch: oldBranches.subList(2, oldBranches.size())) {
					PhyloInternal newInternal = new PhyloInternal();
					PhyloBranch branchToOldInternal = new PhyloBranch();
					branchToOldInternal.setSubtree(almtPhyloSubtree);
					newInternal.addBranch(branchToOldInternal);
					newInternal.addBranch(oldBranch);
					almtPhyloSubtree = newInternal;
				}
			}
			if(omitSingleChildInternals && almtPhyloInternal.getBranches().size() == 1) {
				PhyloBranch singleBranch = almtPhyloInternal.getBranches().get(0);
				// clearly branch lengths will be wrong in this scenario
				return singleBranch.getSubtree();
			}
		}
		almtPhyloSubtree.setName(FreemarkerUtils.processTemplate(alignmentNameTemplate, FreemarkerUtils.templateModelForGlueDataObject(alignment)));
		return almtPhyloSubtree;
	}

	private PhyloBranch buildAlmtPhyloBranch(CommandContext cmdContext,
			Alignment alignment,
			Map<String, List<AlignmentMember>> almtNameToMembers) {
		PhyloSubtree childPhyloSubtree = buildAlmtPhyloSubtree(cmdContext, alignment, almtNameToMembers);
		PhyloBranch almtPhyloBranch = new PhyloBranch();
		almtPhyloBranch.setSubtree(childPhyloSubtree);
		if(almtBranchLengthProperty != null) {
			Double almtBranchLength = (Double) alignment.readProperty(almtBranchLengthProperty);
			if(almtBranchLength != null) {
				almtPhyloBranch.setLength(new BigDecimal(almtBranchLength));
			}
		}
		return almtPhyloBranch;
	}

	private PhyloBranch buildMemberPhyloBranch(AlignmentMember member) {
		PhyloLeaf memberPhyloLeaf = new PhyloLeaf();
		memberPhyloLeaf.setName(FreemarkerUtils.processTemplate(memberNameTemplate, FreemarkerUtils.templateModelForGlueDataObject(member)));
		PhyloBranch memberPhyloBranch = new PhyloBranch();
		memberPhyloBranch.setSubtree(memberPhyloLeaf);
		if(memberBranchLengthProperty != null) {
			Double memberBranchLength = (Double) member.readProperty(memberBranchLengthProperty);
			if(memberBranchLength != null) {
				memberPhyloBranch.setLength(new BigDecimal(memberBranchLength));
			}
		}
		return memberPhyloBranch;
	}
	
}
