package uk.ac.gla.cvr.gluetools.core.reporting.figtree.annotationExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

@PluginClass(elemName="figTreeAnnotationExporter", 
		description="Generates annotation files from project data, suitable for use in FigTree")
public class FigTreeAnnotationExporter extends ModulePlugin<FigTreeAnnotationExporter> {

	public static final String MEMBER_NAME_TEMPLATE = "memberNameTemplate";
	public static final String FIG_TREE_ANNOTATION = "figTreeAnnotation";
	
	public static final String DEFAULT_MEMBER_NAME_TEMPLATE = "alignment/${alignment.name}/member/${sequence.source.name}/${sequence.sequenceID}";

	private Template memberNameTemplate;
	private List<FigTreeAnnotation> figTreeAnnotations;
	
	public FigTreeAnnotationExporter() {
		super();
		registerModulePluginCmdClass(ExportAnnotationsCommand.class);
	}
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		memberNameTemplate = Optional.ofNullable(PluginUtils
				.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, MEMBER_NAME_TEMPLATE, false))
				.orElse(FreemarkerUtils.templateFromString(DEFAULT_MEMBER_NAME_TEMPLATE, pluginConfigContext.getFreemarkerConfiguration()));
		List<Element> figTreeAnnotationElems = PluginUtils.findConfigElements(configElem, FIG_TREE_ANNOTATION);
		figTreeAnnotations = PluginFactory.createPlugins(pluginConfigContext, FigTreeAnnotation.class, figTreeAnnotationElems);
	}
	
	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
	}

	public List<List<String>> getAnnotationRows(CommandContext cmdContext,
			Alignment alignment, Optional<Expression> whereClause, boolean recursive) {
		// could make this configurable
		List<AlignmentMember> allMembers = 
				AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		List<List<String>> allRows = new ArrayList<List<String>>();
		for(AlignmentMember member: allMembers) {
			TemplateModel memberTemplateModel = FreemarkerUtils.templateModelForObject(member);
			List<String> rowValues = new ArrayList<String>();
			rowValues.add(FreemarkerUtils.processTemplate(memberNameTemplate, memberTemplateModel));
			List<FigTreeAnnotation> figTreeAnnotationsToUse = null;
			figTreeAnnotationsToUse = getFigTreeAnnotationsToUse(cmdContext);
			
			for(FigTreeAnnotation figTreeAnnotation: figTreeAnnotationsToUse) {
				rowValues.add(FreemarkerUtils.processTemplate(figTreeAnnotation.getValueFreemarkerTemplate(
						cmdContext.getGluetoolsEngine().getFreemarkerConfiguration()), memberTemplateModel));
			}
			allRows.add(rowValues);
		}
		return allRows;
	}


	private List<FigTreeAnnotation> getFigTreeAnnotationsToUse(
			CommandContext cmdContext) {
		List<FigTreeAnnotation> figTreeAnnotationsToUse;
		if(figTreeAnnotations.isEmpty()) {
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			List<String> listableMemberFields = insideProjectMode.getProject().getListableProperties(ConfigurableTable.alignment_member.name());
			figTreeAnnotationsToUse = listableMemberFields.stream()
					.map(fieldName -> {
						FigTreeAnnotation annot = new FigTreeAnnotation();
						annot.setAnnotationName(fieldName);
						return annot;
					})
					.collect(Collectors.toList());
		} else {
			figTreeAnnotationsToUse = figTreeAnnotations;
		}
		return figTreeAnnotationsToUse;
	}

	public List<String> getColumnHeaders(CommandContext cmdContext) {
		List<String> columnHeaders = new ArrayList<String>();
		columnHeaders.add("taxon");
		getFigTreeAnnotationsToUse(cmdContext).forEach(annot -> columnHeaders.add(annot.getAnnotationName()));
		return columnHeaders;
	}
	
	
	
}
