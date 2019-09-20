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
package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.fasta.AbstractSequence;

public abstract class BaseFastaCommandResult extends CommandResult {

	protected BaseFastaCommandResult(CommandDocument commandDocument) {
		super(commandDocument);
	}

	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		CommandDocument commandDocument = getCommandDocument();
		Map<String, ? extends AbstractSequence> fastaMap;
		String rootName = commandDocument.getRootName();
		if(rootName.equals(FastaUtils.NUCLEOTIDE_FASTA_DOC_ROOT)) {
			fastaMap = FastaUtils.commandDocumentToNucleotideFastaMap(commandDocument);
		} else if(rootName.equals(FastaUtils.AMINO_ACID_FASTA_DOC_ROOT)) {
			fastaMap = FastaUtils.commandDocumentToProteinFastaMap(commandDocument);
		} else {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Command document with root name "+rootName+" is not a FASTA document");
		}
		renderCtx.output(new String(FastaUtils.mapToFasta(fastaMap, LineFeedStyle.forOS())));
	}
	
}
