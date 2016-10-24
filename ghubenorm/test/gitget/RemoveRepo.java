package gitget;

import static gitget.Log.LOG;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.persistence.TypedQuery;

import org.junit.Before;
import org.junit.Test;

import dao.ConfigDAO;
import dao.jpa.CascadeDeleteVisitor;
import dao.jpa.DAO;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.MAssociation;
import model.MAssociationDef;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MJoinColumn;
import model.MProperty;
import model.MTable;
import model.Repo;
public class RemoveRepo {
	@Before
	public void setUp() throws Exception {
	//	ConfigDAO.config(new ConfigNop());
		ConfigDAO.config(JPA_DAO.instance);
	}
	@Test
	public void test() {
		String repos[] = {	
				



			/*	"jadeforrest/OpenACS-on-Rails",
				"steven-zhou/amazon",
				"fisherv1/amazon_project",
				"jgilliam/nb-deprecated",
				"alexs/salva-old"*/

		/*		"SRabbelier/Netbeans",
				"netspective/medigy-java",
				"hibernate/hibernate-search",
				"bobeal/capdemat",
				"identityxx/velo1"*/

			//	"tulios/datasync",
			//	"irishjava/looncms",
			//	"apache/activemq",
			//	"nikhilgupte/shk-fas",				
			//	"activescaffold/active_scaffold",
			//	"djcp/cohort",
			//	"holtonma/markholton-old",
		};
		for (String repo:repos) 
			doRemove(repo);
		
	}
	private void doRemove(String repoName) {
		boolean f=false;
		RepoDAO dao = ConfigDAO.getDAO(Repo.class);
		dao.beginTransaction();
		for (Repo r:dao.findByName(repoName)) {
			CascadeDeleteVisitor del = new CleanRepoVisitor(r);			
			//dao.removeCascade(r);
			LOG.info("visitor executed, now issuing commit for:"+r.getId()+" name "+repoName);
			f=true;
		}
		
		dao.commitAndCloseTransaction();
		if (!f)
			LOG.warning("could not find repo "+repoName);
		else
			LOG.info("repo removed:"+repoName);
	}
}
/**
 * Just cleans the repo (removing classes, sources and everything else)
 * @author torres
 *
 */
class CleanRepoVisitor extends RemoveRepoVisitor {

	CleanRepoVisitor(Repo r) {
		super(r);
		// TODO Auto-generated constructor stub
	}
	public void visitRepo(Repo r) {
		r.getClasses().clear();
		r.getDataSources().clear();
	}
	
}
/**
 * Removes the repo from database
 * @author torres
 *
 */
class RemoveRepoVisitor extends CascadeDeleteVisitor {
	RemoveRepoVisitor(Repo r) {		
		int cnt=DAO.getEm().createQuery(
				"update MClass c set c.superClass=null "
				+ "where c.superClass is not null"
				+ "  and c.repo=:repo").setParameter("repo", r) .executeUpdate();
		
		DAO.getEm().flush();
		LOG.info("seted "+cnt+" superclasses to null");
		
		cnt=DAO.getEm().createQuery(
				"delete from MOverride o2 "				
				+ "where o2.id in (select o.id from MOverride o "
				+ "where o.clazz.repo=:repo)").setParameter("repo", r) .executeUpdate();
		if (cnt>0)
			LOG.info("removed  "+cnt+" overrides");
		cnt=DAO.getEm().createQuery(
				"delete from MDefinition d2 "				
				+ "where d2.id in (select d.id from MDefinition d "
				+ "where d.table.repo=:repo)").setParameter("repo", r) .executeUpdate();
		if (cnt>0)
			LOG.info("removed  "+cnt+" constraint defs");
		DAO.getEm().flush();
		DAO.getEm().refresh(r);
		r.accept(this);	
	}
	
	public void visitMClass(MClass cl) {
		cl.setSuperClass(null);
		if (cl.getDiscriminatorColumn()!=null)
			cl.getDiscriminatorColumn().setColumn(null);		
		cl.getPersistence().setDataSource(null);
		// remove typeClass references
		TypedQuery<MProperty> q = DAO.getEm().createQuery("from MProperty p where p.typeClass=:tp", MProperty.class).setParameter("tp",cl);
		for (MProperty p:q.getResultList()) {
			p.setTypeClass(null);
		}
		//
		
		cl.getRepo().getClasses().remove(cl);
		//DAO.getEm().flush();
		visitObject(cl);
	}
	public void visitMProperty(MProperty p) {
		if (p.getToAssociation()!=null) {
			p.getToAssociation().setTo(null);
			p.setToAssociation(null);
		}
		p.setTypeClass(null);	
		p.getParent().getProperties().remove(p);
		visitObject(p);
	}
	public void visitMAssociationDef(MAssociationDef def) {
		def.getJoinColumns().clear();
		visitObject(def);
		
	}
	public void visitMJoinColumn(MJoinColumn jc) {
		MColumnDefinition col = jc.getColumn();
		if (jc.getAssociationDef()!=null)
			jc.getAssociationDef().getJoinColumns().remove(jc);
		if (jc.getGeneralization()!=null)
			jc.getGeneralization().getJoinCols().remove(jc);
		
		visitObject(jc);
		if (col!=null && col.getTable()==null)
			callAccept(col);
	}
	public void visitMAssociation(MAssociation a) {
		if (a.getTo()!=null)
			a.getTo().setToAssociation(null);
		
		visitObject(a);
	}

	@Override
	public void invoke(Method method, Object[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		method.invoke(this, args);
		
	}
}
