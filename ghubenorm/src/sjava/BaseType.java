package sjava;

public enum BaseType {
	LONG("Long"),STRING("String"),INT("int"),INTEGER("Integer"),BOOLEAN("Boolean"),BOOL("boolean"),DOUBLE("double"),DOUBLE_CLASS("double"),
	FLOAT("float"),FLOAT_CLASS("Float"),SHORT("short"),SHORT_CLASS("Short"),CHAR("char"),CHARACTER("Character"),BYTE("byte"),BYTE_CLASS("Byte"),
	DATE("Date"),TIME("Time"),TIMESTAMP("TimeStamp"),BIG_DECIMAL("BigDecimal"),BIG_INTEGER("BigInteger");
	String name;
	BaseType(String name) {
		this.name= name;
		
	}
	public static boolean isBaseType(String typeName) {
		if (typeName==null)
			return false;
		if (typeName.startsWith("java.lang.") || typeName.startsWith("java.sql.") || typeName.startsWith("java.math.") || typeName.startsWith("java.util.") )
			typeName =  typeName.substring(typeName.lastIndexOf(".")+1);
		for (BaseType bt:BaseType.values()) {
			if (bt.name.equals(typeName))
				return true;
			
		}
		return false;
	}
}
