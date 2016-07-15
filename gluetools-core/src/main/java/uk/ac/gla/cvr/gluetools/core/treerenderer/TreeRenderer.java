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
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.treerenderer.TreeRendererException.Code;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTree;

@PluginClass(elemName="treeRenderer")
public class TreeRenderer extends ModulePlugin<TreeRenderer> {

	public static final String ALMT_BRANCH_LENGTH_PROPERTY = "almtBranchLengthProperty";
	public static final String MEMBER_BRANCH_LENGTH_PROPERTY = "memberBranchLengthProperty";
	public static final String ALMT_NAME_TEMPLATE = "almtNameTemplate";
	public static final String MEMBER_NAME_TEMPLATE = "memberNameTemplate";
	public static final String OMIT_SINGLE_CHILD_INTERNALS = "omitSingleChildInternals";
	public static final String FORCE_BIFURCATING = "forceBifurcating";
	
	public static final String DEFAULT_ALIGNMENT_NAME_TEMPLATE = "alignment/${name}";
	public static final String DEFAULT_MEMBER_NAME_TEMPLATE = "member/${sequence.source.name}/${sequence.sequenceID}";

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
	
	private TreeRendererContext treeRendererContext = new TreeRendererContext();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.treeRendererContext.configure(pluginConfigContext, configElem);
		if(treeRendererContext.getAlignmentNameTemplateString() == null) {
			treeRendererContext.setAlignmentNameTemplateString(DEFAULT_ALIGNMENT_NAME_TEMPLATE);
		}
		if(treeRendererContext.getMemberNameTemplateString() == null) {
			treeRendererContext.setMemberNameTemplateString(DEFAULT_MEMBER_NAME_TEMPLATE);
		}
	}
	
	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		this.treeRendererContext.validate(cmdContext);
	}

	public PhyloTree phyloTreeFromAlignment(CommandContext cmdContext,
			Alignment alignment, Optional<Expression> whereClause, boolean recursive, boolean deduplicate) {
		List<AlignmentMember> allMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, 
				recursive, deduplicate, whereClause);
		return phyloTreeFromAlignment(cmdContext, alignment, allMembers, treeRendererContext);
	}

	
	public static PhyloTree phyloTreeFromAlignment(CommandContext cmdContext, 
			Alignment alignment, List<AlignmentMember> allMembers, TreeRendererContext treeRendererContext) {
		Map<String, List<AlignmentMember>> almtNameToMembers = new LinkedHashMap<String, List<AlignmentMember>>();
		allMembers.forEach(almtMember -> {
			almtNameToMembers.
				computeIfAbsent(almtMember.getAlignment().getName(), name -> new ArrayList<AlignmentMember>())
				.add(almtMember);
		});
		if(emptyAlignment(almtNameToMembers, alignment)) {
			throw new TreeRendererException(Code.ALIGNMENT_HAS_NO_MEMBERS_OR_CHILDREN, alignment.getName());
		}
		PhyloTree phyloTree = new PhyloTree();
		phyloTree.setRoot(buildAlmtPhyloSubtree(cmdContext, alignment, almtNameToMembers, treeRendererContext));
		return phyloTree;
	}

	private static boolean emptyAlignment(Map<String, List<AlignmentMember>> almtNameToMembers, Alignment alignment) {
		String almtName = alignment.getName();
		List<AlignmentMember> members = almtNameToMembers.get(almtName);
		if(members != null && !(members.isEmpty())) {
			return false;
		}
		List<Alignment> childAlmts = alignment.getChildren();
		for(Alignment childAlmt: childAlmts) {
			if(!emptyAlignment(almtNameToMembers, childAlmt)) {
				return false;
			}
		}
		return true;
	}
	
	private static PhyloSubtree buildAlmtPhyloSubtree(CommandContext cmdContext, 
			Alignment alignment, Map<String, List<AlignmentMember>> almtNameToMembers,
			TreeRendererContext treeRendererContext) {
		PhyloSubtree almtPhyloSubtree;
		List<AlignmentMember> members = almtNameToMembers.get(alignment.getName());
		List<Alignment> childAlignments = alignment.getChildren();
		PhyloInternal almtPhyloInternal = treeRendererContext.phyloInternalForAlignment(cmdContext, alignment);
		almtPhyloSubtree = almtPhyloInternal;
		if(members != null) {
			for(AlignmentMember member: members) {
				almtPhyloInternal.addBranch(buildMemberPhyloBranch(cmdContext, member, treeRendererContext));
			}
		}
		for(Alignment childAlignment: childAlignments) {
			if(!emptyAlignment(almtNameToMembers, childAlignment)) {
				almtPhyloInternal.addBranch(buildAlmtPhyloBranch(cmdContext, childAlignment, almtNameToMembers, 
						treeRendererContext));
			}
		}
		if(treeRendererContext.getForceBifurcating() && almtPhyloInternal.getBranches().size() > 2) {
			List<PhyloBranch> oldBranches = almtPhyloInternal.getBranches();
			PhyloInternal currentPhyloInternal = almtPhyloInternal;
			int branchIndex = 0;
			while(branchIndex < oldBranches.size() - 2) {
				List<PhyloBranch> newBranches = new ArrayList<PhyloBranch>(oldBranches.subList(branchIndex, branchIndex+1));
				PhyloBranch newBranch = new PhyloBranch();
				PhyloInternal newPhyloInternal = new PhyloInternal();
				newBranch.setSubtree(newPhyloInternal);
				newBranches.add(newBranch);
				currentPhyloInternal.setBranches(newBranches);
				currentPhyloInternal = newPhyloInternal;
			}
			currentPhyloInternal.setBranches(new ArrayList<PhyloBranch>(oldBranches.subList(oldBranches.size() - 2, oldBranches.size())));
		}
		if(treeRendererContext.getOmitSingleChildInternals() && almtPhyloInternal.getBranches().size() == 1) {
			PhyloBranch singleBranch = almtPhyloInternal.getBranches().get(0);
			// clearly branch lengths will be wrong in this scenario
			
			PhyloSubtree singleBranchSubtree = singleBranch.getSubtree();
			singleBranchSubtree.setUserData(treeRendererContext.mergeUserData(almtPhyloInternal, singleBranchSubtree));
			return singleBranchSubtree;
		}
		return almtPhyloSubtree;
	}

	private static PhyloBranch buildAlmtPhyloBranch(CommandContext cmdContext,
			Alignment alignment,
			Map<String, List<AlignmentMember>> almtNameToMembers,
			TreeRendererContext treeRendererContext) {
		PhyloSubtree childPhyloSubtree = 
				buildAlmtPhyloSubtree(cmdContext, alignment, almtNameToMembers, treeRendererContext);
		PhyloBranch almtPhyloBranch = new PhyloBranch();
		almtPhyloBranch.setSubtree(childPhyloSubtree);
		String almtBranchLengthProperty = treeRendererContext.getAlmtBranchLengthProperty();
		if(almtBranchLengthProperty != null) {
			Double almtBranchLength = (Double) alignment.readProperty(almtBranchLengthProperty);
			if(almtBranchLength != null) {
				almtPhyloBranch.setLength(new BigDecimal(almtBranchLength));
			}
		}
		return almtPhyloBranch;
	}

	private static PhyloBranch buildMemberPhyloBranch(
			CommandContext cmdContext, AlignmentMember member, TreeRendererContext treeRendererContext) {
		PhyloLeaf memberPhyloLeaf = treeRendererContext.phyloLeafForMember(cmdContext, member);
		PhyloBranch memberPhyloBranch = new PhyloBranch();
		memberPhyloBranch.setSubtree(memberPhyloLeaf);
		String memberBranchLengthProperty = treeRendererContext.getMemberBranchLengthProperty();
		if(memberBranchLengthProperty != null) {
			Double memberBranchLength = (Double) member.readProperty(memberBranchLengthProperty);
			if(memberBranchLength != null) {
				memberPhyloBranch.setLength(new BigDecimal(memberBranchLength));
			}
		}
		return memberPhyloBranch;
	}

	
}
