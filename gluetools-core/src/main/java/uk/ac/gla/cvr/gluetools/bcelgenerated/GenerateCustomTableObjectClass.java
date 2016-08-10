package uk.ac.gla.cvr.gluetools.bcelgenerated;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.util.BCELifier;


public class GenerateCustomTableObjectClass {

	public static void main(String[] args) throws Exception {
		
		ClassParser classParser = new ClassParser(CustomTableObject_foo_xxx.class.getResourceAsStream("CustomTableObject_foo_xxx.class"), "CustomTableObject_foo_xxx.class");
		
		BCELifier bcelifier = new BCELifier(classParser.parse(), System.out);
		
		bcelifier.start();
		
	}
	
}
