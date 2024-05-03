package net.luis;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public abstract class ClassExample extends AbstractList<String> implements List<String> {
	
	private final List<String> list = new AbstractList<String>() {
		@Override
		public String get(int index) {
			return null;
		}
		
		@Override
		public int size() {
			return 0;
		}
	};
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
	
	public void someMethod(int index, String value) {
		this.someInternalMethod(index, value, false, new ArrayList<>());
	}
	
	public void someMethod(int index, String value, boolean flag) {
		this.someInternalMethod(index, value, flag, new ArrayList<>());
	}
	
	public void someMethod(int index, String value, boolean flag, List<String> values) {
		this.someInternalMethod(index, value, flag, values);
	}
	
	private void someInternalMethod(int index, String value, boolean flag, List<String> values) {
		System.out.println("Some internal method");
	}
	
	protected abstract void printName();
}
