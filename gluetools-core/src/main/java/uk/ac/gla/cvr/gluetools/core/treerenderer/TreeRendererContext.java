package uk.ac.gla.cvr.gluetools.core.treerenderer;

import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;

public class TreeRendererContext {
	
	
	// field on alignments to be used for branch length
	private String almtBranchLengthProperty = null;
	// field on members to be used for branch length
	private String memberBranchLengthProperty = null;

	private String alignmentNameTemplateString = null;
	private String memberNameTemplateString = null;
	
	private Boolean omitSingleChildInternals = false;
	private Boolean forceBifurcating = false;
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.almtBranchLengthProperty = PluginUtils.configureStringProperty(configElem, TreeRenderer.ALMT_BRANCH_LENGTH_PROPERTY, false);
		this.memberBranchLengthProperty = PluginUtils.configureStringProperty(configElem, TreeRenderer.ALMT_BRANCH_LENGTH_PROPERTY, false);
		this.alignmentNameTemplateString = PluginUtils.configureStringProperty(configElem, TreeRenderer.ALMT_NAME_TEMPLATE, false); 
		this.memberNameTemplateString = PluginUtils.configureStringProperty(configElem, TreeRenderer.MEMBER_NAME_TEMPLATE, false);
		this.omitSingleChildInternals = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, TreeRenderer.OMIT_SINGLE_CHILD_INTERNALS, false)).orElse(omitSingleChildInternals);
		this.forceBifurcating = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, TreeRenderer.FORCE_BIFURCATING, false)).orElse(forceBifurcating);
		// possibly disallow branch length properties if omitSingleChildInternals / forceBifurcating are used.
	}

	public void validate(CommandContext cmdContext) {
		InsideProjectMode insideProjectMode = ((InsideProjectMode) cmdContext.peekCommandMode());
		Project project = insideProjectMode.getProject();
		if(almtBranchLengthProperty != null) {
			project.checkProperty(ConfigurableTable.alignment.name(), almtBranchLengthProperty, FieldType.DOUBLE, false);
		}
		if(memberBranchLengthProperty != null) {
			project.checkProperty(ConfigurableTable.alignment_member.name(), memberBranchLengthProperty, FieldType.DOUBLE, false);
		}
	}

	public void setAlmtBranchLengthProperty(String almtBranchLengthProperty) {
		this.almtBranchLengthProperty = almtBranchLengthProperty;
	}

	public void setMemberBranchLengthProperty(String memberBranchLengthProperty) {
		this.memberBranchLengthProperty = memberBranchLengthProperty;
	}

	public void setOmitSingleChildInternals(Boolean omitSingleChildInternals) {
		this.omitSingleChildInternals = omitSingleChildInternals;
	}

	public void setForceBifurcating(Boolean forceBifurcating) {
		this.forceBifurcating = forceBifurcating;
	}

	public String getAlmtBranchLengthProperty() {
		return almtBranchLengthProperty;
	}

	public String getMemberBranchLengthProperty() {
		return memberBranchLengthProperty;
	}

	public String getAlignmentNameTemplateString() {
		return alignmentNameTemplateString;
	}

	public void setAlignmentNameTemplateString(String alignmentNameTemplateString) {
		this.alignmentNameTemplateString = alignmentNameTemplateString;
	}

	public String getMemberNameTemplateString() {
		return memberNameTemplateString;
	}

	public void setMemberNameTemplateString(String memberNameTemplateString) {
		this.memberNameTemplateString = memberNameTemplateString;
	}

	public Boolean getOmitSingleChildInternals() {
		return omitSingleChildInternals;
	}

	public Boolean getForceBifurcating() {
		return forceBifurcating;
	}

	public PhyloInternal phyloInternalForAlignment(CommandContext commandContext, Alignment alignment) {
		PhyloInternal alignmentPhyloInternal = new PhyloInternal();
		setAlignmentPhyloInternalName(commandContext, alignment, alignmentPhyloInternal);
		return alignmentPhyloInternal;
	}

	protected void setAlignmentPhyloInternalName(CommandContext commandContext,
			Alignment alignment, PhyloSubtree<?> alignmentPhyloInternal) {
		String alignmentNameTemplateString = getAlignmentNameTemplateString();
		if(alignmentNameTemplateString != null) {
			Template template = FreemarkerUtils.templateFromString(alignmentNameTemplateString, commandContext.getGluetoolsEngine().getFreemarkerConfiguration());
			alignmentPhyloInternal.setName(FreemarkerUtils.processTemplate(template, FreemarkerUtils.templateModelForGlueDataObject(alignment)));
		}
	}

	public PhyloLeaf phyloLeafForMember(CommandContext commandContext, AlignmentMember alignmentMember) {
		PhyloLeaf memberPhyloLeaf = new PhyloLeaf();
		setMemberPhyloLeafName(commandContext, alignmentMember, memberPhyloLeaf);
		return memberPhyloLeaf;
	}

	protected void setMemberPhyloLeafName(CommandContext commandContext,
			AlignmentMember alignmentMember, PhyloLeaf memberPhyloLeaf) {
		String memberNameTemplateString = getMemberNameTemplateString();
		if(memberNameTemplateString != null) {
			Template template = FreemarkerUtils.templateFromString(memberNameTemplateString, commandContext.getGluetoolsEngine().getFreemarkerConfiguration());
			memberPhyloLeaf.setName(FreemarkerUtils.processTemplate(template, 
					FreemarkerUtils.templateModelForGlueDataObject(alignmentMember)));
		}
	}

	public Map<String, Object> mergeUserData(PhyloObject<?> almtPhyloInternal,
			PhyloObject<?> singleBranchSubtree) {
		return singleBranchSubtree.getUserData();
	}
}