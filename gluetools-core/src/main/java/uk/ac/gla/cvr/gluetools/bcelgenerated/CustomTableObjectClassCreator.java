package uk.ac.gla.cvr.gluetools.bcelgenerated;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.generic.AnnotationEntryGen;
import org.apache.bcel.generic.ArrayElementValueGen;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ElementValueGen;
import org.apache.bcel.generic.ElementValuePairGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.SimpleElementValueGen;
import org.apache.bcel.generic.Type;

import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;

public class CustomTableObjectClassCreator {
	private InstructionFactory _factory;
	private ConstantPoolGen    _cp;
	private ClassGen           _cg;

	public CustomTableObjectClassCreator(String projectName, String tableName) {
		_cg = new ClassGen(getFullClassName(projectName, tableName), 
				CustomTableObject.class.getCanonicalName(), 
				getSimpleClassName(projectName, tableName)+".java", 
				Const.ACC_PUBLIC | Const.ACC_SUPER, new String[] {  });
		_cp = _cg.getConstantPool();
		_factory = new InstructionFactory(_cg, _cp);
	}

	public static String getFullClassName(String projectName, String tableName) {
		return CustomTableObject.class.getPackage().getName()+"."+getSimpleClassName(projectName, tableName);
	}

	public static String getSimpleClassName(String projectName, String tableName) {
		return "CustomTableObject_"+projectName+"_"+tableName;
	}

	public byte[] create() {
		createMethod_0();
		createAnnotation_0();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			_cg.getJavaClass().dump(baos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return baos.toByteArray();
	}

	private void createAnnotation_0() {
		ObjectType objectType = new ObjectType("uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass");

		ArrayList<ElementValuePairGen> elemValuePairs = new ArrayList<ElementValuePairGen>();
		ArrayElementValueGen arrayElementValueGen = new ArrayElementValueGen(_cp);
		arrayElementValueGen.addElement(new SimpleElementValueGen(ElementValueGen.STRING, _cp, CustomTableObject.ID_PROPERTY));
		elemValuePairs.add(new ElementValuePairGen("defaultListedProperties", arrayElementValueGen, _cp));

		AnnotationEntryGen annotationEntryGen = new AnnotationEntryGen(objectType, elemValuePairs, true, _cp);
		_cg.addAnnotationEntry(annotationEntryGen);
	}

	@SuppressWarnings({ "static-access", "unused" })
	private void createMethod_0() {
		InstructionList il = new InstructionList();
		MethodGen method = new MethodGen(Const.ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {  }, "<init>", "uk.ac.gla.cvr.gluetools.bcelgenerated.CustomTableObject_foo_xxx", il, _cp);

		InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
		il.append(_factory.createInvoke("uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject", "<init>", Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
		InstructionHandle ih_4 = il.append(_factory.createReturn(Type.VOID));
		method.setMaxStack();
		method.setMaxLocals();
		_cg.addMethod(method.getMethod());
		il.dispose();
	}

	public static void main(String[] args) throws Exception {
		uk.ac.gla.cvr.gluetools.bcelgenerated.CustomTableObjectClassCreator creator = 
				new uk.ac.gla.cvr.gluetools.bcelgenerated.CustomTableObjectClassCreator("foo", "xxx");
		creator.create();
		System.out.println("-----------------------------------------------------------------------------------");
		System.out.println("Programmatically generated class:");
		System.out.println("-----------------------------------------------------------------------------------");
		System.out.println(creator._cg.getJavaClass().toString());
		System.out.println("-----------------------------------------------------------------------------------");
		System.out.println("Parsed class:");
		System.out.println("-----------------------------------------------------------------------------------");
		ClassParser classParser = new ClassParser(CustomTableObject_foo_xxx.class.getResourceAsStream("CustomTableObject_foo_xxx.class"), "CustomTableObject_foo_xxx.class");
		System.out.println(classParser.parse().toString());
		System.out.println("-----------------------------------------------------------------------------------");

	}
}
