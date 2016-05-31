package db.jpa;

import javax.persistence.MappedSuperclass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
@MappedSuperclass
@NamedQueries({
	@NamedQuery(
			name="Repo.FindByURL",
			query="SELECT r FROM Repo r WHERE r.url = :url"
			)
})
/**
 * This is a dummy class to store named queries and take advantage of eclipse auto-complete
 * @author torres
 *
 */
final class Queries {
	//This is a dummy class to store named queries and take advantage of eclipse auto-complete
}
