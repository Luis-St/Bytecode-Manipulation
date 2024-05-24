package net.luis;

import net.luis.agent.annotation.implementation.InjectInterface;
import net.luis.agent.annotation.implementation.Injector;
import net.luis.utils.collection.WeightCollection;

@InjectInterface(targets = WeightCollection.class)
public interface IWeightCollection {
	
	@Injector(method = "<init>(Random)", target = "Maps#newTreeMap()")
	default void injectConstructor() {
		System.out.println("Injecting constructor cancellation in WeightCollection#<init>");
	}
	
	@Injector(target = "NavigableMap#put(Object, Object)")
	default boolean injectAdd() {
		System.out.println("Injecting add cancellation in WeightCollection#add");
		return true;
	}
	
	@Injector(target = "IllegalStateException#<init>(String)")
	default void injectNext() {
		System.out.println("Injecting next cancellation in WeightCollection#next");
	}
}
