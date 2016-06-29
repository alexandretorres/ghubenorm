package model;

public enum Language {
	JAVA,RUBY,PYTHON,OTHER;
	public static Language getLanguage(String lang) {	
		if (lang==null)
			return OTHER;
		switch (lang) {
			case "Java":
				return JAVA;
			case "Ruby":
				return RUBY;
			case "Python":
				return PYTHON;
			default:				
				return OTHER;
				
		}			
	}
}
