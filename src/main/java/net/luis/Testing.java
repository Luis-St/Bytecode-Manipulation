package net.luis;

import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.*;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class Testing {
	
	public static void main(String[] args) {
	
/*		Map<String, GenericDeclaration> generics0 = readGenericDeclarations("<X:Ljava/lang/Object;Y::Ljava/util/List<TX;>;>Ljava/lang/Object;Ljava/util/function/Function<TX;Ljava/lang/String;>;");
		Map<String, GenericDeclaration> generics1 = readGenericDeclarations("<X:Ljava/lang/Object;Y::Ljava/util/List<[TX;>;>Ljava/lang/Object;Ljava/util/function/Function<TX;Ljava/lang/String;>;");
		Map<String, GenericDeclaration> generics2 = readGenericDeclarations("<X:Ljava/lang/Object;Y::Ljava/util/List<Ljava/util/Map<Ljava/lang/String;TX;>;>;>Ljava/lang/Object;Ljava/util/function/Function<TX;Ljava/lang/String;>;");
		Map<String, GenericDeclaration> generics3 = readGenericDeclarations("<X:Ljava/lang/Object;Y::Ljava/util/List<-Ljava/lang/String;>;>Ljava/lang/Object;Ljava/util/function/Function<TX;Ljava/lang/String;>;");
		
		System.out.println(generics0);
		System.out.println(generics1);
		System.out.println(generics2);
		System.out.println(generics3);*/
		
		// class Test<X extends Number, Y extends List<X>> {}
		String classSignature0 = "<X::Ljava/lang/Number;Y::Ljava/util/List<TX;>;>Ljava/lang/Object;";
		Map<String, GenericDeclaration> generics0 = readGenericDeclarations(classSignature0);
		
		// class Test<X extends Number, Y extends List<X[]>> {}
		String classSignature1 = "<X::Ljava/lang/Number;Y::Ljava/util/List<[TX;>;>Ljava/lang/Object;";
		Map<String, GenericDeclaration> generics1 = readGenericDeclarations(classSignature1);
		
		// class Test<X extends Number, Y extends Function<X, String> {}
		String classSignature2 = "<X::Ljava/lang/Number;Y::Ljava/util/function/Function<TX;Ljava/lang/String;>;>Ljava/lang/Object;";
		Map<String, GenericDeclaration> generics2 = readGenericDeclarations(classSignature2);
		
		// class Test<X extends Number, Y extends Function<? super X, String> {}
		String classSignature3 = "<X::Ljava/lang/Number;Y::Ljava/util/function/Function<-TX;Ljava/lang/String;>;>Ljava/lang/Object;";
		Map<String, GenericDeclaration> generics3 = readGenericDeclarations(classSignature3);
		
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
	
	public static class ActualType {
		
		private final Type type;
		private final List<ActualType> nested;
		
		private ActualType(@NotNull Type type, @NotNull List<ActualType> nested) {
			this.type = type;
			this.nested = nested;
		}
		
		//region Factory methods
		public static @NotNull ActualType of(@NotNull Type type) {
			return new ActualType(type, new LinkedList<>());
		}
		
		public static @NotNull ActualType of(@NotNull Type type, @NotNull List<ActualType> nested) {
			return new ActualType(type, nested);
		}
		
		public static @NotNull ActualType flatDown(@NotNull ActualType declaration) {
			return flatDown("", declaration);
		}
		
		public static @NotNull ActualType flatDown(@NotNull String pre, @NotNull ActualType declaration) {
			return new ActualType(Type.getType(pre + declaration.type().getDescriptor()), new LinkedList<>());
		}
		//endregion
		
		public @NotNull Type type() {
			return this.type;
		}
		
		@Unmodifiable
		public @NotNull List<ActualType> nested() {
			return List.copyOf(this.nested);
		}
	}
	
	public static @NotNull List<ActualType> parseSignatureParameters(@NotNull Map<String, GenericDeclaration> classGenerics, @NotNull String signature) {
		Map<String, GenericDeclaration> generics = new HashMap<>(classGenerics);
		generics.putAll(readGenericDeclarations(signature));
		
		List<ActualType> types = new LinkedList<>();
		for (String parameter : readSignatureParameters(signature)) {
			types.add(parseSignatureParameter(generics, parameter, false));
		}
		return types;
	}
	
	public static @NotNull ActualType parseSignatureParameter(@NotNull Map<String, GenericDeclaration> generics, @NotNull String parameter, boolean escaped) {
		if (!parameter.contains("<") && !parameter.contains(">")) {
			if (parameter.charAt(0) == '[') {
				int dimensions = 0;
				while (parameter.charAt(dimensions) == '[') {
					dimensions++;
				}
				return ActualType.flatDown("[".repeat(dimensions), parseSignatureParameter(generics, parameter.substring(dimensions), false));
			} else if (parameter.charAt(0) == '-' || parameter.charAt(0) == '+') {
				return ActualType.flatDown(parseSignatureParameter(generics, parameter.substring(1), false));
			} else if (parameter.charAt(0) == 'T') {
				String name = parameter.substring(1, parameter.length() - 1);
				GenericDeclaration generic = generics.get(name);
				if (generic == null) {
					throw new IllegalArgumentException("Found generic parameter which was not previously declared: '" + name + "'");
				}
				return parseGenericSignatureParameter(generic);
			} else if (parameter.charAt(0) == 'L') {
				return ActualType.of(Type.getType(parameter));
			}
			throw new IllegalArgumentException("Unknown parameter declaration: '" + parameter + "'");
		}
		String type = new ScopedStringReader(parameter).readUntil('<') + ";";
		String signature = parameter.substring(type.length(), parameter.length() - 2);
		
		ScopedStringReader reader = new ScopedStringReader(escaped ? signature : signature.replace("[", "\\["));
		List<ActualType> nested = new LinkedList<>();
		while (reader.canRead()) {
			nested.add(parseSignatureParameter(generics, reader.readUntil(';') + ";", true));
		}
		return ActualType.of(Type.getType(type), nested);
	}
	
	public static @NotNull ActualType parseGenericSignatureParameter(@NotNull GenericDeclaration generic) {
		if (!(generic instanceof NestedGenericDeclaration nestedGeneric)) {
			return ActualType.of(generic.type());
		}
		Type type = generic.type();
		List<ActualType> nested = new LinkedList<>();
		for (GenericDeclaration declaration : nestedGeneric.nested()) {
			nested.add(parseGenericSignatureParameter(declaration));
		}
		return ActualType.of(type, nested);
	}
	
	public static @NotNull List<String> readSignatureParameters(@NotNull String signature) {
		List<String> parameters = new ArrayList<>();
		if (signature.isBlank()) {
			return parameters;
		}
		ScopedStringReader reader = new ScopedStringReader(signature);
		while (reader.canRead() && reader.peek() != '(') {
			reader.skip();
		}
		ScopedStringReader inner = new ScopedStringReader(reader.readScope(ScopedStringReader.PARENTHESES));
		inner.skip();
		while (inner.canRead() && inner.peek() != ')') {
			parameters.add(inner.readUntil(';') + ";");
		}
		return parameters;
	}
	
	// public class SignatureTest<X, Y extends List<X>> implements Function<X, String>
	//   <X:Ljava/lang/Object;Y::Ljava/util/List<TX;>;>Ljava/lang/Object;Ljava/util/function/Function<TX;Ljava/lang/String;>;
	
	// public class SignatureTest<X, Y extends List<X[]>> implements Function<X, String>
	//   <X:Ljava/lang/Object;Y::Ljava/util/List<[TX;>;>Ljava/lang/Object;Ljava/util/function/Function<TX;Ljava/lang/String;>;
	
	// public class SignatureTest<X, Y extends List<Map<String, X>> implements Function<X, String>
	//   <X:Ljava/lang/Object;Y::Ljava/util/List<Ljava/util/Map<Ljava/lang/String;TX;>;>;>Ljava/lang/Object;Ljava/util/function/Function<TX;Ljava/lang/String;>;
	
	// public class SignatureTest<X, Y extends List<? super String>> implements Function<X, String>
	//   <X:Ljava/lang/Object;Y::Ljava/util/List<-Ljava/lang/String;>;>Ljava/lang/Object;Ljava/util/function/Function<TX;Ljava/lang/String;>;
	
	public static @NotNull Map<String, GenericDeclaration> readGenericDeclarations(@NotNull String signature) {
		Map<String, GenericDeclaration> generics = new HashMap<>();
		if (signature.isBlank() || signature.charAt(0) != '<') {
			return generics;
		}
		String declaration = new ScopedStringReader(signature).readScope(ScopedStringReader.ANGLE_BRACKETS);
		ScopedStringReader reader = new ScopedStringReader(declaration.replace("[", "\\["));
		reader.skip();
		while (reader.canRead()) {
			if (reader.peek() == '>') {
				break;
			}
			if (reader.peek() == '-') {
				continue;
			}
			String genericName = reader.readUntil(':');
			if (reader.peek() == ':') {
				reader.skip();
			}
			generics.put(genericName, readGenericDeclaration(genericName, generics, reader.readUntil(';') + ";"));
		}
		return generics;
	}
	
	private static @NotNull GenericDeclaration readGenericDeclaration(@NotNull String genericName, @NotNull Map<String, GenericDeclaration> generics, @NotNull String signature) {
		if (!signature.contains("<") && !signature.contains(">")) {
			return new GenericParameterDeclaration(genericName, Type.getType(signature), null);
		}
		ScopedStringReader reader = new ScopedStringReader(signature.substring(0, signature.length() - 2).replace("[", "\\["));
		NestedGenericDeclaration declaration = new NestedGenericDeclaration(Type.getType(reader.readUntil('<') + ";"));
		while (reader.canRead()) {
			declaration.addNested(parseGenericDeclaration(generics, reader.readUntil(';') + ";"));
		}
		return declaration;
	}
	
	private static @NotNull GenericDeclaration parseGenericDeclaration(@NotNull Map<String, GenericDeclaration> generics, @NotNull String part) {
		if (part.charAt(0) == '-') {
			return parseGenericDeclaration(generics, part.substring(1));
		} else if (part.charAt(0) == '[') {
			int dimensions = 0;
			while (part.charAt(dimensions) == '[') {
				dimensions++;
			}
			Type inner = parseGenericDeclaration(generics, part.substring(dimensions)).type();
			return new GenericTypeDeclaration(Type.getType("[".repeat(dimensions) + inner.getDescriptor()));
		} else if (part.charAt(0) == 'T') {
			String name = part.substring(1, part.length() - 1);
			GenericDeclaration generic = generics.get(name);
			if (generic == null) {
				throw new IllegalArgumentException("Found generic parameter which was not previously declared: '" + name + "'");
			}
			return new GenericParameterDeclaration(name, generic.type(), generic);
		} else if (part.charAt(0) == 'L') {
			if (!part.contains("<") && !part.contains(">")) {
				return new GenericTypeDeclaration(Type.getType(part));
			}
			throw new UnsupportedOperationException("Nested generic types are not supported: '" + part + "'");
		} else {
			throw new IllegalArgumentException("Invalid generic signature part: '" + part + "'");
		}
	}
	
	public static interface GenericDeclaration {
		
		@NotNull Type type();
	}
	
	public record GenericTypeDeclaration(Type type) implements GenericDeclaration {}
	
	public static class GenericParameterDeclaration implements GenericDeclaration {
		
		private final String genericName;
		private final Type type;
		private final GenericDeclaration generic;
		
		private GenericParameterDeclaration(@NotNull String genericName, @NotNull Type type, @Nullable GenericDeclaration generic) {
			this.genericName = genericName;
			this.type = type;
			this.generic = generic;
		}
		
		public @NotNull String genericName() {
			return this.genericName;
		}
		
		public @NotNull Type type() {
			return this.type;
		}
		
		public @Nullable GenericDeclaration generic() {
			return this.generic;
		}
	}
	
	public static class NestedGenericDeclaration implements GenericDeclaration {
		
		private final List<GenericDeclaration> nested = new LinkedList<>();
		private final Type type;
		
		public NestedGenericDeclaration(@NotNull Type type) {
			this.type = type;
		}
		
		public @NotNull Type type() {
			return this.type;
		}
		
		public void addNested(@NotNull GenericDeclaration nested) {
			this.nested.add(nested);
		}
		
		@Unmodifiable
		public @NotNull List<GenericDeclaration> nested() {
			return List.copyOf(this.nested);
		}
	}
}
