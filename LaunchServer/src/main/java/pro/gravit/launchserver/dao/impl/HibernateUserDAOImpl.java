package pro.gravit.launchserver.dao.impl;

import org.hibernate.Session;
import org.hibernate.Transaction;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserDAO;
import pro.gravit.launchserver.hibernate.HibernateManager;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class HibernateUserDAOImpl implements UserDAO {

    public User findById(int id) {
        return HibernateManager.sessionFactory.openSession().get(User.class, id);
    }

    public User findByUsername(String username) {
        EntityManager em = HibernateManager.sessionFactory.createEntityManager();
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
        EntityManager em = HibernateManager.sessionFactory.createEntityManager();
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
        Session session = HibernateManager.sessionFactory.openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(user);
        tx1.commit();
        session.close();
    }

    public void update(User user) {
        Session session = HibernateManager.sessionFactory.openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(user);
        tx1.commit();
        session.close();
    }

    public void delete(User user) {
        Session session = HibernateManager.sessionFactory.openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(user);
        tx1.commit();
        session.close();
    }

    public List<User> findAll() {
        List<User> users = (List<User>)  HibernateManager.sessionFactory.openSession().createQuery("From User").list();
        return users;
    }
}