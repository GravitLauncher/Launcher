package pro.gravit.launchserver.dao.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import pro.gravit.launcher.hwid.OshiHWID;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserDAO;
import pro.gravit.launchserver.dao.UserHWID;

public class HibernateUserDAOImpl implements UserDAO {
	private final SessionFactory factory;

	public HibernateUserDAOImpl(SessionFactory factory) {
		this.factory = factory;
	}

    public User findById(int id) {
        try (Session s =  factory.openSession()) {
        	return s.get(User.class, id);
        }
    }

    public User findByUsername(String username) {
        EntityManager em = factory.createEntityManager();
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
        EntityManager em = factory.createEntityManager();
        em.getTransaction().begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> personCriteria = cb.createQuery(User.class);
        Root<User> rootUser = personCriteria.from(User.class);
        personCriteria.select(rootUser).where(cb.equal(rootUser.get("uuid"), uuid));
        List<User> ret = em.createQuery(personCriteria).getResultList();
        em.close();
        return ret.size() == 0 ? null : ret.get(0);
    }

    @Override
    public List<UserHWID> findHWID(OshiHWID hwid) {
        EntityManager em = factory.createEntityManager();
        em.getTransaction().begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserHWID> personCriteria = cb.createQuery(UserHWID.class);
        Root<UserHWID> rootUser = personCriteria.from(UserHWID.class);
        personCriteria.select(rootUser).where(
                cb.or(
                        cb.equal(rootUser.get("totalMemory"), hwid.totalMemory),
                        cb.equal(rootUser.get("HWDiskSerial"), hwid.HWDiskSerial),
                        cb.equal(rootUser.get("serialNumber"), hwid.serialNumber),
                        cb.equal(rootUser.get("processorID"), hwid.processorID),
                        cb.equal(rootUser.get("macAddr"), hwid.macAddr)
                )
        );
        List<UserHWID> ret = em.createQuery(personCriteria).getResultList();
        em.close();
        return ret;
    }

    public void save(User user) {
        try (Session session = factory.openSession()) {
        	Transaction tx1 = session.beginTransaction();
        	session.save(user);
        	tx1.commit();
    	}
    }

    public void update(User user) {
        try (Session session = factory.openSession()) {
        	Transaction tx1 = session.beginTransaction();
        	session.update(user);
        	tx1.commit();
    	}
    }

    public void delete(User user) {
        try (Session session = factory.openSession()) {
        	Transaction tx1 = session.beginTransaction();
        	session.delete(user);
        	tx1.commit();
        }
    }

    @SuppressWarnings("unchecked")
	public List<User> findAll() {
    	try (Session s = factory.openSession()) {
    		return (List<User>) s.createQuery("From User").list();
    	}
    }
}