package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@CommandClass(
		commandWords={"count", "amino-acid"}, 
		description = "Count occurrences of specific amino-acid value in a member feature", 
		docoptUsages = { "-r <acRefName> -f <featureName> <aminoAcid>" },
		docoptOptions = { 
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>  Feature to translate",
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this member's alignment. "+
		"The <featureName> argument names a feature location which is defined on this reference. "+
		"The result will be confined to this feature location. "+
		"The command outputs the number of occurrences of <aminoAcid> in the translation.",
		metaTags = {}	
)
public class MemberCountAminoAcidCommand extends MemberBaseAminoAcidCommand<MemberCountAminoAcidResult> {

	public static final String AMINO_ACID = "aminoAcid";

	private char aminoAcid;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.aminoAcid = PluginUtils.configureCharProperty(configElem, AMINO_ACID, true);
		if(!TranslationUtils.isAminoAcid(aminoAcid)) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Character "+new String(new char[]{aminoAcid})+" is not an amino acid");
		}
	}

	@Override
	public MemberCountAminoAcidResult execute(CommandContext cmdContext) {
		List<LabeledQueryAminoAcid> memberAminoAcids = super.getMemberAminoAcids(cmdContext);
		int count = 0;
		for(LabeledQueryAminoAcid lqaa : memberAminoAcids) {
			if(lqaa.getLabeledAminoAcid().getAminoAcid().charAt(0) == aminoAcid) {
				count++;
			}
		}
		return new MemberCountAminoAcidResult(new String(new char[]{aminoAcid}), count);
	}


	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
