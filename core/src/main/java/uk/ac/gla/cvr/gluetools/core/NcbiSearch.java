package uk.ac.gla.cvr.gluetools.core;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class NcbiSearch {

	public static void main(String[] args) throws Exception {
		
		CloseableHttpClient httpclient = HttpClients.createDefault();

		// lists all the databases
		// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi
		
		// meta-data on database nuccore
		// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi?db=nuccore&version=2.0
		
		
		
		//String dbName = "nuccore";
		String dbName = "nuccoddre";
		
		String url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db="+dbName;
		HttpPost httpPost = new HttpPost(url);
		
		String searchTerm = "\"Hepatitis C\"[Organism] AND 7000:10000[SLEN]";
		
		StringEntity requestEntity = new StringEntity("term="+searchTerm);
		
		requestEntity.setContentType("application/x-www-form-urlencoded");
		httpPost.setEntity(requestEntity);

		Document document;
		
		try(CloseableHttpResponse response = httpclient.execute(httpPost);) {
		    if(response.getStatusLine().getStatusCode() != 200) {
		    	throw new Exception("HTTP failed: "+response.getStatusLine().toString());
		    }
		    
		    HttpEntity entity = response.getEntity();
		    document = XmlUtils.documentFromStream(entity.getContent());
		    // ensure it is fully consumed
		    EntityUtils.consume(entity);
		}
		
		XPath xpath = XPathFactory.newInstance().newXPath();
		String expression = "/eSearchResult/IdList/Id/text()";
		NodeList idNodeTextList = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
		for(int i = 0; i < idNodeTextList.getLength(); i++) {
			Node idNodeText = idNodeTextList.item(i);
			if(idNodeText instanceof Text) {
				System.out.println("ID found:"+((Text) idNodeText).getWholeText());
			}
		}
		
		
		XmlUtils.prettyPrint(document, System.out);
		
	}
	
}
