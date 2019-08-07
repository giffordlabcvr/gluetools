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
package uk.ac.gla.cvr.gluetools.programs.blast;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class BlastResultBuilder {

	public static BlastResult blastResultFromDocument(Document blastDoc) {
		
		// GlueLogger.getGlueLogger().info(new String(GlueXmlUtils.prettyPrint(blastDoc)));
		
		BlastResult blastResult = new BlastResult();
		Element searchElem = getPathElement(blastDoc, "BlastOutput2", "report", "Report", "results", "Results", "search", "Search");
		blastResult.setQueryFastaId(getPathElemText(searchElem, "query-title"));
		List<Element> hitElems = getPathElements(searchElem, "hits", "Hit");
		for(Element hitElem: hitElems) {
			BlastHit blastHit = new BlastHit(blastResult);
			blastResult.addHit(blastHit);
			blastHit.setReferenceName(getPathElemText(hitElem, "description", "HitDescr", "title"));
			List<Element> hspElems = getPathElements(hitElem, "hsps", "Hsp");
			for(Element hspElem : hspElems) {
				BlastHsp blastHsp = new BlastHsp(blastHit);
				blastHit.addHsp(blastHsp);
				blastHsp.setBitScore(getPathElemDouble(hspElem, "bit-score"));
				blastHsp.setScore(getPathElemInt(hspElem, "score"));
				blastHsp.setEvalue(getPathElemDouble(hspElem, "evalue"));
				blastHsp.setIdentity(getPathElemInt(hspElem, "identity"));
				blastHsp.setQueryFrom(getPathElemInt(hspElem, "query-from"));
				blastHsp.setQueryTo(getPathElemInt(hspElem, "query-to"));
				blastHsp.setHitFrom(getPathElemInt(hspElem, "hit-from"));
				blastHsp.setHitTo(getPathElemInt(hspElem, "hit-to"));
				List<Element> hitFrameElems = GlueXmlUtils.findChildElements(hspElem, "hit-frame");
				if(hitFrameElems.size() == 1) {
					blastHsp.setHitFrame(Integer.parseInt(hitFrameElems.get(0).getTextContent()));
				}
				blastHsp.setAlignLen(getPathElemInt(hspElem, "align-len"));
				blastHsp.setGaps(getPathElemInt(hspElem, "gaps"));
				blastHsp.setQseq(getPathElemText(hspElem, "qseq"));
				blastHsp.setHseq(getPathElemText(hspElem, "hseq"));
			}
		}
		return blastResult;
	}

	private static Element getPathElement(Node startNode, String...path) {
		Node node = startNode;
		for(String pathBit: path) {
			node = GlueXmlUtils.findChildElement(node, pathBit);
		}
		return (Element) node;
	}

	private static String getPathElemText(Node startNode, String...path) {
		return getPathElement(startNode, path).getTextContent();
	}

	private static Integer getPathElemInt(Node startNode, String...path) {
		return Integer.parseInt(getPathElemText(startNode, path));
	}

	private static Double getPathElemDouble(Node startNode, String...path) {
		return Double.parseDouble(getPathElemText(startNode, path));
	}

	
	
	private static List<Element> getPathElements(Node startNode, String...path) {
		Node node = startNode;
		for(int i = 0; i < path.length - 1; i++) {
			String pathBit = path[i];
			node = GlueXmlUtils.findChildElement(node, pathBit);
		}
		return GlueXmlUtils.findChildElements(node, path[path.length-1]);
	}


	
}
