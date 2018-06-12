package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;|
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public class GbFeatureSpecification {

	private List<GbFeatureInterval> intervals;
	
	private String featureKey;
	
	private Map<String, String> qualifierKeyValues;

	public GbFeatureSpecification(List<GbFeatureInterval> intervals,
			String featureKey, Map<String, String> qualifierKeyValues) {
		super();
		this.intervals = intervals;
		this.featureKey = featureKey;
		this.qualifierKeyValues = qualifierKeyValues;
	}

	public List<GbFeatureInterval> getIntervals() {
		return intervals;
	}

	public String getFeatureKey() {
		return featureKey;
	}

	public Map<String, String> getQualifierKeyValues() {
		return qualifierKeyValues;
	}

	public static void writeFeatureTableToStream(OutputStream outputStream,
			String sequenceID, List<GbFeatureSpecification> gbFeatureSpecifications) {
		Writer writer = new PrintWriter(outputStream);
		try {
			String linebreakChars = LineFeedStyle.forOS().getLineBreakChars();
			writer.write(">Feature "+sequenceID+linebreakChars);
			writer.flush();
			for(GbFeatureSpecification gbFeatureSpecification: gbFeatureSpecifications) {
				String featureKey = gbFeatureSpecification.getFeatureKey();
				List<GbFeatureInterval> intervals = gbFeatureSpecification.getIntervals();
				for(int i = 0; i < intervals.size(); i++) {
					GbFeatureInterval interval = intervals.get(i);
					writer.write(interval.getStartString());
					writer.write("\t");
					writer.write(interval.getEndString());
					if(i == 0) {
						writer.write("\t");
						writer.write(featureKey);
					}
					writer.write(linebreakChars);
					writer.flush();
				}
				Map<String, String> qualifierKeyValues = gbFeatureSpecification.getQualifierKeyValues();
				for(Map.Entry<String,String> entry: qualifierKeyValues.entrySet()) {
					writer.write("\t");
					writer.write("\t");
					writer.write("\t");
					writer.write(entry.getKey());
					writer.write("\t");
					writer.write(entry.getValue());
					writer.write(linebreakChars);
					writer.flush();
				}
			}
			
		} catch (IOException ioe) {
			GlueLogger.getGlueLogger().warning("IOException whilst writing feature table: "+ioe.getLocalizedMessage());
		} finally {
			try { writer.close(); }  catch(IOException ioe) {
				GlueLogger.getGlueLogger().warning("IOException whilst closing writer: "+ioe.getLocalizedMessage());
			}
		}
		
	}
	
	
	
}
