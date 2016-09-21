package uk.ac.gla.cvr.gluetools.core.document;

public interface CommandDocumentVisitor {

	default void postVisitCommandDocument(CommandDocument commandDocument) {}

	default void preVisitCommandDocument(CommandDocument commandDocument) {}

	default void preVisitCommandObject(String objFieldName, CommandObject commandObject) {}

	default void postVisitCommandObject(String objFieldName, CommandObject commandObject) {}

	default void visitCommandFieldValue(String objFieldName, CommandFieldValue commandFieldValue) {}

	default void preVisitCommandArray(String arrayFieldName, CommandArray commandArray) {}

	default void postVisitCommandArray(String arrayFieldName, CommandArray commandArray) {}

	default void visitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {}

}
