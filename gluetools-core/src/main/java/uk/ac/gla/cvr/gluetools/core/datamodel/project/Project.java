package uk.ac.gla.cvr.gluetools.core.datamodel.project;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommandException;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;

@GlueDataClass(defaultListColumns = {_Project.NAME_PROPERTY, _Project.DESCRIPTION_PROPERTY})
public class Project extends _Project {

	public static Map<String, String> pkMap(String name) {
		return Collections.singletonMap(NAME_PROPERTY, name);
	}

	@Override
	public void setPKValues(Map<String, String> idMap) {
		setName(idMap.get(NAME_PROPERTY));
	}
	
	public Field getSequenceField(String fieldName) {
		return getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().get();
	}

	public List<String> getCustomSequenceFieldNames() {
		return getFields().stream().map(Field::getName).collect(Collectors.toList());
	}

	public List<String> getAllSequenceFieldNames() {
		List<String> fieldNames = getCustomSequenceFieldNames();
		fieldNames.add(Sequence.SOURCE_NAME_PATH);
		fieldNames.add(Sequence.SEQUENCE_ID_PROPERTY);
		fieldNames.add(Sequence.FORMAT_PROPERTY);
		return fieldNames;
	}

	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getName());
	}

	public void checkValidSequenceFieldNames(List<String> fieldNames) {
		List<String> validFieldNamesList = getAllSequenceFieldNames();
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validFieldNames.contains(f)) {
					throw new ProjectModeCommandException(Code.INVALID_FIELD, f, validFieldNamesList);
				}
			});
		}
	}

	public void checkValidCustomSequenceFieldNames(List<String> fieldNames) {
		List<String> validFieldNamesList = getCustomSequenceFieldNames();
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validFieldNames.contains(f)) {
					throw new ProjectModeCommandException(Code.INVALID_FIELD, f, validFieldNamesList);
				}
			});
		}
	}

	
	public void checkValidMemberFieldNames(List<String> fieldNames) {
		List<String> validMemberFieldsList = getValidMemberFields();
		Set<String> validMemberFields = new LinkedHashSet<String>(validMemberFieldsList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validMemberFields.contains(f)) {
					throw new ProjectModeCommandException(Code.INVALID_FIELD, f, validMemberFieldsList);
				}
			});
		}

	}

	public List<String> getValidMemberFields() {
		List<String> validMemberFieldsList = getAllSequenceFieldNames().stream().
			map(s -> _AlignmentMember.SEQUENCE_PROPERTY+"."+s).collect(Collectors.toList());
		return validMemberFieldsList;
	}

	@Override
	public void generateGlueConfig(int indent, StringBuffer glueConfigBuf,
			GlueConfigContext glueConfigContext) {
		if(glueConfigContext.includeVariationCategories()) {
			List<VariationCategory> topologicallySortedVcats = VariationCategory.getTopologicallySortedVcats(glueConfigContext.getCommandContext());
			for(VariationCategory vcat : topologicallySortedVcats) {
				StringBuffer variationCategoryConfigBuf = new StringBuffer();
				vcat.generateGlueConfig(indent+INDENT, variationCategoryConfigBuf, glueConfigContext);
				if(variationCategoryConfigBuf.length() > 0) {
					indent(glueConfigBuf, indent).append("create variation-category "+vcat.getName());
					String description = vcat.getDescription();
					if(description != null) {
						glueConfigBuf.append( "\""+description+"\"");
					}
					glueConfigBuf.append("\n");
					indent(glueConfigBuf, indent).append("variation-category "+vcat.getName()).append("\n");
					glueConfigBuf.append(variationCategoryConfigBuf.toString());
					indent(glueConfigBuf, indent+INDENT).append("exit").append("\n");
				}
			}
		}
		if(glueConfigContext.includeVariations()) {
			StringBuffer refSeqConfigBuf = new StringBuffer();
			List<ReferenceSequence> refSequences = GlueDataObject.query(glueConfigContext.getCommandContext(), ReferenceSequence.class, new SelectQuery(ReferenceSequence.class));
			for(ReferenceSequence refSequence: refSequences) {
				refSequence.generateGlueConfig(indent+INDENT, refSeqConfigBuf, glueConfigContext);
				if(refSeqConfigBuf.length() > 0) {
					indent(glueConfigBuf, indent).append("reference "+refSequence.getName()).append("\n");
					glueConfigBuf.append(refSeqConfigBuf.toString());
					indent(glueConfigBuf, indent+INDENT).append("exit").append("\n");
				}
			}
		}
	}

	
}
