package pro.gravit.launchserver.dao.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.hwid.OshiHWID;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserDAO;
import pro.gravit.launchserver.dao.UserHWID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class HibernateUserDAOImpl implements UserDAO {
    private final SessionFactory factory;

    public HibernateUserDAOImpl(SessionFactory factory) {
        this.factory = factory;
    }

    public UserHibernateImpl findById(int id) {
        try (Session s = factory.openSession()) {
            return s.get(UserHibernateImpl.class, id);
        }
    }

    public UserHibernateImpl findByUsername(String username) {
        EntityManager em = factory.createEntityManager();
        em.getTransaction().begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserHibernateImpl> personCriteria = cb.createQuery(UserHibernateImpl.class);
        Root<UserHibernateImpl> rootUser = personCriteria.from(UserHibernateImpl.class);
        personCriteria.select(rootUser).where(cb.equal(rootUser.get("username"), username));
        List<UserHibernateImpl> ret = em.createQuery(personCriteria).getResultList();
        em.close();
        return ret.size() == 0 ? null : ret.get(0);
    }

    public UserHibernateImpl findByUUID(UUID uuid) {
        EntityManager em = factory.createEntityManager();
        em.getTransaction().begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserHibernateImpl> personCriteria = cb.createQuery(UserHibernateImpl.class);
        Root<UserHibernateImpl> rootUser = personCriteria.from(UserHibernateImpl.class);
        personCriteria.select(rootUser).where(cb.equal(rootUser.get("uuid"), uuid));
        List<UserHibernateImpl> ret = em.createQuery(personCriteria).getResultList();
        em.close();
        return ret.size() == 0 ? null : ret.get(0);
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