package vhone.api.completecommand;

public final class AliasObject implements Aliasable {
	
	private String alias;
	private Object object;
	
	public AliasObject(String alias, Object object) {
		this.alias = alias;
		this.object = object;
	}

//	@Override
//	public String getAlias() {
//		return this.alias;
//	}

	@Override
	public Object getData() {
		return this.object;
	}

	@Override
	public String toString() {
		return this.alias;
	}
	
}
