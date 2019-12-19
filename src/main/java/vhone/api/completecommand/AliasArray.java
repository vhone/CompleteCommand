package vhone.api.completecommand;

public final class AliasArray implements Aliasable {

	private String alias;
	private Object[] object;
	
	public AliasArray(String alias, String[] array) {
		this.alias = "[" + alias + "]";
		this.object = array;
	}
	public AliasArray(String alias, AliasObject[] array) {
		this.alias = "[" + alias + "]";
		this.object = array;
	}
	
//	@Override
//	public String getAlias() {
//		return this.alias;
//	}

	@Override
	public Object[] getData() {
		return this.object;
	}
	
	@Override
	public String toString() {
		return this.alias;
	}
}
