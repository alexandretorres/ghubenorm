package model;

public enum Language {
	JAVA,RUBY,PYTHON,OTHER;
	public static Language getLanguage(String lang) {		
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
