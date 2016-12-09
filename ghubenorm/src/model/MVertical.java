package model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

import common.ReflectiveVisitor;
import common.Visitable;

@Entity
@JsonIdentityInfo(generator=JSOGGenerator.class)
public class MVertical extends MDiscrminableGeneralization implements Visitable{
	@OneToMany(mappedBy="generalization")
	private Set<MJoinColumn> joinCols = new HashSet<MJoinColumn>();

	public Set<MJoinColumn> getJoinCols() {
		return joinCols;
	}

	protected void setJoinCols(Set<MJoinColumn> joinCols) {
		this.joinCols = joinCols; 
	}

	@Override
	public void accept(ReflectiveVisitor visitor) {
		for (MJoinColumn jc:joinCols) {
			visitor.callAccept(jc);
		}
		visitor.visit(this);
		
	}
	
}
