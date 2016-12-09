package db.jpa;

import javax.persistence.MappedSuperclass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
@MappedSuperclass
@NamedQueries({
	@NamedQuery(
			name="Repo.FindByURL",
			query="SELECT r FROM Repo r WHERE r.url = :url"
			),
	@NamedQuery(
			name="Repo.FindByName",
			query="SELECT r FROM Repo r WHERE r.name = :name"
			),
	@NamedQuery(
			name="Repo.FindByPublicId",
			query="SELECT r FROM Repo r WHERE r.publicId = :pid"
			)
})
@NamedStoredProcedureQuery(
		name = "CleanRepo", 
		procedureName = "\"CleanRepo\"", 
		parameters = { 
		//	@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class),
			@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class), 
			
			
		}
	)
/**
 * This is a dummy class to store named queries and take advantage of eclipse auto-complete
 * @author torres
 *
 */
final class Queries {
	//This is a dummy class to store named queries and take advantage of eclipse auto-complete
}
