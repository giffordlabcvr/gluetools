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
