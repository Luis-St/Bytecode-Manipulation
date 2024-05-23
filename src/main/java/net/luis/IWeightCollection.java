package net.luis;

import net.luis.agent.annotation.implementation.InjectInterface;
import net.luis.agent.annotation.implementation.Injector;
import net.luis.utils.collection.WeightCollection;

@InjectInterface(targets = WeightCollection.class)
public interface IWeightCollection {

	@Injector(target = "NavigableMap#put(Object, Object)")
	default boolean injectAdd() {
		System.out.println("Injecting add cancellation in WeightCollection#add");
		return true;
	}
}
