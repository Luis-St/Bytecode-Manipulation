package net.luis.test;

import net.luis.agent.annotation.instrumentation.InjectInterface;
import net.luis.agent.annotation.instrumentation.Modificator;
import net.luis.agent.annotation.util.*;
import net.luis.agent.util.ModifyTarget;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

@InjectInterface(ModifyTest.class)
public interface IModifyTest {
	
	@Modificator(target = ModifyTarget.PARAMETER, value = "0")
	default int modifyCalculate() {
		return 1;
	}
	
	@Modificator(method = "calculate", target = ModifyTarget.PARAMETER, value = "0", priority = 100)
	default int modifyCalculateSecond() {
		return 2;
	}
	
	@Modificator(method = "calculate", target = ModifyTarget.PARAMETER, value = "values")
	default @NotNull List<Integer> modifyValues(@Original @NotNull List<Integer> values, @This ModifyTest instance) {
		return List.of(1, 2, 3);
	}
	
	@Modificator(method = "calculate", target = ModifyTarget.FIELD, value = "ModifyTest#result", ordinal = 0)
	default int modifyResult(@Original int result) {
		return (int) Math.pow(result, 2);
	}
	
	@Modificator(method = "calculate", target = ModifyTarget.CONSTANT, value = "10")
	default byte modifyRadix() {
		return 9;
	}
	
	@Modificator(target = ModifyTarget.RETURN)
	default int modificatorCalculate(@Original int value, @Local(value = "r") int radix) {
		return value % radix;
	}
	
	@Modificator(method = "calculate", target = ModifyTarget.CONSTANT, value = "Invalid radix: ${\0}")
	default @NotNull String replaceExceptionMessage(@Original @NotNull String message) {
		return message.replace("Invalid radix", "Invalid base");
	}
	
	@Modificator(method = "calculate", target = ModifyTarget.CONSTANT, value = "Integer.class")
	static @NotNull Class<?> replaceClass(@Original @NotNull Class<?> clazz) {
		return clazz == Integer.class ? String.class : clazz;
	}
}
