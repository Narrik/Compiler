package sem;

import java.util.HashMap;
import java.util.Map;

public class Scope {
	private Scope outer;
	private Map<String, Symbol> symbolTable;
	
	public Scope(Scope outer) { 
		this.outer = outer;
		this.symbolTable = new HashMap<>();
	}
	
	public Scope() {
		this.outer = null;
		this.symbolTable = new HashMap<>();
	}
	
	public Symbol lookup(String name) {
		Symbol curSymbol = lookupCurrent(name);
		if (curSymbol != null)
			return curSymbol;
		if (outer != null) {
			return outer.lookup(name);
		}
		return null;
	}
	
	public Symbol lookupCurrent(String name) {
		return symbolTable.get(name);
	}
	
	public void put(Symbol sym) {
		symbolTable.put(sym.name, sym);
	}
}
