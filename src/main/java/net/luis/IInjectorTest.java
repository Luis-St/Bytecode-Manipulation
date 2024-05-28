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
	
	//region ASSIGN
	@Injector(method = "test(int)", target = @Target(value = "#i", type = TargetType.ASSIGN))
	default void injectAssignFieldByName() {
		System.out.println("Assign Field by Name");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "InjectorTest#i", type = TargetType.ASSIGN, mode = TargetMode.AFTER))
	default void injectAssignFieldByType() {
		System.out.println("Assign Field by Type#Name");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "index", type = TargetType.ASSIGN/* , mode = TargetMode.AFTER*/)) // ToDo: modify placement
	default void injectAssignParameterByName() {
		System.out.println("Assign Parameter by Name");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "1", type = TargetType.ASSIGN, ordinal = 1))
	default void injectAssignParameterByIndex() {
		System.out.println("Assign Parameter by Index");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "5", type = TargetType.ASSIGN))
	default void injectAssignVariableByName() {
		System.out.println("Assign Variable by Name");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "i", type = TargetType.ASSIGN, ordinal = 1))
	default void injectAssignVariableByIndex() {
		System.out.println("Assign Variable by Index");
	}
	//endregion
	
	//region ASSIGN_ARRAY
	@Injector(method = "test(int)", target = @Target(type = TargetType.ASSIGN_ARRAY, ordinal = 11))
	default void injectAssignArray() {
		System.out.println("Assign array");
	}
	//endregion
	
	//region CONSTANT
	@Injector(method = "test(int)", target = @Target(value = "String ", type = TargetType.CONSTANT))
	default void injectConstantString() {
		System.out.println("Constant with Value 'String '");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "100", type = TargetType.CONSTANT))
	default void injectConstant100() {
		System.out.println("Constant with Value '100'");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "0", type = TargetType.CONSTANT, ordinal = 3))
	default void injectConstant0() {
		System.out.println("Constant with Value '0'");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "null", type = TargetType.CONSTANT))
	default void injectConstantNull() {
		System.out.println("Constant with Value 'null'");
	}
	//endregion
	
	//region NUMERIC_OPERAND
	@Injector(method = "test(int)", target = @Target(value = "+", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandAdd() {
		System.out.println("Numeric Operand Add");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "-", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandSub() {
		System.out.println("Numeric Operand Sub");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "*", type = TargetType.NUMERIC_OPERAND, ordinal = 1))
	default void injectNumericOperandMul() {
		System.out.println("Numeric Operand Mul");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "/", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandDiv() {
		System.out.println("Numeric Operand Div");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "%", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandRem() {
		System.out.println("Numeric Operand Rem");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "neg", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandNeg() {
		System.out.println("Numeric Operand Neg");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "&", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandAnd() {
		System.out.println("Numeric Operand And");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "|", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandOr() {
		System.out.println("Numeric Operand Or");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "^", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandXor() {
		System.out.println("Numeric Operand Xor");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "<<", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandShl() {
		System.out.println("Numeric Operand Shl");
	}
	
	@Injector(method = "test(int)", target = @Target(value = ">>", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandShr() {
		System.out.println("Numeric Operand Shr");
	}
	
	@Injector(method = "test(int)", target = @Target(value = ">>>", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandUshr() {
		System.out.println("Numeric Operand Ushr");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "~", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandNot() {
		System.out.println("Numeric Operand Not");
	}
	//endregion
	
	//region COMPARE
	@Injector(method = "test(int)", target = @Target(value = "==", type = TargetType.COMPARE))
	default void injectCompareEqual() {
		System.out.println("Compare Equal");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "!=", type = TargetType.COMPARE))
	default void injectCompareNotEqual() {
		System.out.println("Compare Not Equal");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "<", type = TargetType.COMPARE)) // ToDo: Not targetable
	default void injectCompareLess() {
		System.out.println("Compare Less");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "<=", type = TargetType.COMPARE))
	default void injectCompareLessEqual() {
		System.out.println("Compare Less Equal");
	}
	
	@Injector(method = "test(int)", target = @Target(value = ">", type = TargetType.COMPARE))
	default void injectCompareGreater() {
		System.out.println("Compare Greater");
	}
	
	@Injector(method = "test(int)", target = @Target(value = ">=", type = TargetType.COMPARE))
	default void injectCompareGreaterEqual() {
		System.out.println("Compare Greater Equal");
	}
	
	@Injector(method = "test(int)", target = @Target(value = "instanceof", type = TargetType.COMPARE))
	default void injectCompareInstanceOf() {
		System.out.println("Compare Instance Of");
	}
	//endregion
	
	//region RETURN
	@Injector(method = "test(int)", target = @Target(type = TargetType.RETURN))
	default void injectReturn() { // ToDo: Break everything
		System.out.println("Return");
	}
	//endregion
}
