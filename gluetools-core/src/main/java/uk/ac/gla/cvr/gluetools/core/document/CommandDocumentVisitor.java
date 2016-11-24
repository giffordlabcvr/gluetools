package uk.ac.gla.cvr.gluetools.core.document;

public interface CommandDocumentVisitor {

	default void postVisitCommandDocument(CommandDocument commandDocument) {}

	default void preVisitCommandDocument(CommandDocument commandDocument) {}

	default void preVisitCommandObject(String objFieldName, CommandObject commandObject) {}

	default void postVisitCommandObject(String objFieldName, CommandObject commandObject) {}

	default void preVisitCommandFieldValue(String objFieldName, CommandFieldValue commandFieldValue) {}

	default void postVisitCommandFieldValue(String objFieldName, CommandFieldValue commandFieldValue) {}

	default void preVisitCommandArray(String arrayFieldName, CommandArray commandArray) {}

	default void postVisitCommandArray(String arrayFieldName, CommandArray commandArray) {}

	default void preVisitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {}

	default void postVisitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {}

}
