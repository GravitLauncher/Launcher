package pro.gravit.launchserver.dao.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.Transaction;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserDAO;
import pro.gravit.launchserver.hibernate.SessionFactoryManager;

public class HibernateUserDAOImpl implements UserDAO {
	private final SessionFactoryManager manager;

	public HibernateUserDAOImpl(LaunchServer srv) {
		manager = SessionFactoryManager.forLaunchServer(srv);
	}

    public User findById(int id) {
        try (Session s =  manager.fact.openSession()) {
        	return s.get(User.class, id);
        }
    }

    public User findByUsername(String username) {
        EntityManager em = manager.fact.createEntityManager();
        em.getTransaction().begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> personCriteria = cb.createQuery(User.class);
        Root<User> rootUser = personCriteria.from(User.class);
        personCriteria.select(rootUser).where(cb.equal(rootUser.get("username"), username));
        List<User> ret = em.createQuery(personCriteria).getResultList();
        em.close();
        return ret.size() == 0 ? null : ret.get(0);
    }

    public User findByUUID(UUID uuid) {
        EntityManager em = manager.fact.createEntityManager();
        em.getTransaction().begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> personCriteria = cb.createQuery(User.class);
        Root<User> rootUser = personCriteria.from(User.class);
        personCriteria.select(rootUser).where(cb.equal(rootUser.get("uuid"), uuid));
        List<User> ret = em.createQuery(personCriteria).getResultList();
        em.close();
        return ret.size() == 0 ? null : ret.get(0);
    }

    public void save(User user) {
        try (Session session = manager.fact.openSession()) {
        	Transaction tx1 = session.beginTransaction();
        	session.save(user);
        	tx1.commit();
    	}
    }

    public void update(User user) {
        try (Session session = manager.fact.openSession()) {
        	Transaction tx1 = session.beginTransaction();
        	session.update(user);
        	tx1.commit();
    	}
    }

    public void delete(User user) {
        try (Session session = manager.fact.openSession()) {
        	Transaction tx1 = session.beginTransaction();
        	session.delete(user);
        	tx1.commit();
        }
    }

    @SuppressWarnings("unchecked")
	public List<User> findAll() {
    	try (Session s = manager.fact.openSession()) {
    		return (List<User>) s.createQuery("From User").list();
    	}
    }
}