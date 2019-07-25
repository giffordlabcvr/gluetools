package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.translation.AmbigNtTripletInfo;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public class SimpleLabeledCodon extends LabeledCodon {

	private int ntMiddle;
	
	public SimpleLabeledCodon(String featureName, String codonLabel, int ntStart, int ntMiddle, int ntEnd,
			int transcriptionIndex) {
		super(featureName, codonLabel, Arrays.asList(ntStart, ntMiddle, ntEnd), transcriptionIndex);
		this.ntMiddle = ntMiddle;
	}

	public int getNtMiddle() {
		return ntMiddle;
	}

	public LabeledQueryAminoAcid translate(CommandContext cmdContext, Translator translator, List<LabeledCodonQueryAlignedSegment> codonLcQaSegs,
			NucleotideContentProvider queryNucleotideContent) {
		char[] nts = new char[3];
		Integer queryNtStart = null;
		Integer queryNtMiddle = null;
		Integer queryNtEnd = null;
		for(LabeledCodonQueryAlignedSegment lcQaSeg : codonLcQaSegs) {
			for(int i = 0; i < lcQaSeg.getCurrentLength(); i++) {
				if(lcQaSeg.getRefStart()+i == getNtStart()) {
					queryNtStart = lcQaSeg.getQueryStart()+i;
					nts[0] = queryNucleotideContent.nt(cmdContext, queryNtStart);
				} else if(lcQaSeg.getRefStart()+i == getNtMiddle()) {
					queryNtMiddle = lcQaSeg.getQueryStart()+i;
					nts[1] = queryNucleotideContent.nt(cmdContext, queryNtMiddle);
				} else if(lcQaSeg.getRefStart()+i == getNtEnd()) {
					queryNtEnd = lcQaSeg.getQueryStart()+i;
					nts[2] = queryNucleotideContent.nt(cmdContext, queryNtEnd);
				} 
			}
		}
		String ntsToTranslate = new String(nts);
		AmbigNtTripletInfo ambigNtTripletInfo = translator.translate(ntsToTranslate).get(0);
		LabeledAminoAcid labeledAminoAcid = new LabeledAminoAcid(this, ambigNtTripletInfo);
		return new LabeledQueryAminoAcid(labeledAminoAcid, Arrays.asList(queryNtStart, queryNtMiddle, queryNtEnd));
	}
}
