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
		
		try(InputStream is = new FileInputStream(new File("/Users/joshsinger/pubs.xml"))) {
			Document doc = GlueXmlUtils.documentFromStream(is);
			List<String> pmids = GlueXmlUtils.getXPathStrings(doc, "/Publications/PubmedArticle/MedlineCitation/PMID/text()");
			System.out.println("id\ttitle\tauthors_short\tauthors_full\tyear\tjournal\tvolume\tissue\tpages\turl\tdoi");
			
			for(String pmid: pmids) {
				System.out.print(pmid);
				String title = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/ArticleTitle/text()");
				System.out.print("\t"+title.trim());
				List<Node> authorNodeList = GlueXmlUtils.getXPathNodes(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/AuthorList/Author");
				String firstAuthorSurname = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/AuthorList/Author[1]/LastName/text()");
				if(authorNodeList.size() == 1) {
					System.out.print("\t"+firstAuthorSurname);
				} else if(authorNodeList.size() == 2) {
					String secondAuthorSurname = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/AuthorList/Author[2]/LastName/text()");
					System.out.print("\t"+firstAuthorSurname+" and "+secondAuthorSurname);
				} else {
					System.out.print("\t"+firstAuthorSurname+" et al.");
				}
				StringBuffer authorsFull = new StringBuffer();
				
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
						if(initials != null) {
							for(char c : initials.toCharArray()) {
								authorsFull.append(c);
								authorsFull.append(". ");
							}
						}
						String surname = GlueXmlUtils.getXPathString(authorNode, "LastName/text()");
						if(surname != null) {
							authorsFull.append(surname);
						}
					}
				}
				System.out.print("\t"+authorsFull.toString());
				String year = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/JournalIssue/PubDate/Year/text()");
				if(year == null) {
					year = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/JournalIssue/PubDate/MedlineDate/text()");					
					if(year != null && year.length() >=4) {
						year = year.substring(0, 4);
					}
				}
				
				System.out.print("\t"+year);
				String journal = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/ISOAbbreviation/text()");
				System.out.print("\t"+journal);
				String volumeText = null;
				String issueText = null;
				Node volumeNode = GlueXmlUtils.getXPathNode(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/JournalIssue/Volume");
				if(volumeNode != null) {
					volumeText = GlueXmlUtils.getXPathString(volumeNode, "text()");
				}
				Node issueNode = GlueXmlUtils.getXPathNode(doc, "/Publications/PubmedArticle/MedlineCitation[PMID/text() = '"+pmid+"']/Article/Journal/JournalIssue/Issue");
				if(issueNode != null) {
					issueText = GlueXmlUtils.getXPathString(issueNode, "text()");
				}
				if(issueText == null && volumeText != null && volumeText.matches("\\d+ \\( Pt \\d+\\)")) {
					issueText = volumeText.substring(volumeText.indexOf("Pt")+3, volumeText.indexOf(")"));
					volumeText = volumeText.substring(0, volumeText.indexOf(" "));
				}
				if(volumeText != null) {
					System.out.print("\t"+volumeText);
				} else {
					System.out.print("\t");
				}
				if(issueText != null) {
					if(issueText.startsWith("Pt ")) {
						issueText = issueText.replace("Pt ", "");
					}
					System.out.print("\t"+issueText);
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
				if(doi == null) {
					doi = GlueXmlUtils.getXPathString(doc, "/Publications/PubmedArticle[MedlineCitation/PMID/text() = '"+pmid+"']/PubmedData/ArticleIdList/ArticleId[@IdType='doi']/text()");				
				}
				if(doi != null) {
					System.out.print("\thttps://doi.org/"+doi);
				} else {
					System.out.print("\t");
				}
				System.out.print("\n");
			
			}
			
		}
		
		
	}
	
}
