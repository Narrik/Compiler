package sem;

public abstract class Symbol {
	public String name;

	public boolean isVar(){
		if (this instanceof VarSymbol){
			return true;
		}
		return false;
	}

	public boolean isFun(){
		if (this instanceof FunSymbol){
			return true;
		}
		return false;
	}

	public Symbol(String name) {
		this.name = name;
	}
}
