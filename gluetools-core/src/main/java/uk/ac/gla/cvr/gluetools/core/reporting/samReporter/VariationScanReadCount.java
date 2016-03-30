package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

public class VariationScanReadCount {

	private Variation variation;
	private int readsWherePresent;
	private double pctWherePresent;
	private int readsWhereAbsent;
	private double pctWhereAbsent;
	
	public VariationScanReadCount(Variation variation, int readsWherePresent,
			double pctWherePresent, int readsWhereAbsent, double pctWhereAbsent) {
		super();
		this.variation = variation;
		this.readsWherePresent = readsWherePresent;
		this.pctWherePresent = pctWherePresent;
		this.readsWhereAbsent = readsWhereAbsent;
		this.pctWhereAbsent = pctWhereAbsent;
	}
	
	public Variation getVariation() {
		return variation;
	}
	public int getReadsWherePresent() {
		return readsWherePresent;
	}
	public double getPctWherePresent() {
		return pctWherePresent;
	}
	public int getReadsWhereAbsent() {
		return readsWhereAbsent;
	}
	public double getPctWhereAbsent() {
		return pctWhereAbsent;
	}
	
	public static void sortVariationScanReadCounts(List<VariationScanReadCount> variationScanReadCounts) {
		Comparator<VariationScanReadCount> comparator = new Comparator<VariationScanReadCount>(){
			@Override
			public int compare(VariationScanReadCount o1, VariationScanReadCount o2) {
				int comp = o1.getVariation().getFeatureLoc().getReferenceSequence().getName().compareTo(o2.getVariation().getFeatureLoc().getReferenceSequence().getName());
				if(comp == 0) {
					comp = Integer.compare(o1.getVariation().getRefStart(), o2.getVariation().getRefStart());
				}
				if(comp == 0) {
					comp = o1.getVariation().getName().compareTo(o2.getVariation().getName());
				}
				return comp;
			}
		};
		Collections.sort(variationScanReadCounts, comparator);
	}

	
	
}
