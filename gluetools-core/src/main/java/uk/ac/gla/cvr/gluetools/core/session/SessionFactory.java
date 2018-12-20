package uk.ac.gla.cvr.gluetools.core.session;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.session.SessionException.Code;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class SessionFactory {

	private static Multiton multiton = new Multiton();
	private static Multiton.Creator<SessionFactory> creator = new
			Multiton.SuppliedCreator<>(SessionFactory.class, SessionFactory::new);
	
	public static SessionFactory get() {
		return multiton.get(creator);
	}

	private Map<String, Supplier<Session>> sessionTypeToSupplier = new LinkedHashMap<String, Supplier<Session>>();
	
	private SessionFactory() {
		this.registerSessionSupplier(SamFileSession.SESSION_TYPE, SamFileSession::new);
	}
	
	public Session createSession(CommandContext commandContext, SessionKey sessionKey) {
		String sessionType = sessionKey.getSessionType();
		Supplier<Session> supplier = this.sessionTypeToSupplier.get(sessionType);
		if(supplier == null) {
			throw new SessionException(Code.SESSION_CREATION_ERROR, "Unknown session type: "+sessionType);
		}
		Session session = supplier.get();
		session.init(commandContext, sessionKey.getSessionArgs());
		return session;
	}
	
	private void registerSessionSupplier(String sessionType, Supplier<Session> supplier) {
		this.sessionTypeToSupplier.put(sessionType, supplier);
	}
	
}
