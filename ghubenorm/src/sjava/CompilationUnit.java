package sjava;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static sjava.JPATags.*;

public class CompilationUnit {
	Set<Clazz> classes= new HashSet<Clazz>();
	Set<Import> imports = new HashSet<Import>();
	public boolean importsTag(JPATags tag) {
		for (Import imp:imports) {
			if (tag.isImport(imp))
				return true;			
		}
		return false;
	}
	public String toString() {
		String ret="";
		for (Import i:imports) {
			if (i.from.equals("javax.persistence.*")) {
				ret+=("imports all persistence package\n");
			} else if (i.from.equals("javax.persistence.Entity")) {
				ret+=("imports Entity persistence package\n");
			}
		}
		for (Clazz c:classes) {
			ret+=c.toString()+"\n";
		}
		return ret;
	}
}
class Import {
	String from;
	public static Import newImport(String from) {
		Import ret = new Import();
		ret.from=from;
		return ret;
	}
	private Import() {
		
	}
	public String getFrom() {
		return from;
	}
}