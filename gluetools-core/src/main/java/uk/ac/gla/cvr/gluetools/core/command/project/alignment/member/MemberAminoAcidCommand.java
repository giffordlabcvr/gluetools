package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate a member sequence to amino acids", 
		docoptUsages = { "-r <acRefName> -f <featureName>" },
		docoptOptions = { 
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>  Feature to translate",
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this member's alignment. "+
		"The <featureName> argument names a feature location which is defined on this reference. "+
		"The result will be confined to this feature location",
		metaTags = {}	
)
public class MemberAminoAcidCommand extends MemberBaseAminoAcidCommand<MemberAminoAcidResult> {

	@Override
	public MemberAminoAcidResult execute(CommandContext cmdContext) {
		return new MemberAminoAcidResult(getMemberAminoAcids(cmdContext));
	}


	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
