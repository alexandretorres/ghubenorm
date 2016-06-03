package model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
 

@Entity

public class Repo {
	
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)	
	private int id;
	private String name;
	private String url;
	private Language language;	
	/**
	 * This is the internal path to the "base" file that defines the repository. For ruby it is the schema.db file,
	 * for java it may be the presistence.xml. It is used to differentiate what portion of the repository is 
	 * under analysis. Notice that a repository MAY have more than one "database", in distinct source directories. 
	 * Sometimes these will be just "test" databases. 
	 */
	private String configPath;
	
	@OneToMany(fetch=FetchType.EAGER,mappedBy="repo",cascade=CascadeType.ALL)
	Set<MClass> classes = new HashSet<MClass>();
	@OneToMany(fetch=FetchType.EAGER,mappedBy="repo",cascade=CascadeType.PERSIST)
	Set<MTable> tables = new HashSet<MTable>();
	
	protected Repo() {}
	public Repo(Language lang) {
		this.language=lang;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Language getLanguage() {
		return language;
	}
	public void setLanguage(Language language) {
		this.language = language;
	}
	public String getConfigPath() {
		return configPath;
	}
	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}
	public Set<MClass> getClasses() {
		return classes;
	}
	protected void setClasses(Set<MClass> classes) {
		this.classes = classes;
	}
	public Set<MTable> getTables() {
		return tables;
	}
	protected void setTables(Set<MTable> tables) {
		this.tables = tables;
	}
	
}
