package net.luis;

import org.jetbrains.annotations.NotNull;

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
	
	protected ClassExample(@NotNull String name) {
		this.name = name;
	}
	
	public @NotNull String getName() {
		return this.name;
	}
	
	public @NotNull String getTestName() {
		return Objects.requireNonNull(this.name, "Name cannot be null");
	}
	
	public void setName(@NotNull String name) throws IllegalArgumentException {
		if (name == null) {
			throw new NullPointerException("Name cannot be null");
		}
		this.name = name;
	}
	
	public String someMethod(int index, @NotNull String value) {
		if (value == null) {
			throw new NullPointerException("Value cannot be null");
		}
		return this.someInternalMethod(index, value, false, new ArrayList<>());
	}
	
	public String someMethod(int index, @NotNull String value, boolean flag) {
		return this.someInternalMethod(index, value, flag, new ArrayList<>());
	}
	
	public String someMethod(int index, @NotNull String value, boolean flag, String @NotNull [] values) {
		return this.someInternalMethod(index, value, flag, Arrays.asList(values));
	}
	
	public String someMethod(int index, @NotNull String value, boolean flag, @NotNull List<String> values) {
		return this.someInternalMethod(index, value, flag, values);
	}
	
	private String someInternalMethod(int index, @NotNull String value, boolean flag, @NotNull List<String> values) {
		Objects.requireNonNull(value, "Value cannot be null");
		System.out.println("Some internal method");
		return "";
	}
	
	protected abstract void printName();
}
