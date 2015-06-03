package uk.ac.gla.cvr.gluetools.core.testdb;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class TestDB {

	//@Test
	public void test() throws Exception {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("glue-persistence-unit");
		EntityManager entityManager = emf.createEntityManager();
		entityManager.close();
		emf.close();
	}
	
}
