/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;


public class MultiReferenceBlastDB extends BlastDB<MultiReferenceBlastDB> {
	
	private String name;
	private Set<String> referenceNames;
	
	public MultiReferenceBlastDB(MultiReferenceBlastDbKey key, String name, Set<String> referenceNames) {
		super(key);
		this.name = name;
		this.referenceNames = referenceNames;
	}

	public static class MultiReferenceBlastDbKey extends BlastDbKey<MultiReferenceBlastDB> {

		private String name;
		private Set<String> referenceNames;

		public MultiReferenceBlastDbKey(String projectName, String name, Set<String> referenceNames) {
			super(projectName);
			this.name = name;
			this.referenceNames = referenceNames;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result
					+ ((getProjectName() == null) ? 0 : getProjectName().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MultiReferenceBlastDbKey other = (MultiReferenceBlastDbKey) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (getProjectName() == null) {
				if (other.getProjectName() != null)
					return false;
			} else if (!getProjectName().equals(other.getProjectName()))
				return false;
			return true;
		}

		@Override
		protected File getProjectRelativeBlastDbDir(File projectPath) {
			return new File(projectPath, "multi_reference_"+name);
		}

		@Override
		public MultiReferenceBlastDB createBlastDB() {
			return new MultiReferenceBlastDB(this, name, referenceNames);
		}
		
	}


	@Override
	public String getTitle() {
		return "Reference DB "+name+" in project "+getKey().getProjectName();
	}

	@Override
	public long getLastUpdateTime(CommandContext cmdContext) {
		long lastUpdateTime = -1;
		List<ReferenceSequence> referenceSequences = getReferenceSequences(cmdContext);
		for(ReferenceSequence refSeq: referenceSequences) {
			long creationTime = refSeq.getCreationTime();
			if(creationTime > lastUpdateTime) {
				lastUpdateTime = creationTime;
			}
		}
		return lastUpdateTime;
	}

	@Override
	public InputStream getFastaContentInputStream(CommandContext cmdContext) {
		List<ReferenceSequence> referenceSequences = getReferenceSequences(cmdContext);
		Map<String, DNASequence> sequenceIdToNucleotides = new LinkedHashMap<String, DNASequence>();
		referenceSequences.forEach(refSeq -> {
			String nucleotides = refSeq.getSequence().getSequenceObject().getNucleotides(cmdContext);
			sequenceIdToNucleotides.put(refSeq.getName(), FastaUtils.ntStringToSequence(nucleotides));
		});
		byte[] fastaBytes = FastaUtils.mapToFasta(sequenceIdToNucleotides, LineFeedStyle.forOS());
		return new ByteArrayInputStream(fastaBytes);
	}

	private List<ReferenceSequence> getReferenceSequences(CommandContext cmdContext) {
		return referenceNames.stream()
				.map(rn -> GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(rn)))
				.collect(Collectors.toList());
	}

	
}