package db.jpa;

import java.util.Date;

import db.daos.RepoDAO;
/**
 * A facade to repo services exclusive for database
 * @author torres
 *
 */
public class RepoDB {
	RepoDaoImpl dao;
	public RepoDB(RepoDAO dao) {
		this.dao = (RepoDaoImpl) dao;
	}
	public void setDate(int publicId,Date dt,int lastPid) {
		dao.setDate(publicId, dt,lastPid);
	}
	public void setDate(String name,Date dt) {
		dao.setDate(name, dt);
	}
}
