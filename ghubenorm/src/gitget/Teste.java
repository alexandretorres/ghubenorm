package gitget;

public class Teste {
	private String name;
	private String bla;
	protected Teste(){
		
	}
	public Teste(String name, String bla) {
		super();
		this.name = name;
		this.bla = bla;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getBla() {
		return bla;
	}
	public void setBla(String bla) {
		this.bla = bla;
	}
	
}
