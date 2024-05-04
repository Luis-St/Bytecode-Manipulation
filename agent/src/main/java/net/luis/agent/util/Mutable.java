package net.luis.agent.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * @author Luis-St
 *
 */

public class Mutable<T> implements Supplier<T>, Consumer<T> {
	
	private T value;
	
	public Mutable() {
		this(null);
	}
	
	public Mutable(@Nullable T value) {
		this.value = value;
	}
	
	public boolean isPresent() {
		return this.value != null;
	}
	
	@Override
	public void accept(@Nullable T t) {
		this.value = t;
	}
	
	@Override
	public @Nullable T get() {
		return this.value;
	}
}
