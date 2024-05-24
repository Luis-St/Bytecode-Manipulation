package net.luis;

import net.luis.agent.annotation.implementation.InjectInterface;
import net.luis.agent.annotation.implementation.Injector;
import net.luis.agent.annotation.util.Target;
import net.luis.agent.util.TargetType;
import net.luis.utils.collection.WeightCollection;

@InjectInterface(targets = WeightCollection.class)
public interface IWeightCollection {
	
	@Injector(method = "<init>(Random)",
		target = @Target(value = "Maps#newTreeMap()", type = TargetType.INVOKE)
	)
	default void injectConstructor() {
		System.out.println("Injecting constructor cancellation in WeightCollection#<init>");
	}
	
	@Injector(target = @Target(value = "NavigableMap#put(Object, Object)", type = TargetType.INVOKE))
	default boolean injectAdd() {
		System.out.println("Injecting add cancellation in WeightCollection#add");
		return true;
	}
	
	@Injector(target = @Target(value = "IllegalStateException#<init>(String)", type = TargetType.INVOKE))
	default void injectNext() {
		System.out.println("Injecting next cancellation in WeightCollection#next");
	}
}
