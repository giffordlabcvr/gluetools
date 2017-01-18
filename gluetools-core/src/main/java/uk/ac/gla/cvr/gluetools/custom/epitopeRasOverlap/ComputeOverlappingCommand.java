package uk.ac.gla.cvr.gluetools.custom.epitopeRasOverlap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegmentTree;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@CommandClass(
		commandWords={"compute", "overlapping"}, 
		description = "Compute which RAS variations overlap which Epitope variations", 
		docoptUsages = { ""},
		docoptOptions = {},
		furtherHelp = "",
		metaTags = {}	
)
public class ComputeOverlappingCommand extends ModulePluginCommand<ComputeOverlappingCommand.Result, EpitopeRasOverlap> {

	private Set<IncludedRASLocation> includedRasLocations = new LinkedHashSet<IncludedRASLocation>();
	
	public ComputeOverlappingCommand() {
		super();
		addIncluded();
	}

	@Override
	protected Result execute(CommandContext cmdContext, EpitopeRasOverlap modulePlugin) {
		Expression rasExp = ExpressionFactory.matchExp("is_resistance_associated_variant", Boolean.TRUE)
				.andExp(ExpressionFactory.matchExp("featureLoc.referenceSequence.name", "H77_AF009606"));
		SelectQuery rasQuery = new SelectQuery(Variation.class, rasExp); 
		List<Variation> rasList = GlueDataObject.query(cmdContext, Variation.class, rasQuery);

		Expression epitopeExp = ExpressionFactory.matchExp("is_epitope", Boolean.TRUE)
				.andExp(ExpressionFactory.notLikeExp("epitope_full_hla", "%undetermined%"))
				.andExp(ExpressionFactory.noMatchExp("epitope_full_hla", null))
				.andExp(ExpressionFactory.matchExp("featureLoc.referenceSequence.name", "H77_AF009606"));
		SelectQuery epitopeQuery = new SelectQuery(Variation.class, epitopeExp); 
		List<Variation> epitopeList = GlueDataObject.query(cmdContext, Variation.class, epitopeQuery);

		ReferenceSegmentTree<EpitopeSegment> epitopeSegTree = new ReferenceSegmentTree<ComputeOverlappingCommand.EpitopeSegment>(
				new Comparator<EpitopeSegment>() {
					public int compare(EpitopeSegment o1, EpitopeSegment o2) {
						int comp = Integer.compare(
								Integer.parseInt((String) o1.epitope.readProperty("epitope_start_codon")), 
								Integer.parseInt((String) o2.epitope.readProperty("epitope_start_codon")));
						if(comp != 0) {
							return comp;
						}
						return o1.epitope.getName().compareTo(o2.epitope.getName());
					};
				}
		);
		
		for(Variation epitope : epitopeList) {
			epitopeSegTree.add(new EpitopeSegment(epitope));
		}
		
		
		Set<Variation> overlappingRasSet = new LinkedHashSet<Variation>();
		Set<Variation> overlappingEpitopeSet = new LinkedHashSet<Variation>();
		
		List<Overlap> overlaps = new ArrayList<Overlap>();

		FeatureLocation precursorPolyprotein = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap("H77_AF009606", "precursor_polyprotein"));
		int codon1Start = precursorPolyprotein.getCodon1Start(cmdContext);
		
		for(Variation ras: rasList) {
			if(ras.getPatternLocs().size() != 1) {
				continue;
			}
			Integer firstCodon = (Integer) ras.readProperty("rav_first_codon");
			String gene = ras.getFeatureLoc().getFeature().getName();
			if(!includedRasLocations.contains(new IncludedRASLocation(gene, firstCodon))) {
				continue;
			}
			
			int refStart = ras.getPatternLocs().get(0).getRefStart();
			int refEnd = ras.getPatternLocs().get(0).getRefEnd();
			List<EpitopeSegment> overlappingEpSegs = new ArrayList<EpitopeSegment>();
			epitopeSegTree.findOverlapping(refStart, refEnd, overlappingEpSegs);
			for(EpitopeSegment epSeg: overlappingEpSegs) {
				Overlap overlap = new Overlap();
				overlap.epitope = epSeg.epitope;
				overlap.ras = ras;
				overlap.rasPpCodon = TranslationUtils.getCodon(codon1Start, refStart);
				
				overlaps.add(overlap);
				overlappingRasSet.add(ras); 
				overlappingEpitopeSet.add(epSeg.epitope); 
			}
		}
		GlueLogger.log(Level.INFO, "RASs involved in overlaps: "+overlappingRasSet.size());
		GlueLogger.log(Level.INFO, "Epitopes involved in overlaps: "+overlappingEpitopeSet.size());
		return new Result(overlaps);
	}

	private class EpitopeSegment extends ReferenceSegment {
		private Variation epitope;
		
		private EpitopeSegment(Variation epitope) {
			super(epitope.getPatternLocs().get(0).getRefStart(), epitope.getPatternLocs().get(0).getRefEnd());
			this.epitope = epitope;
		}
	}

	public static class Overlap {
		Variation ras;
		int rasPpCodon;
		Variation epitope;
	}
	
	public static class Result extends BaseTableResult<ComputeOverlappingCommand.Overlap> {

		public Result(List<Overlap> rowObjects) {
			super("overlappingResult", rowObjects, 
					column("rasGene", ov -> ov.ras.getFeatureLoc().getFeature().getName()), 
					column("rasSubstitution", ov -> ov.ras.readProperty("rav_substitutions")),
					column("rasPpCodon", ov -> ov.rasPpCodon),
					column("epitopeID", ov -> ov.epitope.getName()), 
					column("epitopeStartCodon", ov -> ov.epitope.readProperty("epitope_start_codon")), 
					column("epitopeEndCodon", ov -> ov.epitope.readProperty("epitope_end_codon")),
					column("epitopeHLA", ov -> ov.epitope.readProperty("epitope_full_hla")));
		}
		
	}

	public static class IncludedRASLocation {
		String gene;
		Integer codon;
		public IncludedRASLocation(String gene, Integer codon) {
			super();
			this.gene = gene;
			this.codon = codon;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((codon == null) ? 0 : codon.hashCode());
			result = prime * result + ((gene == null) ? 0 : gene.hashCode());
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
			IncludedRASLocation other = (IncludedRASLocation) obj;
			if (codon == null) {
				if (other.codon != null)
					return false;
			} else if (!codon.equals(other.codon))
				return false;
			if (gene == null) {
				if (other.gene != null)
					return false;
			} else if (!gene.equals(other.gene))
				return false;
			return true;
		}
		
	}
	
	private void included(String gene, Integer codon) {
		includedRasLocations.add(new IncludedRASLocation(gene, codon));
	}
	
	private void addIncluded() {
		included("NS3", 36);
		included("NS3", 41);
		included("NS3", 43);
		included("NS3", 54);
		included("NS3", 55);
		included("NS3", 80);
		included("NS3", 86);
		included("NS3", 109);
		included("NS3", 155);
		included("NS3", 156);
		included("NS3", 168);
		included("NS3", 170);
		included("NS3", 176);
		included("NS5A", 28);
		included("NS5A", 30);
		included("NS5A", 31);
		included("NS5A", 32);
		included("NS5A", 58);
		included("NS5A", 93);
		included("NS5B", 282);
		included("NS5B", 316);
		included("NS5B", 320);
		included("NS5B", 321);
	}
	
}
