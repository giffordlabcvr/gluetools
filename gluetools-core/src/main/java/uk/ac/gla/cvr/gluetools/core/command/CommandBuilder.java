package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

public class CommandBuilder<R extends CommandResult, C extends Command<R>> {

	private CommandContext cmdContext;
	private DocumentBuilder documentBuilder;
	private Class<C> cmdClass;
	private ObjectBuilder cmdObjectBuilder;
	
	public CommandBuilder(CommandContext cmdContext, Class<C> cmdClass) {
		super();
		this.cmdContext = cmdContext;
		this.cmdClass = cmdClass;
		String[] cmdWords = CommandUsage.cmdWordsForCmdClass(cmdClass);
		documentBuilder = new DocumentBuilder(cmdWords[0]);
		cmdObjectBuilder = documentBuilder;
		for(int i = 1; i < cmdWords.length; i++) {
			cmdObjectBuilder = cmdObjectBuilder.setObject(cmdWords[i]);
		}
	}

	public CommandBuilder<R, C> setInt(String name, int value) {
		cmdObjectBuilder.setInt(name, value);
		return this;
	}

	public CommandBuilder<R, C> setBoolean(String name, boolean value) {
		cmdObjectBuilder.setBoolean(name, value);
		return this;
	}

	public CommandBuilder<R, C> setDouble(String name, double value) {
		cmdObjectBuilder.setDouble(name, value);
		return this;
	}

	public CommandBuilder<R, C> setNull(String name) {
		cmdObjectBuilder.setNull(name);
		return this;
	}

	public CommandBuilder<R, C> setString(String name, String value) {
		cmdObjectBuilder.setString(name, value);
		return this;
	}

	public CommandBuilder<R, C> set(String name, Object value) {
		cmdObjectBuilder.set(name, value);
		return this;
	}

	public CommandArrayBuilder setArray(String name) {
		ArrayBuilder arrayBuilder = cmdObjectBuilder.setArray(name);
		return new CommandArrayBuilder(arrayBuilder);
	}
	
	public class CommandArrayBuilder {
		private ArrayBuilder arrayBuilder;
		
		public CommandArrayBuilder(ArrayBuilder arrayBuilder) {
			this.arrayBuilder = arrayBuilder;
		}
		
		public CommandArrayBuilder addInt(int value) {
			arrayBuilder.addInt(value);
			return this;
		}
		public CommandArrayBuilder addBoolean(boolean value) {
			arrayBuilder.addBoolean(value);
			return this;
		}
		public CommandArrayBuilder addDouble(double value) {
			arrayBuilder.addDouble(value);
			return this;
		}
		public CommandArrayBuilder addNull() {
			arrayBuilder.addNull();
			return this;
		}
		public CommandArrayBuilder addString(String value) {
			arrayBuilder.addString(value);
			return this;
		}
		public CommandArrayBuilder add(Object value) {
			arrayBuilder.add(value);
			return this;
		}
		public C build() {
			return CommandBuilder.this.build();
		}
		public R execute() {
			return CommandBuilder.this.execute();
		}

	}

	public C build() {
		return cmdClass.cast(cmdContext.commandFromElement(documentBuilder.getXmlDocument().getDocumentElement()));
	}

	public R execute() {
		return build().execute(cmdContext);
	}

	
	
}
