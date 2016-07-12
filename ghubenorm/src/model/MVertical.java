package model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

import javax.persistence.OneToMany;

@Entity
public class MVertical extends MDiscrminableGeneralization {
	@OneToMany(mappedBy="generalization")
	private Set<MJoinColumn> joinCols = new HashSet<MJoinColumn>();

	public Set<MJoinColumn> getJoinCols() {
		return joinCols;
	}

	protected void setJoinCols(Set<MJoinColumn> joinCols) {
		this.joinCols = joinCols; 
	}
	
}
