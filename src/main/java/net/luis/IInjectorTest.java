package net.luis;

import net.luis.agent.annotation.implementation.*;
import net.luis.agent.annotation.util.Target;
import net.luis.agent.util.TargetMode;
import net.luis.agent.util.TargetType;

/**
 *
 * @author Luis-St
 *
 */

@InjectInterface(targets = InjectorTest.class)
public interface IInjectorTest {
	
	//region HEAD
	@Injector(method = "test(int)", target = @Target(type = TargetType.HEAD))
	default void injectHead() {
		System.out.println("Head");
	}
	//endregion
	
	//region NEW
	@Injector(method = "test(int)", target = @Target(value = "ArrayList", type = TargetType.NEW))
	default void injectNewType() {
		System.out.println("New Type");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "int[]", type = TargetType.NEW))
	default void injectNewArrayType() {
		System.out.println("New Array Type");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "int[][]", type = TargetType.NEW))
	default void injectNewMultiArrayType() {
		System.out.println("New Multi Array Type");
	}
	//endregion
	
	//region INVOKE
	@Injector(method = "test(int)", target = @Target(value = "InjectorTest#validate", type = TargetType.INVOKE))
	default void injectInvokePrivate() {
		System.out.println("Invoke Private");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "InjectorTest#silentThrow", type = TargetType.INVOKE))
	default void injectInvokeStatic() {
		System.out.println("Invoke Private");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "List#add", type = TargetType.INVOKE, mode = TargetMode.AFTER, ordinal = 2))
	default void injectInvokeInstance() {
		System.out.println("Invoke Private");
	}
	//endregion
	
	//region ACCESS
	@Injector(method = "test(int)", target = @Target(value = "#i", type = TargetType.ACCESS))
	default void injectAccessFieldByName() {
		System.out.println("Access Field by Name");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "InjectorTest#i", type = TargetType.ACCESS, mode = TargetMode.AFTER))
	default void injectAccessFieldByType() {
		System.out.println("Access Field by Type#Name");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "index", type = TargetType.ACCESS))
	default void injectAccessParameterByName() {
		System.out.println("Access Parameter by Name");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "1", type = TargetType.ACCESS))
	default void injectAccessParameterByIndex() {
		System.out.println("Access Parameter by Index");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "5", type = TargetType.ACCESS))
	default void injectAccessVariableByName() {
		System.out.println("Access Variable by Name");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "i", type = TargetType.ACCESS, ordinal = 3))
	default void injectAccessVariableByIndex() {
		System.out.println("Access Variable by Index");
	}
	//endregion
	
	//region ACCESS_ARRAY
	@Injector(method = "test(int)", target = @Target(type = TargetType.ACCESS_ARRAY))
	default void injectAccessArray() {
		System.out.println("Access array");
	}
	//endregion
}
