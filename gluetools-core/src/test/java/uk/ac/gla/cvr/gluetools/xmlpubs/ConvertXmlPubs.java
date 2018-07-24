package uk.ac.gla.cvr.gluetools.xmlpubs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class ConvertXmlPubs {

	
	public static void main(String[] args) throws Exception {
		
		try(InputStream is = new FileInputStream(new File("/Users/joshsinger/gitrepos_ssh/PHE-HCV-DRUG-RESISTANCE/tabular/pubs.xml"))) {
			Document doc = GlueXmlUtils.documentFromStream(is);
			List<String> pmids = GlueXmlUtils.getXPathStrings(doc, "/Publications/PubmedArticle/MedlineCitation/PMID/text()");
			for(String pmid: pmids) {
				System.out.print(pmid);
				String title = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/ArticleTitle/text()");
				System.out.print("\t"+title.trim());
				String firstAuthorSurname = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/AuthorList/Author[1]/LastName/text()");
				System.out.print("\t"+firstAuthorSurname+" et al.");
				StringBuffer authorsFull = new StringBuffer();
				List<Node> authorNodeList = GlueXmlUtils.getXPathNodes(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/AuthorList/Author");
				int i = 0;
				for(Node authorNode: authorNodeList) {
					if(i > 0) {
						if(i < authorNodeList.size()-1) {
							authorsFull.append(", ");
						} else {
							authorsFull.append(" and ");
						}
					}
					i++;
					String collectiveName = GlueXmlUtils.getXPathString(authorNode, "CollectiveName/text()");
					if(collectiveName != null) {
						authorsFull.append(collectiveName);
					} else {
						String initials = GlueXmlUtils.getXPathString(authorNode, "Initials/text()");
						for(char c : initials.toCharArray()) {
							authorsFull.append(c);
							authorsFull.append(". ");
						}
						String surname = GlueXmlUtils.getXPathString(authorNode, "LastName/text()");
						authorsFull.append(surname);
					}
				}
				System.out.print("\t"+authorsFull.toString());
				String year = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/JournalIssue/PubDate/Year/text()");
				System.out.print("\t"+year);
				String journal = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/ISOAbbreviation/text()");
				System.out.print("\t"+journal);
				Node volumeNode = GlueXmlUtils.getXPathNode(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/JournalIssue/Volume");
				if(volumeNode != null) {
					System.out.print("\t"+GlueXmlUtils.getXPathString(volumeNode, "text()"));
				} else {
					System.out.print("\t");
				}
				Node issueNode = GlueXmlUtils.getXPathNode(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/JournalIssue/Issue");
				if(issueNode != null) {
					System.out.print("\t"+GlueXmlUtils.getXPathString(issueNode, "text()"));
				} else {
					System.out.print("\t");
				}
				Node pagesNode = GlueXmlUtils.getXPathNode(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Pagination/MedlinePgn");
				if(pagesNode != null) {
					System.out.print("\t"+GlueXmlUtils.getXPathString(pagesNode, "text()"));
				} else {
					System.out.print("\t");
				}
				System.out.print("\t"); // "url"
				String doi = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/ELocationID[@EIdType='doi']/text()");
				System.out.print("\thttps://doi.org/"+doi);
				System.out.print("\n");
			
			}
			
		}
		
		
	}
	
}
