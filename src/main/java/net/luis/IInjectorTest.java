package net.luis;

import net.luis.agent.annotation.implementation.Inject;
import net.luis.agent.annotation.implementation.InjectInterface;
import net.luis.agent.annotation.util.*;
import net.luis.agent.util.TargetMode;
import net.luis.agent.util.TargetType;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

@InjectInterface(target = "net.luis.InjectorTest")
public interface IInjectorTest {
	
	//region HEAD
	@Inject(method = "test(int, int[])", target = @Target(type = TargetType.HEAD), restricted = false)
	static void injectHead(@Local int index) {
		System.out.println("Head");
	}
	//endregion
	
	//region NEW
	@Inject(method = "test(int, int[])", target = @Target(value = "ArrayList", type = TargetType.NEW))
	default void injectNewType(@This InjectorTest test) {
		System.out.println("New Type");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "int[]", type = TargetType.NEW))
	default void injectNewArrayType() {
		System.out.println("New Array Type");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "int[][]", type = TargetType.NEW))
	default void injectNewMultiArrayType() {
		System.out.println("New Multi Array Type");
	}
	//endregion
	
	//region INVOKE
	@Inject(method = "test(int, int[])", target = @Target(value = "InjectorTest#validate", type = TargetType.INVOKE))
	default void injectInvokePrivate() {
		System.out.println("Invoke Private");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "InjectorTest#silentThrow", type = TargetType.INVOKE))
	default void injectInvokeStatic() {
		System.out.println("Invoke Private");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "List#add", type = TargetType.INVOKE, mode = TargetMode.AFTER, ordinal = 2))
	default void injectInvokeInstance(@Local List<Object> list, @Local int[] array, @Local int[][] multiArray) {
		System.out.println("Invoke Private");
	}
	//endregion
	
	//region ACCESS
	@Inject(method = "test(int, int[])", target = @Target(value = "#i", type = TargetType.ACCESS))
	default void injectAccessFieldByName() {
		System.out.println("Access Field by Name");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "InjectorTest#i", type = TargetType.ACCESS, mode = TargetMode.AFTER))
	default void injectAccessFieldByType() {
		System.out.println("Access Field by Type#Name");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "index", type = TargetType.ACCESS))
	default void injectAccessParameterByName() {
		System.out.println("Access Parameter by Name");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "1", type = TargetType.ACCESS))
	default void injectAccessParameterByIndex() {
		System.out.println("Access Parameter by Index");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "i", type = TargetType.ACCESS, ordinal = 3))
	default void injectAccessVariableByName(@Local int i) {
		System.out.println("Access Variable by Name");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "6", type = TargetType.ACCESS))
	default void injectAccessVariableByIndex() {
		System.out.println("Access Variable by Index");
	}
	//endregion
	
	//region ACCESS_ARRAY
	@Inject(method = "test(int, int[])", target = @Target(type = TargetType.ACCESS_ARRAY))
	default void injectAccessArray() {
		System.out.println("Access Array");
	}
	//endregion
	
	//region ASSIGN
	@Inject(method = "test(int, int[])", target = @Target(value = "#i", type = TargetType.ASSIGN))
	default void injectAssignFieldByName() {
		System.out.println("Assign Field by Name");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "InjectorTest#i", type = TargetType.ASSIGN, mode = TargetMode.AFTER))
	default void injectAssignFieldByType() {
		System.out.println("Assign Field by Type#Name");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "index", type = TargetType.ASSIGN, mode = TargetMode.AFTER))
	default void injectAssignParameterByName() {
		System.out.println("Assign Parameter by Name");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "1", type = TargetType.ASSIGN, ordinal = 1))
	default void injectAssignParameterByIndex() {
		System.out.println("Assign Parameter by Index");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "i", type = TargetType.ASSIGN, ordinal = 1))
	default void injectAssignVariableByName() {
		System.out.println("Assign Variable by Name");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "6", type = TargetType.ASSIGN))
	default void injectAssignVariableByIndex() {
		System.out.println("Assign Variable by Index");
	}
	//endregion
	
	//region ASSIGN_ARRAY
	@Inject(method = "test(int, int[])", target = @Target(type = TargetType.ASSIGN_ARRAY, ordinal = 11))
	default void injectAssignArray() {
		System.out.println("Assign Array");
	}
	//endregion
	
	//region CONSTANT
	@Inject(method = "test(int, int[])", target = @Target(value = "100", type = TargetType.CONSTANT))
	default void injectConstant100() {
		System.out.println("Constant with Value '100'");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "0", type = TargetType.CONSTANT, ordinal = 3))
	default void injectConstant0() {
		System.out.println("Constant with Value '0'");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "null", type = TargetType.CONSTANT))
	default void injectConstantNull() {
		System.out.println("Constant with Value 'null'");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "true", type = TargetType.CONSTANT))
	default void injectConstantTrue() {
		System.out.println("Constant with Value 'true'");
	}
	//endregion
	
	//region STRING
	@Inject(method = "test(int, int[])", target = @Target(value = "String ", type = TargetType.STRING))
	default void injectString() {
		System.out.println("String with Value 'String '");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "null", type = TargetType.STRING))
	default void injectStringNull() {
		System.out.println("String with Value 'null'");
	}
	//endregion
	
	//region NUMERIC_OPERAND
	@Inject(method = "test(int, int[])", target = @Target(value = "+", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandAdd() {
		System.out.println("Numeric Operand Add");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "-", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandSub() {
		System.out.println("Numeric Operand Sub");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "*", type = TargetType.NUMERIC_OPERAND, ordinal = 1))
	default void injectNumericOperandMul() {
		System.out.println("Numeric Operand Mul");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "/", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandDiv() {
		System.out.println("Numeric Operand Div");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "%", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandRem() {
		System.out.println("Numeric Operand Rem");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "neg", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandNeg() {
		System.out.println("Numeric Operand Neg");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "&", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandAnd() {
		System.out.println("Numeric Operand And");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "|", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandOr() {
		System.out.println("Numeric Operand Or");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "^", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandXor() {
		System.out.println("Numeric Operand Xor");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "<<", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandShl() {
		System.out.println("Numeric Operand Shl");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = ">>", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandShr() {
		System.out.println("Numeric Operand Shr");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = ">>>", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandUshr() {
		System.out.println("Numeric Operand Ushr");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "~", type = TargetType.NUMERIC_OPERAND))
	default void injectNumericOperandNot() {
		System.out.println("Numeric Operand Not");
	}
	//endregion
	
	//region COMPARE
	@Inject(method = "test(int, int[])", target = @Target(value = "==", type = TargetType.COMPARE))
	default void injectCompareEqual() {
		System.out.println("Compare Equal");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "!=", type = TargetType.COMPARE))
	default void injectCompareNotEqual() {
		System.out.println("Compare Not Equal");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "<", type = TargetType.COMPARE))
	default void injectCompareLess() {
		System.out.println("Compare Less");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "<=", type = TargetType.COMPARE))
	default void injectCompareLessEqual() {
		System.out.println("Compare Less Equal");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = ">", type = TargetType.COMPARE))
	default void injectCompareGreater() {
		System.out.println("Compare Greater");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = ">=", type = TargetType.COMPARE))
	default void injectCompareGreaterEqual() {
		System.out.println("Compare Greater Equal");
	}
	
	@Inject(method = "test(int, int[])", target = @Target(value = "instanceof", type = TargetType.COMPARE))
	default void injectCompareInstanceOf() {
		System.out.println("Compare Instance Of");
	}
	//endregion
	
	//region RETURN
	@Inject(method = "test(int, int[])", target = @Target(type = TargetType.RETURN))
	default void injectReturn() {
		System.out.println("Return");
	}
	//endregion
	
	//region LAMBDA
	@Inject(method = "test(int, int[])", target = @Target(value = "System#getProperty", type = TargetType.INVOKE))
	static void injectLambdaTest() {
		System.out.println("Lambda");
	}
	//endregion
}
