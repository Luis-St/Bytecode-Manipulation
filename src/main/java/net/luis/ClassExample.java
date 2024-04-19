package net.luis;

import java.util.AbstractList;
import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public abstract class ClassExample extends AbstractList<String> implements List<String> {
	
	private String name;
	
	protected ClassExample(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) throws IllegalArgumentException {
		this.name = name;
	}
	
	protected abstract void printName();
}
