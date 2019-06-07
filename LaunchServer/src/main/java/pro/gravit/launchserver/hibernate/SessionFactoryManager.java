package pro.gravit.launchserver.hibernate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.hibernate.SessionFactory;

import pro.gravit.launchserver.LaunchServer;

public class SessionFactoryManager {
	public SessionFactory fact;
	private static final Map<LaunchServer, SessionFactoryManager> sessionFactories = new ConcurrentHashMap<>();
	private static final Function<LaunchServer, SessionFactoryManager> cr = e -> new SessionFactoryManager();
	public static SessionFactoryManager forLaunchServer(LaunchServer srv) {
		return sessionFactories.computeIfAbsent(srv, cr);
	}
}
