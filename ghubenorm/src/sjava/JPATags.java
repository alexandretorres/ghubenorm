package sjava;

public enum JPATags {
	Entity,PK,Column;
	String path;
	JPATags() {
		path="javax.persistence";
	}
	JPATags(String path) {
		this.path=path;
	}
	public boolean isImport(String s) {
		if (s==null)
			return false;
		if (s.equals(path+"."+this.name())) {
			return true;
		}
		if (s.endsWith(".*")) {
			s = s.substring(0,s.length()-2);
			return s.equals(path);
		}
		return false;
	}
	public boolean isImport(Import imp) {
		return isImport(imp.getFrom());
	}
}
