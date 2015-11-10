package uk.ac.gla.cvr.gluetools.core.datamodel.alignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.AlignmentException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@GlueDataClass(defaultListColumns = {_Alignment.NAME_PROPERTY, Alignment.PARENT_NAME_PATH, Alignment.REF_SEQ_NAME_PATH})
public class Alignment extends _Alignment {
	
	public static final String REF_SEQ_NAME_PATH = 
			_Alignment.REF_SEQUENCE_PROPERTY+"."+ReferenceSequence.NAME_PROPERTY;
	public static final String PARENT_NAME_PATH = _Alignment.PARENT_PROPERTY+"."+_Alignment.NAME_PROPERTY;
	
	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}
	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getName());
	}
	
	public List<Alignment> getAncestors() {
		Alignment current = this;
		LinkedHashSet<Alignment> ancestors = new LinkedHashSet<Alignment>();
		while(current != null) {
			if(!ancestors.contains(current)) {
				ancestors.add(current);
			} else {
				break; // loop avoidance.
			}
			current = current.getParent();
		}
		return new ArrayList<Alignment>(ancestors);
	}

	@Override
	public void setParent(Alignment parent) {
		if(parent != null) {
			Set<String> loopAlignmentNames = new LinkedHashSet<String>();
			loopAlignmentNames.add(this.getName());
			Alignment current = parent;
			while(current != null) {
				String currentName = current.getName();
				if(loopAlignmentNames.contains(currentName)) {
					List<String> loopNames = new ArrayList<String>(loopAlignmentNames);
					loopNames.add(currentName);
					throw new AlignmentException(Code.PARENT_RELATIONSHIP_LOOP, loopNames);
				} else {
					loopAlignmentNames.add(currentName);
					current = current.getParent();
				}
			}
			ReferenceSequence refSequence = this.getRefSequence();
			if(refSequence == null) {
				throw new AlignmentException(Code.ALIGNMENT_IS_UNCONSTRAINED, this.getName());
			}
			List<AlignmentMember> refAlmtMemberships = refSequence.getSequence().getAlignmentMemberships();
			boolean refIsMemberOfParent = false;
			String parentName = parent.getName();
			for(AlignmentMember almtMembership : refAlmtMemberships) {
				if(almtMembership.getAlignment().getName().equals(parentName)) {
					refIsMemberOfParent = true;
					break;
				}
			}
			if(!refIsMemberOfParent) {
				throw new AlignmentException(Code.REFERENCE_NOT_MEMBER_OF_PARENT, this.getName(), parentName, refSequence.getName());
			}
		}
		super.setParent(parent);
	}

	
	public List<Alignment> getDescendents() {
		List<Alignment> descendents = new ArrayList<Alignment>();
		List<Alignment> children = getChildren();
		for(Alignment childAlignment: children) {
			descendents.add(childAlignment);
			descendents.addAll(childAlignment.getDescendents());
		}
		return descendents;
	}

	
}
