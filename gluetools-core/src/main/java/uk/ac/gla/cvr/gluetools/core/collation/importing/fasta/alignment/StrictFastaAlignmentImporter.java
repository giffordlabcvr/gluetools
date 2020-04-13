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
package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.StrictFastaAlignmentImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@PluginClass(elemName="strictFastaAlignmentImporter",
description="Strict import of a nucleotide alignment from a FASTA file")
public class StrictFastaAlignmentImporter extends BaseFastaAlignmentImporter<StrictFastaAlignmentImporter> {

	
	public StrictFastaAlignmentImporter() {
		super();
		registerModulePluginCmdClass(StrictFastaAlignmentImporterImportCommand.class);
		registerModulePluginCmdClass(StrictFastaAlignmentImporterPreviewCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}

	public FastaAlignmentImporterResult doPreview(ConsoleCommandContext cmdContext, String fileName, String sourceName) {
		return doImport(cmdContext, fileName, null, sourceName);
	}

	public final FastaAlignmentImporterResult doImport(ConsoleCommandContext cmdContext, String fileName, 
			Alignment alignment, String sourceName) {

		byte[] fastaFileBytes = cmdContext.loadBytes(fileName);
		
		Map<String, DNASequence> sequenceMap = FastaUtils.parseFasta(fastaFileBytes);
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		int alignmentRows = 0;
		
		for(Map.Entry<String, DNASequence> entry: sequenceMap.entrySet()) {
			String fastaID = entry.getKey();

			Sequence foundSequence = findSequence(cmdContext, fastaID, sourceName);
			if(foundSequence == null) {
				continue;
			}
			
			String memberSourceName = foundSequence.getSource().getName();
			String memberSequenceID = foundSequence.getSequenceID();

			this.log(Level.FINEST, "Fasta ID "+fastaID+" was mapped to sequence "+memberSourceName+"/"+memberSequenceID);

			AlignmentMember almtMember = null; 

			List<QueryAlignedSegment> queryAlignedSegs = null; 

			DNASequence alignmentRowDnaSequence = entry.getValue();
			String alignmentRowAsString = alignmentRowDnaSequence.getSequenceAsString();
			String queryId = foundSequence.getSource().getName()+"/"+foundSequence.getSequenceID();
			AbstractSequenceObject foundSeqObj = foundSequence.getSequenceObject();
			String queryNucleotides = foundSeqObj.getNucleotides(cmdContext);
			queryAlignedSegs = strictAlignmentRowImport(queryId, queryNucleotides, alignmentRowAsString);

			if(alignment != null) { // null == preview
				for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
					AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, 
							AlignedSegment.pkMap(alignment.getName(), memberSourceName, memberSequenceID, 
									queryAlignedSeg.getRefStart(), queryAlignedSeg.getRefEnd(), 
									queryAlignedSeg.getQueryStart(), queryAlignedSeg.getQueryEnd()), false);
					alignedSegment.setAlignmentMember(almtMember);
				}
			}
			
			Map<String, Object> memberResultMap = new LinkedHashMap<String, Object>();
			memberResultMap.put("fastaID", fastaID);
			memberResultMap.put("sourceName", memberSourceName);
			memberResultMap.put("sequenceID", memberSequenceID);
			memberResultMap.put("numSegmentsAdded", new Integer(queryAlignedSegs.size()));
			resultListOfMaps.add(memberResultMap);
			alignmentRows++;
			if(alignmentRows % 25 == 0) {
				log("Imported "+alignmentRows+" alignment rows");
			}
			
		}
		log("Imported "+alignmentRows+" alignment rows");
		
		cmdContext.commit();
		return new FastaAlignmentImporterResult(resultListOfMaps);
	}

	@Override
	public List<QueryAlignedSegment> alignmentRowImport(CommandContext cmdContext, String queryId, String queryNucleotides, String alignmentRow) {
		return strictAlignmentRowImport(queryId, queryNucleotides, alignmentRow);
	}

	private List<QueryAlignedSegment> strictAlignmentRowImport(String queryId, String queryNucleotides, String alignmentRow) {
		List<QueryAlignedSegment> qaSegs = new ArrayList<QueryAlignedSegment>();
		QueryAlignedSegment currentQaSeg = null;
		int queryLoc = 1;
		char queryNt = FastaUtils.nt(queryNucleotides, queryLoc);
		boolean queryExhausted = false;
		for(int uLoc = 1; uLoc <= alignmentRow.length(); uLoc++) {
			char uNt = FastaUtils.nt(alignmentRow, uLoc);
			if(uNt == '-') {
				if(currentQaSeg != null) {
					qaSegs.add(currentQaSeg);
					currentQaSeg = null;
				}
			} else {
				if(queryExhausted) {
					throw new StrictFastaAlignmentImporterException(Code.STRICT_IMPORT_ERROR, 
							"query "+queryId+" is exhausted but alignment row still has non-gap character at position "+uLoc);
				}
				if(uNt == queryNt) {
					if(currentQaSeg == null) {
						currentQaSeg = new QueryAlignedSegment(uLoc, uLoc, queryLoc, queryLoc);
					} else {
						currentQaSeg.setQueryEnd(queryLoc);
						currentQaSeg.setRefEnd(uLoc);
					}
					queryLoc++;
					if(queryLoc > queryNucleotides.length()) {
						queryExhausted = true;
					} else {
						queryNt = FastaUtils.nt(queryNucleotides, queryLoc);
					}
				} else {
					throw new StrictFastaAlignmentImporterException(Code.STRICT_IMPORT_ERROR, 
							"mismatch between query "+queryId+" char "+queryNt+" at location "+queryLoc+" and alignment row char "+uNt+" at position "+uLoc);
				}
			}
		}
		if(currentQaSeg != null) {
			qaSegs.add(currentQaSeg);
		}
		if(!queryExhausted) {
			throw new StrictFastaAlignmentImporterException(Code.STRICT_IMPORT_ERROR, 
					"query "+queryId+" is not exhausted at end of alignment row");
		}
		return qaSegs;
	}

	
}
