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
package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;


@CommandClass( 
	commandWords={"show", "nucleotides"}, 
	docoptUsages={"[-b <beginIndex>] [-e <endIndex>]"},
	docoptOptions={
		"-b <idx>, --beginIndex <idx>  Start index of the subsequence (from 1, inclusive)",
		"-e <idx>, --endIndex <idx>    End index of the subsequence (inclusive)"},
	description="Show nucleotides from the sequence",
	furtherHelp="A subsequence is returned using nucleotide codes in FASTA format. "+
	"If the beginIndex is omitted the subsequence starts at the beginning of the sequence. "+
			"Similarly if the endIndex is omitted, the subsequence starts at the end of the sequence. "+
			"If both startIndex and endIndex are provided, then startIndex may be greater than endIndex. "+
			"In this case the nucleotides are returned in descending order.") 
public class ShowNucleotidesCommand extends SequenceModeCommand<NucleotidesResult> {

	public static final String END_INDEX = "endIndex";
	public static final String BEGIN_INDEX = "beginIndex";
	private Integer beginIndex;
	private Optional<Integer> endIndex;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		beginIndex = PluginUtils.configureIntProperty(configElem, BEGIN_INDEX, 1);
		endIndex = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, END_INDEX, false));
	}

	@Override
	public NucleotidesResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext);
		String nucleotides = sequence.getSequenceObject().getNucleotides(cmdContext);
		int end = endIndex.orElse(nucleotides.length());
		return new NucleotidesResult(beginIndex, end, SegmentUtils.base1SubString(nucleotides, beginIndex, end));
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}

}
