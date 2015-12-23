package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.OriginalDataResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowOriginalDataCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup.GroupShowLastUpdateTimeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup.ListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.groupMember.GroupMember;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class SequenceGroupBlastDB extends BlastDB {

	private String groupName;

	public SequenceGroupBlastDB(String project, String groupName) {
		super(project);
		this.groupName = groupName;
	}
	
	public static class SequenceGroupBlastDbKey extends BlastDbKey<SequenceGroupBlastDB> {

		private String projectName;
		private String groupName;

		public SequenceGroupBlastDbKey(String projectName, String groupName) {
			this.projectName = projectName;
			this.groupName = groupName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((groupName == null) ? 0 : groupName.hashCode());
			result = prime * result
					+ ((projectName == null) ? 0 : projectName.hashCode());
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
			SequenceGroupBlastDbKey other = (SequenceGroupBlastDbKey) obj;
			if (groupName == null) {
				if (other.groupName != null)
					return false;
			} else if (!groupName.equals(other.groupName))
				return false;
			if (projectName == null) {
				if (other.projectName != null)
					return false;
			} else if (!projectName.equals(other.projectName))
				return false;
			return true;
		}

		@Override
		public SequenceGroupBlastDB createBlastDB() {
			return new SequenceGroupBlastDB(projectName, groupName);
		}
		
	}

	@Override
	protected File getProjectRelativeBlastDbDir(File projectPath) {
		return new File(projectPath, "group_"+groupName);
	}

	@Override
	public String getTitle() {
		return "Sequences of group "+groupName+" in project "+getProjectName();
	}

	@Override
	public long getLastUpdateTime(CommandContext cmdContext) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("group", groupName)) {
			return cmdContext.cmdBuilder(GroupShowLastUpdateTimeCommand.class).execute().getLastUpdateTime();
		}
	}

	@Override
	public InputStream getFastaContentInputStream(CommandContext cmdContext) {
		List<Map<String, Object>> groupMembersMap;
		try(ModeCloser modeCloser = cmdContext.pushCommandMode("group", groupName)) {
			groupMembersMap = cmdContext.cmdBuilder(ListMemberCommand.class).execute().asListOfMaps();
		}
		GlueLogger.getGlueLogger().finest("Building BLAST DB for sequence group "+groupName+", "+groupMembersMap.size()+" sequences");
		return new MySequenceInputStream(new SequenceFastaEnumeration(cmdContext, groupMembersMap));
	}
	
	private class SequenceFastaEnumeration implements Enumeration<InputStream> {

		private Iterator<Map<String, Object>> iterator;
		private CommandContext cmdContext;
		private int sequencesAdded = 0;
		private int totalSequences;
		
		public SequenceFastaEnumeration(CommandContext cmdContext, List<Map<String, Object>> groupMembersMap) {
			this.cmdContext = cmdContext;
			this.totalSequences = groupMembersMap.size();
			this.iterator = groupMembersMap.iterator();
		}
		
		@Override
		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		@Override
		public InputStream nextElement() {
			Map<String, Object> sequenceEntry = iterator.next();
			sequencesAdded++;
			String sourceName = (String) sequenceEntry.get(GroupMember.SOURCE_NAME_PATH);
			String sequenceID = (String) sequenceEntry.get(GroupMember.SEQUENCE_ID_PATH);
			OriginalDataResult originalDataResult;
			try (ModeCloser refSeqMode = cmdContext.pushCommandMode("sequence", sourceName, sequenceID)) {
				originalDataResult = cmdContext.cmdBuilder(ShowOriginalDataCommand.class).execute();
			}
			String fastaString = FastaUtils.seqIdNtsPairToFasta(sourceName+"#"+sequenceID, 
					originalDataResult.getSequenceObject().getNucleotides(cmdContext));
			if(sequencesAdded % 50 == 0 || sequencesAdded == totalSequences) {
				GlueLogger.getGlueLogger().finest("Added "+sequencesAdded+" sequences to BLAST DB for sequence group "+groupName);
			}
			return new ByteArrayInputStream(fastaString.getBytes());

		}
		
	}

	// modded to properly implement available()
	private static class MySequenceInputStream extends InputStream {
	    private Enumeration<? extends InputStream> e;
	    private InputStream in;

	    public MySequenceInputStream(Enumeration<? extends InputStream> e) {
	        this.e = e;
	        try {
	            nextStream();
	        } catch (IOException ex) {
	            // This should never happen
	            throw new Error("panic");
	        }
	    }

	    final void nextStream() throws IOException {
	        if (in != null) {
	            in.close();
	        }

	        if (e.hasMoreElements()) {
	            in = (InputStream) e.nextElement();
	            if (in == null)
	                throw new NullPointerException();
	        }
	        else in = null;

	    }

	    public int available() throws IOException {
	        while(in != null) {
	        	int inAvailable = in.available();
	        	if(inAvailable == 0) {
	        		nextStream();
	        	} else {
	        		return inAvailable;
	        	}
	        }
	        return 0;
	    }

	    public int read() throws IOException {
	        while (in != null) {
	            int c = in.read();
	            if (c != -1) {
	                return c;
	            }
	            nextStream();
	        }
	        return -1;
	    }

	    public int read(byte b[], int off, int len) throws IOException {
	        if (in == null) {
	            return -1;
	        } else if (b == null) {
	            throw new NullPointerException();
	        } else if (off < 0 || len < 0 || len > b.length - off) {
	            throw new IndexOutOfBoundsException();
	        } else if (len == 0) {
	            return 0;
	        }
	        do {
	            int n = in.read(b, off, len);
	            if (n > 0) {
	                return n;
	            }
	            nextStream();
	        } while (in != null);
	        return -1;
	    }

	    public void close() throws IOException {
	        do {
	            nextStream();
	        } while (in != null);
	    }
	}

	

	
}
