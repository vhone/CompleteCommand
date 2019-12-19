package vhone.api.completecommand;

public enum Type {
	
	STRING	("<String>",	"^.*$"						),
	INTEGER	("<Integer>",	"^[0-9]*$"					),
	DOUBLE	("<Double>",	"^[0-9]+.[0-9]*|[0-9]*$"	),
	BOOLEAN	("<Boolean>",	"^(?i)true|(?i)false$"		);
	
	private String name;
	private String expression;
	
	private Type(String name, String expression) {
		this.name = name;
		this.expression = expression;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
	public String getRegex() {
		return this.expression;
	}
	
	public Object parsedValue(String arg) {
		Object value = null; 
		switch (this) {
			case BOOLEAN: 	value = Boolean.valueOf(arg); break;
			case INTEGER: 	value = Integer.valueOf(arg); break;
			case DOUBLE: 	value =  Double.valueOf(arg); break;
			case STRING: 	value = arg; break;
		}
		return value;
	}
	
	public static Type parseType(String name) {
		Type type = null;
		try {
			for (Type t : Type.values()) {
				if (t.toString().equals(name)) {
					type = t;
					break;
				} else {
					throw new Exception("잘못된 타입 변환");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return type;
	}

}
