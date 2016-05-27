package uk.ac.gla.cvr.gluetools.core.datamodel.alignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.HasDisplayName;
import uk.ac.gla.cvr.gluetools.core.datamodel.HasName;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.AlignmentException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@GlueDataClass(
		defaultListedProperties = { _Alignment.NAME_PROPERTY, Alignment.PARENT_NAME_PATH, Alignment.REF_SEQ_NAME_PATH }, 
		listableBuiltInProperties = { _Alignment.NAME_PROPERTY, _Alignment.DISPLAY_NAME_PROPERTY, Alignment.PARENT_NAME_PATH, Alignment.REF_SEQ_NAME_PATH, Alignment.DESCRIPTION_PROPERTY }, 
		modifiableBuiltInProperties = { Alignment.DESCRIPTION_PROPERTY, _Alignment.DISPLAY_NAME_PROPERTY }
		)
public class Alignment extends _Alignment implements HasDisplayName {
	
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
	public Map<String, String> pkMap() {
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

	public List<ReferenceSequence> getAncestorReferences() {
		List<ReferenceSequence> ancestorRefs = new ArrayList<ReferenceSequence>();
		for(Alignment ancAlmt: getAncestors()) {
			ReferenceSequence ancRef = ancAlmt.getRefSequence();
			if(ancRef != null) {
				ancestorRefs.add(ancRef);
			}
		}
		return ancestorRefs;
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

	// given segments aligning a sequence to a specific member of this alignment, translate them to
	// be aligned to the constraining reference of this alignment.
	public List<QueryAlignedSegment> translateToRef(
			CommandContext cmdContext, String memberSourceName, String memberSequenceID,
			List<QueryAlignedSegment> seqToMemberSegs) {
		ReferenceSequence refSequence = this.getRefSequence();
		if(refSequence == null) {
			throw new AlignmentException(Code.ALIGNMENT_IS_UNCONSTRAINED, this.getName());
		}
		AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(getName(), memberSourceName, memberSequenceID));
		List<QueryAlignedSegment> memberToRefSegs = almtMember.getAlignedSegments().stream()
			.map(seg -> seg.asQueryAlignedSegment())
			.collect(Collectors.toList());
		return QueryAlignedSegment.translateSegments(seqToMemberSegs, memberToRefSegs);
	}

	public ReferenceSequence getAncConstrainingRef(CommandContext cmdContext, String referenceName) {
		ReferenceSequence ancConstrainingRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), false);
		List<String> ancestorRefNames = getAncestorReferences()
				.stream().map(ref -> ref.getName()).collect(Collectors.toList());
		
		if(!ancestorRefNames.contains(ancConstrainingRef.getName())) {
        	throw new AlignmentException(AlignmentException.Code.REFERENCE_DOES_NOT_CONSTRAIN_ANCESTOR, referenceName, this.getName());
		}

		return ancConstrainingRef;
	}
	
	public List<ReferenceSequence> getAncestorPathReferences(CommandContext cmdContext, String acRefName) {
		List<ReferenceSequence> ancestorReferences = getAncestorReferences();
		List<ReferenceSequence> ancestorPathReferences = new ArrayList<ReferenceSequence>();
		boolean acRefFound = false;
		for(ReferenceSequence ancestorReference: ancestorReferences) {
			ancestorPathReferences.add(ancestorReference);
			if(ancestorReference.getName().equals(acRefName)) {
				acRefFound = true;
				break;
			}
		}
		if(!acRefFound) {
        	throw new AlignmentException(AlignmentException.Code.REFERENCE_DOES_NOT_CONSTRAIN_ANCESTOR, acRefName, this.getName());
		}
		return ancestorPathReferences;
		
	}

	public ReferenceSequence getConstrainingRef() {
		ReferenceSequence constrainingRef = getRefSequence();
		if(constrainingRef == null) {
        	throw new AlignmentException(AlignmentException.Code.ALIGNMENT_IS_UNCONSTRAINED, this.getName());
		}
		return constrainingRef;
	}

	public List<QueryAlignedSegment> translateToAncConstrainingRef(CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToConstrainingRefSags, ReferenceSequence ancConstrainingRef) {
		Alignment currentAlignment = this;
		ReferenceSequence constrainingRef = currentAlignment.getConstrainingRef();
		// translate segments up the tree until we get to the ancestor constraining reference.
		while(!constrainingRef.getName().equals(ancConstrainingRef.getName())) {
			Sequence refSeqSeq = constrainingRef.getSequence();
			Alignment parentAlmt = currentAlignment.getParent();
			queryToConstrainingRefSags = parentAlmt.translateToRef(cmdContext, 
					refSeqSeq.getSource().getName(), refSeqSeq.getSequenceID(), queryToConstrainingRefSags);
			currentAlignment = parentAlmt;
			constrainingRef = currentAlignment.getConstrainingRef();
		}
		return queryToConstrainingRefSags;
	}

	public List<ReferenceSequence> getAncConstrainingRefs() {
		List<ReferenceSequence> ancConstrainingRefs = new ArrayList<ReferenceSequence>();
		for(Alignment almt: getAncestors()) {
			ReferenceSequence refSequence = almt.getRefSequence();
			if(refSequence != null) {
				ancConstrainingRefs.add(refSequence);
			}
		}
		return ancConstrainingRefs;
	}

	
	public Alignment getAncestorWithReferenceName(String referenceName) {
		Optional<Alignment> ancestor = getAncestors().stream()
				.filter(anc -> (anc.getRefSequence() != null && anc.getRefSequence().getName().equals(referenceName)))
				.findFirst();
		return ancestor.orElse(null);
	}
	
	public boolean isConstrained() {
		return getRefSequence() != null;
	}
}

