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
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.AlignmentException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@GlueDataClass(
		defaultObjectRendererFtlFile = "defaultRenderers/alignment.ftlx",
		defaultListedProperties = { _Alignment.NAME_PROPERTY, Alignment.PARENT_NAME_PATH, Alignment.REF_SEQ_NAME_PATH }, 
		listableBuiltInProperties = { _Alignment.NAME_PROPERTY, _Alignment.DISPLAY_NAME_PROPERTY, Alignment.PARENT_NAME_PATH, Alignment.PARENT_DISPLAY_NAME_PATH, Alignment.REF_SEQ_NAME_PATH, Alignment.DESCRIPTION_PROPERTY }, 
		modifiableBuiltInProperties = { Alignment.DESCRIPTION_PROPERTY, _Alignment.DISPLAY_NAME_PROPERTY }
		)
public class Alignment extends _Alignment implements HasDisplayName {
	
	public static final String REF_SEQ_NAME_PATH = 
			_Alignment.REF_SEQUENCE_PROPERTY+"."+ReferenceSequence.NAME_PROPERTY;
	public static final String PARENT_NAME_PATH = _Alignment.PARENT_PROPERTY+"."+_Alignment.NAME_PROPERTY;

	public static final String PARENT_DISPLAY_NAME_PATH = _Alignment.PARENT_PROPERTY+"."+
			_Alignment.DISPLAY_NAME_PROPERTY;

	
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
		return getAncestorsUpTo(null);
	}

	
	// get a list of ancestors up to and including rootAlmt.
	// or all of them, if rootAlmt == null;
	public List<Alignment> getAncestorsUpTo(Alignment rootAlmt) {
		Alignment current = this;
		LinkedHashSet<Alignment> ancestors = new LinkedHashSet<Alignment>();
		while(current != null) {
			if(!ancestors.contains(current)) {
				ancestors.add(current);
			} else {
				break; // loop avoidance.
			}
			if(rootAlmt != null && current.getName().equals(rootAlmt.getName())) {
				break;
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

	public ReferenceSequence getRelatedRef(CommandContext cmdContext, String referenceName) {
		if(this.getRefSequence() == null) { // unconstrained
			ReferenceSequence relatedRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), false);
			// check the relatedRef is a member of this alignment.
			String relatedRefSeqID = relatedRef.getSequence().getSequenceID();
			String relatedRefSourceName = relatedRef.getSequence().getSource().getName();
			Map<String,String> memberPkMap = AlignmentMember.pkMap(this.getName(), relatedRefSourceName, relatedRefSeqID);
			// this will throw if no such member exists.
			GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap);
			return relatedRef;
		} else {
			return getAncConstrainingRef(cmdContext, referenceName);
		}
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

	public List<QueryAlignedSegment> translateToRelatedRef(CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToAlmtSegs, ReferenceSequence relatedRef) {
		if(this.getRefSequence() != null) {
			return translateToAncConstrainingRef(cmdContext, queryToAlmtSegs, relatedRef);
		} else {
			// unconstrained
			AlignmentMember refMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
					AlignmentMember.pkMap(getName(), 
							relatedRef.getSequence().getSource().getName(), 
							relatedRef.getSequence().getSequenceID()));
			
			
			List<QueryAlignedSegment> uToRefSegs = refMember.getAlignedSegments().stream()
				.map(seg -> seg.asQueryAlignedSegment())
				.map(seg -> seg.invert())
				.collect(Collectors.toList());
			return QueryAlignedSegment.translateSegments(queryToAlmtSegs, uToRefSegs);
		}
	}
		
	public List<QueryAlignedSegment> translateToAncConstrainingRef(CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToConstrainingRefSegs, ReferenceSequence ancConstrainingRef) {
		Alignment currentAlignment = this;
		ReferenceSequence constrainingRef = currentAlignment.getConstrainingRef();
		// translate segments up the tree until we get to the ancestor constraining reference.
		while(!constrainingRef.getName().equals(ancConstrainingRef.getName())) {
			Sequence refSeqSeq = constrainingRef.getSequence();
			Alignment parentAlmt = currentAlignment.getParent();
			queryToConstrainingRefSegs = parentAlmt.translateToRef(cmdContext, 
					refSeqSeq.getSource().getName(), refSeqSeq.getSequenceID(), queryToConstrainingRefSegs);
			currentAlignment = parentAlmt;
			constrainingRef = currentAlignment.getConstrainingRef();
		}
		return queryToConstrainingRefSegs;
	}

	public List<ReferenceSequence> getRelatedRefs() {
		if(this.getRefSequence() == null) { // unconstrained
			List<ReferenceSequence> relatedRefs = new ArrayList<ReferenceSequence>();
			for(AlignmentMember member: getMembers()) {
				relatedRefs.addAll(member.getSequence().getReferenceSequences());
			}
			return relatedRefs;
		}
		else {
			return getAncConstrainingRefs();
		}
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
	
	public Integer getDepth() {
		Alignment parent = getParent();
		if(parent == null) {
			return 0;
		}
		return parent.getDepth() + 1;
	}

	public static PhyloFormat getPhylogenyPhyloFormat(CommandContext cmdContext) {
		String phyloFormatSettingValue = cmdContext.getProjectSettingValue(ProjectSettingOption.ALIGNMENT_PHYLOGENY_FORMAT);
		return PhyloFormat.valueOf(phyloFormatSettingValue);
	}
	
}

