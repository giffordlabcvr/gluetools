package uk.ac.gla.cvr.gluetools.core.document;


public class CommandDocument extends CommandObject {

	private String rootName;
	
	public CommandDocument(String rootName) {
		super();
		this.rootName = rootName;
	}

	public String getRootName() {
		return rootName;
	}

	public void accept(CommandDocumentVisitor visitor) {
		visitor.preVisitCommandDocument(this);
		super.accept(rootName, visitor);
		visitor.postVisitCommandDocument(this);
	}
}
