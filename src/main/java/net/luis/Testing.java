package net.luis;

import net.luis.agent.asm.signature.ActualType;
import net.luis.agent.asm.signature.GenericDeclaration;
import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.*;
import org.objectweb.asm.Type;

import java.util.*;

import static net.luis.agent.asm.signature.SignatureUtils.*;

/**
 *
 * @author Luis-St
 *
 */

public class Testing {
	
	public static void main(String[] args) {
		// class Test<X extends Number, Y extends List<X>> {}
		String classSignature0 = "<X::Ljava/lang/Number;Y::Ljava/util/List<TX;>;>Ljava/lang/Object;";
		Map<String, GenericDeclaration> generics0 = parseGenericDeclarations(classSignature0);
		
		// class Test<X extends Number, Y extends List<X[]>> {}
		String classSignature1 = "<X::Ljava/lang/Number;Y::Ljava/util/List<[TX;>;>Ljava/lang/Object;";
		Map<String, GenericDeclaration> generics1 = parseGenericDeclarations(classSignature1);
		
		// class Test<X extends Number, Y extends Function<X, String> {}
		String classSignature2 = "<X::Ljava/lang/Number;Y::Ljava/util/function/Function<TX;Ljava/lang/String;>;>Ljava/lang/Object;";
		Map<String, GenericDeclaration> generics2 = parseGenericDeclarations(classSignature2);
		
		// class Test<X extends Number, Y extends Function<? super X, String> {}
		String classSignature3 = "<X::Ljava/lang/Number;Y::Ljava/util/function/Function<-TX;Ljava/lang/String;>;>Ljava/lang/Object;";
		Map<String, GenericDeclaration> generics3 = parseGenericDeclarations(classSignature3);
		
		// <Z extends Number> void modify(X number, Function<X, Z> modifier, Function<Z, X> reverse, BiConsumer<Z, Y> store)
		String methodSignature0 = "<Z::Ljava/lang/Number;>(TX;Ljava/util/function/Function<TX;TZ;>;Ljava/util/function/Function<TZ;TX;>;Ljava/util/function/BiConsumer<TZ;TY;>;)V";
		//parseSignatureParameters(generics, methodSignature0);
		
		// <Z extends Number> void modify(X number, Function<? super X, Z> modifier, Function<Z, X> reverse, BiConsumer<Z, Y> store)
		String methodSignature1 = "<Z::Ljava/lang/Number;>(TX;Ljava/util/function/Function<-TX;TZ;>;Ljava/util/function/Function<TZ;TX;>;Ljava/util/function/BiConsumer<TZ;TY;>;)V";
		
		// <Z extends Number> void modify(X number, Function<String, Z> modifier, Function<Z, String> reverse, BiConsumer<Z, Y> store)
		String methodSignature2 = "<Z::Ljava/lang/Number;>(TX;Ljava/util/function/Function<Ljava/lang/String;TZ;>;Ljava/util/function/Function<TZ;Ljava/lang/String;>;Ljava/util/function/BiConsumer<TZ;TY;>;)V";
		
		List<ActualType> types0 = parseSignatureParameters(generics0, methodSignature0);
		List<ActualType> types1 = parseSignatureParameters(generics1, methodSignature1);
		List<ActualType> types2 = parseSignatureParameters(generics2, methodSignature2);
		
		System.out.println(types0);
		System.out.println(types1);
		System.out.println(types2);
		
		System.out.println();
	}
}
