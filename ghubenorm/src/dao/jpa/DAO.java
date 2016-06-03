package dao.jpa;

import static gitget.Log.LOG;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import dao.DAOInterface;
import gitget.Auth;

public class DAO<C> implements DAOInterface<C>{
	private static EntityManagerFactory emf;// = Persistence.createEntityManagerFactory("primary");
	private static EntityManager em;
	private Class<C> clazz;
	
	static {
		Map<String,String> props = new HashMap<String,String>();
		props.put("hibernate.connection.password", Auth.getProperty("hibernate.connection.password"));
		props.put("hibernate.show_sql", "false");
		emf = Persistence.createEntityManagerFactory("gitenorm",props);
	}
	
	protected DAO(Class<C> cl) {
		clazz = cl;
		((ConfigJPA)ConfigJPA.getConfig()).addDAO(cl,this);
	}
	/*
	@SuppressWarnings("unchecked")
	protected Class<C> getTypeClass() {
		if (clazz==null)
			clazz = (Class<C>) ((ParameterizedType) getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[0];
		return clazz;
	}*/
	public static EntityManager getEm() {
		return em;
	}
	public static void setEm(EntityManager pem) {
		em = pem;
	}
	
	public void beginTransaction() {
		try{
			em = emf.createEntityManager();			 
			em.getTransaction().begin();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}
	public void commitAndCloseTransaction() {
		if (em.getTransaction().isActive())
			em.getTransaction().commit();
		em.close();
		em=null;
	}
	public void rollbackAndCloseTransaction() {
		try {
			if (em.getTransaction().isActive())
				em.getTransaction().rollback();
		} catch (Exception ex) {
			LOG.log(Level.WARNING,ex.getMessage(),ex);	
		}
		
		em.close();
		em=null;
	}
	public <T> T first(Collection<T> col) {
		if (col.isEmpty())
			return null;
		return col.iterator().next();
	}
	public C find(Object pk) {
		return em.find(clazz, pk);
	}
	public C persit(C obj) {
		em.persist(obj);
		return obj;
	}
	public static void finish() {
		//map=null;
		try {
			if (em!=null && em.isOpen()) {
				em.close();
			}
			if (emf!=null && emf.isOpen()) {			
				emf.close();	
				
			}
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
		
	}
	
}
