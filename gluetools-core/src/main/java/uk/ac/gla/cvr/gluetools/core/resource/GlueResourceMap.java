package uk.ac.gla.cvr.gluetools.core.resource;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class GlueResourceMap {

	private static Multiton instances = new Multiton();
	
	private static Multiton.Creator<GlueResourceMap> creator = new
			Multiton.SuppliedCreator<>(GlueResourceMap.class, GlueResourceMap::new);
	
	public static GlueResourceMap getInstance() {
		return instances.get(creator);
	}
	
	private Map<String, byte[]> nameToBytes = new LinkedHashMap<String, byte[]>();
	
	public void put(String name, byte[] bytes) {
		nameToBytes.put(name, bytes);
	}
	
	public byte[] get(String name) {
		return nameToBytes.get(name);
	}
	
	private GlueResourceMap() {

	}

}
