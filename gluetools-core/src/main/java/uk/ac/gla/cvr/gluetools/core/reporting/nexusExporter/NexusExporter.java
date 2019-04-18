package uk.ac.gla.cvr.gluetools.core.reporting.nexusExporter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Element;

import freemarker.template.Configuration;
import freemarker.template.Template;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.newick.NewickGenerator;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickGenerator;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator.MemberAnnotationGenerator;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporter;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;


@PluginClass(elemName="nexusExporter", 
		description="Generates Nexus files from project data, suitable for use in FigTree")
public class NexusExporter extends ModulePlugin<NexusExporter> {

	public static final String PHYLO_FIELD_NAME = "phyloFieldName";
	public static final String MEMBER_NAME_TEMPLATE = "memberNameTemplate";
	
	private static final String DEFAULT_MEMBER_NAME_TEMPLATE = "alignment/${alignment.name}/member/${sequence.source.name}/${sequence.sequenceID}";
	private static final String FIGTREE_PROPERTY = "figtreeProperty";
	
	private String phyloFieldName;
	private List<Object> annotationGeneratorsAndGroups;
	private Template memberNameTemplate;
	private Map<String, Object> figtreeProperties;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.phyloFieldName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, PHYLO_FIELD_NAME, false)).orElse("phylogeny");
		this.memberNameTemplate = 
				Optional.ofNullable(PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, MEMBER_NAME_TEMPLATE, false))
				.orElse(defaultTemplate(pluginConfigContext.getFreemarkerConfiguration()));
		this.annotationGeneratorsAndGroups = MemberAnnotationGenerator.configureAnnotationGeneratorsAndGroups(pluginConfigContext, configElem);

		this.figtreeProperties = defaultFigtreeProperties();
		List<Element> figtreePropertyElems = PluginUtils.findConfigElements(configElem, FIGTREE_PROPERTY);
		for(Element figtreePropertyElem: figtreePropertyElems) {
			String name = PluginUtils.configureString(figtreePropertyElem, "@name", true);
			String value = PluginUtils.configureString(figtreePropertyElem, "text()", true);
			String type = PluginUtils.configureString(figtreePropertyElem, "@type", "string");
			try {
				if(type.equals("string")) {
					this.figtreeProperties.put(name, value);
				} else if(type.equals("integer")) {
					this.figtreeProperties.put(name, Integer.parseInt(value));
				} else if(type.equals("boolean")) {
					this.figtreeProperties.put(name, Boolean.parseBoolean(value));
				} else if(type.equals("double")) {
					this.figtreeProperties.put(name, Double.parseDouble(value));
				} else if(type.equals("unquoted")) {
					this.figtreeProperties.put(name, new UnquotedValue(value));
				}
			} catch(NumberFormatException nfe) {
				throw new PluginConfigException(nfe, PluginConfigException.Code.CONFIG_CONSTRAINT_VIOLATION, "figtreeProperty "+name+
						" of type "+type+" incorrectly formatted"); }
		}
	}

	
	
	private Map<String, Object> defaultFigtreeProperties() {
		Map<String, Object> figtreeProperties = new LinkedHashMap<String, Object>();
		
		figtreeProperties.put("appearance.backgroundColorAttribute", "Default");
		figtreeProperties.put("appearance.backgroundColour", new UnquotedValue("#ffffff"));
		figtreeProperties.put("appearance.branchColorAttribute", "User selection");
		figtreeProperties.put("appearance.branchColorGradient", false);
		figtreeProperties.put("appearance.branchLineWidth", 1.0);
		figtreeProperties.put("appearance.branchMinLineWidth", 0.0);
		figtreeProperties.put("appearance.branchWidthAttribute", "Fixed");
		figtreeProperties.put("appearance.foregroundColour", new UnquotedValue("#000000"));
		figtreeProperties.put("appearance.hilightingGradient", false);
		figtreeProperties.put("appearance.selectionColour", new UnquotedValue("#2d3680"));
		figtreeProperties.put("branchLabels.colorAttribute", "User selection");
		figtreeProperties.put("branchLabels.displayAttribute", "Branch times");
		figtreeProperties.put("branchLabels.fontName", "Abadi MT Condensed Extra Bold");
		figtreeProperties.put("branchLabels.fontSize", 8);
		figtreeProperties.put("branchLabels.fontStyle", 0);
		figtreeProperties.put("branchLabels.isShown", false);
		figtreeProperties.put("branchLabels.significantDigits", 4);
		figtreeProperties.put("layout.expansion", 0);
		figtreeProperties.put("layout.layoutType", "RECTILINEAR");
		figtreeProperties.put("layout.zoom", 0);
		figtreeProperties.put("legend.attribute", "bootstraps");
		figtreeProperties.put("legend.fontSize", 10.0);
		figtreeProperties.put("legend.isShown", false);
		figtreeProperties.put("legend.significantDigits", 4);
		figtreeProperties.put("nodeBars.barWidth", 4.0);
		figtreeProperties.put("nodeBars.displayAttribute", new UnquotedValue("null"));
		figtreeProperties.put("nodeBars.isShown", false);
		figtreeProperties.put("nodeLabels.colorAttribute", "User selection");
		figtreeProperties.put("nodeLabels.fontName", "Abadi MT Condensed Extra Bold");
		figtreeProperties.put("nodeLabels.fontSize", 8);
		figtreeProperties.put("nodeLabels.fontStyle", 0);
		figtreeProperties.put("nodeLabels.significantDigits", 4);
		figtreeProperties.put("nodeShape.colourAttribute", "User selection");
		figtreeProperties.put("nodeShape.isShown", false);
		figtreeProperties.put("nodeShape.minSize", 10.0);
		figtreeProperties.put("nodeShape.scaleType", new UnquotedValue("Width"));
		figtreeProperties.put("nodeShape.shapeType", new UnquotedValue("Circle"));
		figtreeProperties.put("nodeShape.size", 4.0);
		figtreeProperties.put("nodeShape.sizeAttribute", "Fixed");
		figtreeProperties.put("polarLayout.alignTipLabels", false);
		figtreeProperties.put("polarLayout.angularRange", 0);
		figtreeProperties.put("polarLayout.rootAngle", 0);
		figtreeProperties.put("polarLayout.rootLength", 100);
		figtreeProperties.put("polarLayout.showRoot", true);
		figtreeProperties.put("radialLayout.spread", 0.0);
		figtreeProperties.put("rectilinearLayout.alignTipLabels", false);
		figtreeProperties.put("rectilinearLayout.curvature", 0);
		figtreeProperties.put("rectilinearLayout.rootLength", 100);
		figtreeProperties.put("scale.offsetAge", 0.0);
		figtreeProperties.put("scale.rootAge", 1.0);
		figtreeProperties.put("scale.scaleFactor", 1.0);
		figtreeProperties.put("scale.scaleRoot", false);
		figtreeProperties.put("scaleAxis.automaticScale", true);
		figtreeProperties.put("scaleAxis.fontSize", 8.0);
		figtreeProperties.put("scaleAxis.isShown", false);
		figtreeProperties.put("scaleAxis.lineWidth", 1.0);
		figtreeProperties.put("scaleAxis.majorTicks", 1.0);
		figtreeProperties.put("scaleAxis.origin", 0.0);
		figtreeProperties.put("scaleAxis.reverseAxis", false);
		figtreeProperties.put("scaleAxis.showGrid", true);
		figtreeProperties.put("scaleBar.automaticScale", true);
		figtreeProperties.put("scaleBar.fontSize", 10.0);
		figtreeProperties.put("scaleBar.isShown", true);
		figtreeProperties.put("scaleBar.lineWidth", 1.0);
		figtreeProperties.put("scaleBar.scaleRange", 0.0);
		figtreeProperties.put("tipLabels.fontName", "Abadi MT Condensed Extra Bold");
		figtreeProperties.put("tipLabels.fontSize", 14);
		figtreeProperties.put("tipLabels.fontStyle", 0);
		figtreeProperties.put("tipLabels.isShown", true);
		figtreeProperties.put("tipLabels.significantDigits", 4);
		figtreeProperties.put("trees.order", false);
		figtreeProperties.put("trees.orderType", "increasing");
		figtreeProperties.put("trees.rooting", false);
		figtreeProperties.put("trees.rootingType", "User Selection");
		figtreeProperties.put("trees.transform", false);
		figtreeProperties.put("trees.transformType", "cladogram");
		return figtreeProperties;
	}

	private class UnquotedValue {
		private String value;
		public UnquotedValue(String value) {
			super();
			this.value = value;
		}
		public String getValue() {
			return value;
		}
	}
	
	
	private Template defaultTemplate(Configuration freemarkerConfiguration) {
		return FreemarkerUtils.templateFromString(DEFAULT_MEMBER_NAME_TEMPLATE, freemarkerConfiguration);
	}
		

	public NexusExporter() {
		super();
		registerModulePluginCmdClass(ExportTreeCommand.class);
		addSimplePropertyName(PHYLO_FIELD_NAME);
	}

	public String exportNexus(CommandContext cmdContext, Alignment alignment) {
		
		
		
		boolean recursive;
		if(alignment.isConstrained()) {
			recursive = true;
		} else {
			recursive = false;
		}
		PhyloTree phyloTree = PhyloExporter.exportAlignmentPhyloTree(cmdContext, alignment, phyloFieldName, recursive);
		List<AlignmentMember> almtMembers = new ArrayList<AlignmentMember>();
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafName = phyloLeaf.getName();
				Map<String,String> memberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, leafName);
				AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap);
				almtMembers.add(almtMember);
			}
		});
		almtMembers.sort(new Comparator<AlignmentMember>() {
			@Override
			public int compare(AlignmentMember o1, AlignmentMember o2) {
				int comp;
				comp = o1.getAlignment().getName().compareTo(o2.getAlignment().getName());
				if(comp != 0) return comp;
				comp = o1.getSequence().getSource().getName().compareTo(o2.getSequence().getSource().getName());
				if(comp != 0) return comp;
				comp = o1.getSequence().getSequenceID().compareTo(o2.getSequence().getSequenceID());
				if(comp != 0) return comp;
				return 0;
			}
		});
		StringBuffer buf = new StringBuffer();
		buf.append("#NEXUS\n");
		buf.append("begin taxa;\n");
		buf.append("\tdimensions ntax=").append(Integer.toString(almtMembers.size())).append(";\n");
		buf.append("\ttaxlabels\n");
		for(AlignmentMember almtMember: almtMembers) {
			LinkedHashMap<String, String> annotations = MemberAnnotationGenerator.generateAnnotations(cmdContext, almtMember,
					annotationGeneratorsAndGroups);
			
			String memberName = templateMemberName(almtMember);
			buf.append("\t'").append(memberName).append("'[&");
			List<Map.Entry<String, String>> annotationEntries = new ArrayList<Map.Entry<String, String>>(annotations.entrySet());
			for(int i = 0; i < annotationEntries.size(); i++) {
				if(i > 0) { buf.append(","); }
				Entry<String, String> annotationEntry = annotationEntries.get(i);
				buf.append(annotationEntry.getKey()).append("=");
				buf.append("\"").append(StringEscapeUtils.escapeJava(annotationEntry.getValue())).append("\"");
			}
			buf.append("]\n");
		}
		buf.append(";\n");
		buf.append("end;\n\n");

		buf.append("begin trees;\n");
		buf.append("\ttree tree_1 = [&R] ");

		PhyloTreeToNewickGenerator newickNexusGenerator = new PhyloTreeToNewickGenerator(
				new NewickGenerator() {
					@Override
					public String generateLeafName(PhyloLeaf phyloLeaf) {
						String leafName = phyloLeaf.getName();
						Map<String,String> memberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, leafName);
						AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap);
						return "'"+templateMemberName(almtMember)+"'";
					}
					@Override
					public String generateInternalComment(PhyloInternal phyloInternal) {
						Integer bootstraps = (Integer) phyloInternal.ensureUserData().get("bootstraps");
						if(bootstraps != null) {
							return "&bootstraps="+Integer.toString(bootstraps); 
						}
						return null;
					}
				}
		);
		phyloTree.accept(newickNexusGenerator);
		buf.append(newickNexusGenerator.getNewickString());
		buf.append("end;\n\n");

		buf.append("begin figtree;\n");
		this.figtreeProperties.forEach((name, value) -> {
			buf.append("\tset ").append(name).append("=");
			if(value instanceof String) {
				buf.append("\""+StringEscapeUtils.escapeJava((String) value)+"\"");
			} else if(value instanceof UnquotedValue) {
				buf.append(((UnquotedValue) value).getValue());
			} else if(value instanceof Boolean) {
				buf.append(Boolean.toString((Boolean) value));
			} else if(value instanceof Integer) {
				buf.append(Integer.toString((Integer) value));
			} else if(value instanceof Double) {
				buf.append(Double.toString((Double) value));
			}
			buf.append(";\n");
		});
		buf.append("end;\n\n");
		
		return buf.toString();
		
	}



	private String templateMemberName(AlignmentMember almtMember) {
		return FreemarkerUtils.processTemplate(memberNameTemplate, FreemarkerUtils.templateModelForObject(almtMember));	
	}
	
}
