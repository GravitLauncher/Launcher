package pro.gravit.launchserver.hibernate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.SessionFactory;

import pro.gravit.launchserver.LaunchServer;

public class HibernateManager {
    private static final Map<LaunchServer, SessionFactory> sessionMap = new ConcurrentHashMap<>();

	public static SessionFactory getSessionFactory(LaunchServer srv) {
    	return sessionMap.get(srv);
    };

	public static SessionFactory setSessionFactory(LaunchServer srv, SessionFactory fact) {
    	return sessionMap.get(srv);
    };
}
