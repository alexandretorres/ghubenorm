package model;

public enum Visibility {
PUBLIC,PACKAGE,PROTECTED,PRIVATE;

	public static Visibility findVisibility(boolean pub,boolean protec,boolean priv) {
		if (pub)
			return PUBLIC;
		if (protec)
			return PROTECTED;
		if (priv)
			return PRIVATE;
		return PACKAGE;
	}
}
