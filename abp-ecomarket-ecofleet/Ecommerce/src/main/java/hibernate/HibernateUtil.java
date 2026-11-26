package hibernate;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Utilidad Singleton para inicializar y gestionar la SessionFactory de Hibernate.
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            try {
                // Silenciar logs informativos/advertencias internas de arranque de Hibernate en consola
                Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
                sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
            } catch (Throwable ex) {
                System.err.println("[ERROR] Fallo al inicializar SessionFactory de Hibernate: " + ex.getMessage());
                throw new ExceptionInInitializerError(ex);
            }
        }
        return sessionFactory;
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
