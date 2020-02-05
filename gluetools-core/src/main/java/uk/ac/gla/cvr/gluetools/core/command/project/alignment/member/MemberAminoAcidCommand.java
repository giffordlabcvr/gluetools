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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAminoAcidColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate a member sequence to amino acids", 
		docoptUsages = { "-r <relRefName> -f <featureName> [-c <lcStart> <lcEnd>]" },
		docoptOptions = { 
		"-r <relRefName>, --relRefName <relRefName>     Related reference",
		"-f <featureName>, --featureName <featureName>  Feature to translate",
		"-c, --labelledCodon                            Region between codon labels",

		},
		furtherHelp = 
		"If this member is in a constrained alignment, the <relRefName> argument names a reference "+
				"sequence constraining an ancestor alignment of this member's alignment. "+
				"If this member is in an unconstrained alignment, the <relRefName> argument names a reference "+
				"sequence which is a member of the same alignment. "+
		"The <featureName> argument names a feature location which is defined on this reference. "+
		"The result will be confined to this feature location",
		metaTags = {}	
)
public class MemberAminoAcidCommand extends MemberBaseAminoAcidCommand<MemberAminoAcidResult> {

	public static final String LABELLED_CODON = "labelledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";

	private Boolean labelledCodon;
	private String lcStart;
	private String lcEnd;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.labelledCodon = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, LABELLED_CODON, false)).orElse(false);
		this.lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		this.lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		if(labelledCodon && (lcStart == null || lcEnd == null)) {
			usageError1();
		}
		if(!labelledCodon && (lcStart != null || lcEnd != null)) {
			usageError2();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --labelledCodon is used, both <lcStart> and <lcEnd> must be specified");
	}

	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <lcStart> and <lcEnd> may only be used with --labelledCodon");
	}


	@Override
	public MemberAminoAcidResult execute(CommandContext cmdContext) {
		List<LabeledQueryAminoAcid> memberAminoAcids = memberAminoAcids(cmdContext, lookupMember(cmdContext), 
				new SimpleAminoAcidColumnsSelector(getRelRefName(), getFeatureName(), lcStart, lcEnd));
		return new MemberAminoAcidResult(memberAminoAcids);
	}


	@CompleterClass
	public static final class Completer extends FeatureOfRelatedRefCompleter {}

	
}
