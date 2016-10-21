package dao.jpa;

import common.ReflectiveVisitor;
import dao.ConfigDAO;
import gitget.Log;
/**
 * This default visitor deletes every object by visiting the tree. Implementors can extend this visitor
 * implementing the visit<ClassName> methods to perform extra deletion such as setting associations to NULL
 * before the removal of interdependant classes
 * @author torres
 *
 */
public abstract class CascadeDeleteVisitor extends ReflectiveVisitor {	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void visitObject(Object obj) {
		DAO dsdao = ConfigDAO.getDAO(obj.getClass());
		if (DAO.getEm().contains(obj))
			dsdao.remove(obj);
		//DAO.getEm().flush();
		if (DAO.getEm().contains(obj))
			Log.LOG.warning("Object not removed by cascadedeletevisitor:"+obj);
	}
	
}