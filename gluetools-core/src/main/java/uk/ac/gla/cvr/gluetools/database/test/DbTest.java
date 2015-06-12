package uk.ac.gla.cvr.gluetools.database.test;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.database.Artist;
import uk.ac.gla.cvr.gluetools.database.Gallery;
import uk.ac.gla.cvr.gluetools.database.Painting;

public class DbTest {
	
	public static void main(String[] args) throws Exception {
		ServerRuntime serverRuntime = new ServerRuntime("cayenne-project.xml");
		ObjectContext context = serverRuntime.getContext();
		
		Artist picasso = context.newObject(Artist.class);
		picasso.setName("Pablo Picasso");
		picasso.setDateOfBirthString("18811025");
		
		Gallery metropolitan = context.newObject(Gallery.class);
		metropolitan.setName("Metropolitan Museum of Art"); 

		Painting girl = context.newObject(Painting.class);
		girl.setName("Girl Reading at a Table");
		        
		Painting stein = context.newObject(Painting.class);
		stein.setName("Gertrude Stein");

	
		picasso.addToPaintings(girl);
		picasso.addToPaintings(stein);
		        
		girl.setGallery(metropolitan);
		stein.setGallery(metropolitan);
		
		context.commitChanges();
	}
}
