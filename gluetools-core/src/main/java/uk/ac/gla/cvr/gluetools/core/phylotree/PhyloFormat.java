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
package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.io.StringWriter;

import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.newick.NewickBootstrapsToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.newick.NewickJPlaceToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.newick.NewickToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickBootstrapsGenerator;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickGenerator;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickJPlaceGenerator;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.DocumentToPhyloTreeTransformer;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.PhyloTreeToDocumentTransformer;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentJsonUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public enum PhyloFormat {

	GLUE_XML {
		@Override
		public PhyloTree parse(byte[] bytes) {
			Document xmlDocument = GlueXmlUtils.documentFromBytes(bytes);
			CommandDocument cmdDocument = CommandDocumentXmlUtils.xmlDocumentToCommandDocument(xmlDocument);
			DocumentToPhyloTreeTransformer documentToPhyloTreeTransformer = new DocumentToPhyloTreeTransformer();
			cmdDocument.accept(documentToPhyloTreeTransformer);
			return documentToPhyloTreeTransformer.getPhyloTree();
		}

		@Override
		public byte[] generate(PhyloTree phyloTree) {
			PhyloTreeToDocumentTransformer phyloTreeToDocumentTransformer = new PhyloTreeToDocumentTransformer();
			phyloTree.accept(phyloTreeToDocumentTransformer);
			CommandDocument cmdDocument = phyloTreeToDocumentTransformer.getDocument();
			Document xmlDocument = CommandDocumentXmlUtils.commandDocumentToXmlDocument(cmdDocument);
			return GlueXmlUtils.prettyPrint(xmlDocument);
		}
	},
	GLUE_JSON {
		@Override
		public PhyloTree parse(byte[] bytes) {
			JsonObject jsonObj = JsonUtils.stringToJsonObject(new String(bytes));
			CommandDocument cmdDocument = CommandDocumentJsonUtils.jsonObjectToCommandDocument(jsonObj);
			DocumentToPhyloTreeTransformer documentToPhyloTreeTransformer = new DocumentToPhyloTreeTransformer();
			cmdDocument.accept(documentToPhyloTreeTransformer);
			return documentToPhyloTreeTransformer.getPhyloTree();
			
		}

		@Override
		public byte[] generate(PhyloTree phyloTree) {
			PhyloTreeToDocumentTransformer phyloTreeToDocumentTransformer = new PhyloTreeToDocumentTransformer();
			phyloTree.accept(phyloTreeToDocumentTransformer);
			CommandDocument cmdDocument = phyloTreeToDocumentTransformer.getDocument();
			StringWriter stringWriter = new StringWriter();
			JsonGenerator jsonGenerator = JsonUtils.jsonGenerator(stringWriter);
			CommandDocumentJsonUtils.commandDocumentGenerateJson(jsonGenerator, cmdDocument);
			jsonGenerator.flush();
			return stringWriter.toString().getBytes();
		}
	},
	// Newick format, allowing the following constructs:
	// -- optional internal name, e.g. X, set as "name" property on PhyloTree internal node
	// -- optional leaf name, e.g. X, set as "name" property on PhyloTree leaf node
	// -- optional branch length, e.g. :0.10334, signified by a colon, set as "length" property on PhyloTree branches
	// -- optional branch labels e.g. {123}, comes after branch length, set as "label" property on PhyloTree branches
	// -- optional branch comments e.g. [xyz234098], comes after branch length, set as "comment" property on PhyloTree branches
	NEWICK {
		@Override
		public PhyloTree parse(byte[] bytes) {
			NewickToPhyloTreeParser newickToPhyloTreeParser = new NewickToPhyloTreeParser();
			return newickToPhyloTreeParser.parseNewick(new String(bytes));
		}

		@Override
		public byte[] generate(PhyloTree phyloTree) {
			PhyloTreeToNewickGenerator phyloTreeToNewickGenerator = new PhyloTreeToNewickGenerator();
			phyloTree.accept(phyloTreeToNewickGenerator);
			return phyloTreeToNewickGenerator.getNewickString().getBytes();
		}
	},
	// as NEWICK, but any internal name string is not set as a "name" property on PhyloTree internal nodes, but as a 
	// "bootstrap" property on the internal node's parent branch. This is expected to be an integer between 0 and 100.
	// NEWICK_BOOTSTRAPS is produced by RAxML 8.x in the RAxML_bipartitions file. 
	// NEWICK_BOOTSTRAPS is also the input format for ClusterPicker.
	// FigTree will allow you to import NEWICK_BOOTSTRAPS and prompt you to name the node property.
	NEWICK_BOOTSTRAPS {
		@Override
		public PhyloTree parse(byte[] bytes) {
			NewickBootstrapsToPhyloTreeParser newickBootstrapsToPhyloTreeParser = new NewickBootstrapsToPhyloTreeParser();
			return newickBootstrapsToPhyloTreeParser.parseNewick(new String(bytes));
		}

		@Override
		public byte[] generate(PhyloTree phyloTree) {
			PhyloTreeToNewickBootstrapsGenerator phyloTreeToNewickBootstrapsGenerator = new PhyloTreeToNewickBootstrapsGenerator();
			phyloTree.accept(phyloTreeToNewickBootstrapsGenerator);
			return phyloTreeToNewickBootstrapsGenerator.getNewickString().getBytes();
		}
	},
	// as NEWICK, but expects each branch to have an integer branch label. These are stored using the key "jPlaceBranchLabel"
	// in PhyloTree branches.
	// This variant is specified in the jPlace standard:
	// https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0031009
	// and is produced by RAxML-EPA
	NEWICK_JPLACE {
		@Override
		public PhyloTree parse(byte[] bytes) {
			NewickJPlaceToPhyloTreeParser newickJPlaceToPhyloTreeParser = new NewickJPlaceToPhyloTreeParser();
			return newickJPlaceToPhyloTreeParser.parseNewick(new String(bytes));
		}

		@Override
		public byte[] generate(PhyloTree phyloTree) {
			PhyloTreeToNewickJPlaceGenerator phyloTreeToNewickJPlaceGenerator = new PhyloTreeToNewickJPlaceGenerator();
			phyloTree.accept(phyloTreeToNewickJPlaceGenerator);
			return phyloTreeToNewickJPlaceGenerator.getNewickString().getBytes();
		}
	};
	
	public abstract PhyloTree parse(byte[] bytes);

	public abstract byte[] generate(PhyloTree phyloTree);

}
