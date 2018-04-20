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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.reporting.VariationScanMemberCount;

@CommandClass(
		commandWords={"variation", "frequency"}, 
		description = "Compute variation frequencies within the alignment", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <relRefName> [-m] -f <featureName> [-d] [-v <vWhereClause>]" },
		docoptOptions = { 
		"-c, --recursive                                   Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>     Qualify members",
		"-v <vWhereClause>, --vWhereClause <vWhereClause>  Qualify variations",
		"-r <relRefName>, --relRefName <relRefName>        Related reference",
		"-m, --multiReference                              Scan across references",
		"-f <featureName>, --featureName <featureName>     Feature containing variations",
		"-d, --descendentFeatures                          Include descendent features",
		},
		furtherHelp = 
		"The <relRefName> argument names a reference sequence constraining an ancestor alignment of this alignment (if constrained), "+
		"or simply a reference which is a member of this alignment (if unconstrained). "+
		"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
		"path between this alignment's reference and the ancestor-constraining reference, in the alignment tree. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference. "+
		"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. ",
		metaTags = {}	
)
public class AlignmentVariationFrequencyCommand extends AlignmentModeCommand<AlignmentVariationFrequencyResult> {

	
	public static final String RECURSIVE = AlignmentVariationFrequencyCmdDelegate.RECURSIVE;
	public static final String WHERE_CLAUSE = AlignmentVariationFrequencyCmdDelegate.WHERE_CLAUSE;
	public static final String VARIATION_WHERE_CLAUSE = AlignmentVariationFrequencyCmdDelegate.VARIATION_WHERE_CLAUSE;
	public static final String REL_REF_NAME = AlignmentVariationFrequencyCmdDelegate.REL_REF_NAME;
	public static final String MULTI_REFERENCE = AlignmentVariationFrequencyCmdDelegate.MULTI_REFERENCE;
	public static final String FEATURE_NAME = AlignmentVariationFrequencyCmdDelegate.FEATURE_NAME;
	public static final String DESCENDENT_FEATURES = AlignmentVariationFrequencyCmdDelegate.DESCENDENT_FEATURES;
	
	private AlignmentVariationFrequencyCmdDelegate delegate = new AlignmentVariationFrequencyCmdDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem);
	}
	
	@Override
	public AlignmentVariationFrequencyResult execute(CommandContext cmdContext) {
		String alignmentName = getAlignmentName();
		Map<String, List<VariationScanMemberCount>> almtNameToScanCountList = delegate.execute(alignmentName, false, cmdContext);
		List<VariationScanMemberCount> scanCountList = almtNameToScanCountList.get(alignmentName);
		if(scanCountList == null) {
			scanCountList = new ArrayList<VariationScanMemberCount>();
		}
		return new AlignmentVariationFrequencyResult(scanCountList);
	}

	
	@CompleterClass
	public static final class Completer extends FeatureOfRelatedRefCompleter {}

	
}
