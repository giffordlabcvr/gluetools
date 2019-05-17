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
package uk.ac.gla.cvr.gluetools.core.blastRotator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.blastRotator.BlastSequenceRotatorException.Code;
import uk.ac.gla.cvr.gluetools.core.blastRotator.RotationResultRow.Status;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHspFilter;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.MultiReferenceBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="blastSequenceRotator",
		description="Identifies circular sequences requiring rotation using nucleotide BLAST against a ReferenceSequence")
public class BlastSequenceRotator extends ModulePlugin<BlastSequenceRotator> {

	private static final String REFERENCE_SEQUENCE = "referenceSequence";
	private static final String BLAST_RUNNER = "blastRunner";
	private static final String MINIMUM_BIT_SCORE = "minimumBitScore";
	private static final String MINIMUM_SEGMENT_LENGTH = "minimumSegmentLength";
	
	private BlastRunner blastRunner = new BlastRunner();
	private String refSeqName;
	private Optional<Double> minimumBitScore;
	private Integer minimumSegmentLength;
	
	public BlastSequenceRotator() {
		super();
		registerModulePluginCmdClass(RotateSequenceCommand.class);
		registerModulePluginCmdClass(RotateFileCommand.class);
		registerModulePluginCmdClass(RotateFastaDocumentCommand.class);
		addSimplePropertyName(MINIMUM_BIT_SCORE);
		addSimplePropertyName(MINIMUM_SEGMENT_LENGTH);
		addSimplePropertyName(REFERENCE_SEQUENCE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, BLAST_RUNNER);
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
		this.refSeqName = PluginUtils.configureStringProperty(configElem, REFERENCE_SEQUENCE, true);
		this.minimumBitScore = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MINIMUM_BIT_SCORE, false));
		this.minimumSegmentLength = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MINIMUM_SEGMENT_LENGTH, false)).orElse(10);

	}

	@Override
	public void init(CommandContext cmdContext) {
		super.init(cmdContext);
		BlastDbManager.getInstance().removeMultiRefBlastDB(cmdContext, dbName());
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName), true);
		if(refSequence == null) {
			throw new BlastSequenceRotatorException(Code.NO_SUCH_REFERENCE_SEQUENCE, refSeqName);
		}
	}

	public Map<String, RotationResultRow> rotate(CommandContext cmdContext, Map<String, DNASequence> queries) {
		Map<String, RotationResultRow> queryIdToRotationResult = new LinkedHashMap<String, RotationResultRow>();
		Set<String> refNamesSet = new LinkedHashSet<String>();
		refNamesSet.add(refSeqName);
		
		MultiReferenceBlastDB multiReferenceDB = BlastDbManager.getInstance().ensureMultiReferenceDB(cmdContext, dbName(), refNamesSet);
		GlueLogger.getGlueLogger().finest("Executing BLAST");

		List<BlastResult> blastResults = blastRunner.executeBlast(cmdContext, BlastRunner.BlastType.BLASTN, multiReferenceDB, 
				FastaUtils.mapToFasta(queries, LineFeedStyle.forOS()));

		BlastHspFilter hspFilter = initHspFilter();
		Map<String, List<QueryAlignedSegment>> queryIdToQaSegs = BlastUtils.blastNResultsToAlignedSegmentsMap(refSeqName, blastResults, hspFilter, false);
		
		queryIdToQaSegs.forEach( (queryId, qaSegs) -> {
			RotationResultRow rotationResultRow;
			if(qaSegs.isEmpty()) {
				rotationResultRow = new RotationResultRow(queryId, Status.NO_ACCEPTABLE_HSPS, null);
			} else {
				rotationResultRow = qaSegsToRotationResult(queryId, queries.get(queryId).getSequenceAsString().length(), qaSegs);
			}
			queryIdToRotationResult.put(queryId, rotationResultRow);
		} );
		
		return queryIdToRotationResult;
	}

	private RotationResultRow qaSegsToRotationResult(String queryId, int sequenceLength, List<QueryAlignedSegment> qaSegs) {
		qaSegs.sort(new Comparator<QueryAlignedSegment>(){
			@Override
			public int compare(QueryAlignedSegment o1, QueryAlignedSegment o2) {
				int comp;
				comp = Integer.compare(o1.getQueryStart(), o2.getQueryStart());
				if(comp != 0) {return comp;}
				comp = Integer.compare(o1.getQueryEnd(), o2.getQueryEnd());
				return 0;
			}
		});

		RotationResultRow.Status status = Status.NO_ROTATION_NECESSARY;
		Integer rotationNts = null;
		
		QueryAlignedSegment lastQaSeg = null;
		
		for(QueryAlignedSegment qaSeg: qaSegs) {
			GlueLogger.log(Level.FINEST, "Rotation processing qaSeg: "+qaSeg.toString());
			if(qaSeg.getCurrentLength() < minimumSegmentLength) {
				GlueLogger.log(Level.FINEST, "Rotation ignored short qaSeg: "+qaSeg.toString());
				continue;
			}
			if(status == Status.NO_ROTATION_NECESSARY) {
				if(lastQaSeg != null) {
					if(qaSeg.getRefStart() < lastQaSeg.getRefStart()) {
						status = Status.ROTATION_NECESSARY;
						GlueLogger.log(Level.FINEST, "Rotation identified cut point at qaSeg: "+qaSeg.toString());
						rotationNts = sequenceLength-(qaSeg.getQueryStart() - 1);
					}
				}
			} 
			lastQaSeg = qaSeg;
		}
		return new RotationResultRow(queryId, status, rotationNts);
	}

	private BlastHspFilter initHspFilter() {
		return new BlastHspFilter() {
			// hsp must be forward direction and meet any minimum bit score
			@Override
			public boolean allowBlastHsp(BlastHsp blastHsp) {
				boolean hspAccepted = blastHsp.getQueryTo() >= blastHsp.getQueryFrom() &&
						blastHsp.getHitTo() >= blastHsp.getHitFrom() &&
						minimumBitScore.map(minBitScore -> (minBitScore <= blastHsp.getBitScore())).orElse(true);
				if(hspAccepted) {
					GlueLogger.log(Level.FINEST, "Rotator accepted HSP "+blastHsp);
				} else {
					GlueLogger.log(Level.FINEST, "Rotator rejected HSP "+blastHsp);
				}
				return hspAccepted;			}
		};
	}

	private String dbName() {
		return "blastSequenceRotator_"+getModuleName();
	}
	
		
	
}
