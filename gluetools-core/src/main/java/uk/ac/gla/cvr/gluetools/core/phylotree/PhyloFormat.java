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
