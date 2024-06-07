package net.luis;

import net.luis.agent.annotation.implementation.InjectInterface;
import net.luis.agent.annotation.implementation.Inject;
import net.luis.agent.annotation.util.Target;
import net.luis.agent.util.TargetType;
import net.luis.utils.collection.WeightCollection;

@InjectInterface(WeightCollection.class)
public interface IWeightCollection {
	
	@Inject(method = "<init>(Random)", target = @Target(value = "Maps#newTreeMap()", type = TargetType.INVOKE))
	default void injectConstructor() {
		System.out.println("Injecting constructor cancellation in WeightCollection#<init>");
	}
	
	@Inject(target = @Target(value = "NavigableMap#put(Object, Object)", type = TargetType.INVOKE))
	default boolean injectAdd() {
		System.out.println("Injecting add cancellation in WeightCollection#add");
		return true;
	}
	
	@Inject(target = @Target(value = "IllegalStateException", type = TargetType.NEW))
	default void injectNext() {
		System.out.println("Injecting next cancellation in WeightCollection#next");
	}
}
