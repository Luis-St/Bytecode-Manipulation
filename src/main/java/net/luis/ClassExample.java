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
	
	public String someMethod(int index, String value) {
		return this.someInternalMethod(index, value, false, new ArrayList<>());
	}
	
	public String someMethod(int index, String value, boolean flag) {
		return this.someInternalMethod(index, value, flag, new ArrayList<>());
	}
	
	public String someMethod(int index, String value, boolean flag, String[] values) {
		return this.someInternalMethod(index, value, flag, Arrays.asList(values));
	}
	
	public String someMethod(int index, String value, boolean flag, List<String> values) {
		return this.someInternalMethod(index, value, flag, values);
	}
	
	private String someInternalMethod(int index, String value, boolean flag, List<String> values) {
		Objects.requireNonNull(value, "Value cannot be null");
		System.out.println("Some internal method");
		return "";
	}
	
	protected abstract void printName();
}
