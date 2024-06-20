package net.luis;

import net.luis.agent.annotation.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class LazyTest {
	
	@Lazy protected static final List<String> NAMES = List.of("Luis", "St");
	@Lazy protected static final int MAX = 100;
	
	@Lazy private final String name;
	@Lazy private final int age;
	//private final Supplier<String> supplier;
	
	public LazyTest(String name, int age) {
		this.name = name;
		this.age = age;
		//this.supplier = () -> name;
	}
	
/*	public static @NotNull List<String> getNames() {
		return NAMES;
	}*/
	
	public @NotNull String getInstanceName() {
		return this.name;
	}
	
	public int getAge() {
		return this.age;
	}
	
	@Override
	public String toString() {
		return this.name + " " + this.age;
	}
}
