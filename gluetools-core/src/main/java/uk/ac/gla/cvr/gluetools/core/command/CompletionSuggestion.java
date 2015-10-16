package uk.ac.gla.cvr.gluetools.core.command;

public class CompletionSuggestion {

	private String suggestedWord;
	private boolean completed;
	
	public CompletionSuggestion(String suggestedWord, boolean completed) {
		super();
		this.suggestedWord = suggestedWord;
		this.completed = completed;
	}

	public String getSuggestedWord() {
		return suggestedWord;
	}

	public boolean isCompleted() {
		return completed;
	}
	
}
