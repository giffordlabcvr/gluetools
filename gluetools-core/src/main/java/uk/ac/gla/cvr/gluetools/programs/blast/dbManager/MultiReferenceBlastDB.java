package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;


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
		SelectQuery selectQuery = new SelectQuery(ReferenceSequence.class, ExpressionFactory.inExp(ReferenceSequence.NAME_PROPERTY, referenceNames));
		return GlueDataObject.query(cmdContext, ReferenceSequence.class, selectQuery);
	}

	
}