package pro.gravit.launchserver.dao.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.hwid.OshiHWID;
import pro.gravit.launchserver.dao.HwidDAO;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserHWID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class HibernateHwidDAOImpl implements HwidDAO {
    private final SessionFactory factory;

    public HibernateHwidDAOImpl(SessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public List<UserHWIDImpl> findHWIDs(HWID hwid) {
        if(!(hwid instanceof OshiHWID)) throw new UnsupportedOperationException();
        OshiHWID oshiHWID = (OshiHWID) hwid;
        EntityManager em = factory.createEntityManager();
        em.getTransaction().begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserHWIDImpl> personCriteria = cb.createQuery(UserHWIDImpl.class);
        Root<UserHWIDImpl> rootUser = personCriteria.from(UserHWIDImpl.class);
        personCriteria.select(rootUser).where(
                cb.or(
                        cb.equal(rootUser.get("totalMemory"), oshiHWID.totalMemory),
                        cb.equal(rootUser.get("HWDiskSerial"), oshiHWID.HWDiskSerial),
                        cb.equal(rootUser.get("serialNumber"), oshiHWID.serialNumber),
                        cb.equal(rootUser.get("processorID"), oshiHWID.processorID),
                        cb.equal(rootUser.get("macAddr"), oshiHWID.macAddr)
                )
        );
        List<UserHWIDImpl> list = em.createQuery(personCriteria).getResultList();
        em.close();
        return list;
    }

    public UserHWIDImpl findById(long id) {
        try (Session s = factory.openSession()) {
            return s.get(UserHWIDImpl.class, id);
        }
    }
    public void save(UserHWID user) {
        try (Session session = factory.openSession()) {
            Transaction tx1 = session.beginTransaction();
            session.save(user);
            tx1.commit();
        }
    }

    public void update(UserHWID user) {
        try (Session session = factory.openSession()) {
            Transaction tx1 = session.beginTransaction();
            session.update(user);
            tx1.commit();
        }
    }

    public void delete(UserHWID user) {
        try (Session session = factory.openSession()) {
            Transaction tx1 = session.beginTransaction();
            session.delete(user);
            tx1.commit();
        }
    }

    @SuppressWarnings("unchecked")
    public List<UserHWID> findAll() {
        try (Session s = factory.openSession()) {
            return (List<UserHWID>) s.createQuery("From user_hwids").list();
        }
    }
}
