package net.luis.agent.asm.signature;

import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class SignatureUtils {
	
	//region Parsing signature
	public static @NotNull List<ActualType> parseSignatureParameters(@NotNull Map<String, GenericDeclaration> classGenerics, @NotNull String signature) {
		Map<String, GenericDeclaration> generics = new HashMap<>(classGenerics);
		generics.putAll(parseGenericDeclarations(signature));
		List<ActualType> types = new LinkedList<>();
		for (String parameter : parseSignatureParameters(signature)) {
			types.add(parseSignatureParameter(generics, parameter));
		}
		return types;
	}
	
	public static @NotNull List<String> parseSignatureParameters(@NotNull String signature) {
		List<String> parameters = new ArrayList<>();
		if (signature.isBlank()) {
			return parameters;
		}
		ScopedStringReader reader = new ScopedStringReader(signature);
		while (reader.canRead() && reader.peek() != '(') {
			reader.skip();
		}
		ScopedStringReader inner = new ScopedStringReader(reader.readScope(ScopedStringReader.PARENTHESES).replace("[", "\\["));
		inner.skip();
		while (inner.canRead() && inner.peek() != ')') {
			char c = inner.peek();
			// Handle primitive types (ZBCSIJFD) which don't end with ';'
			if (c == 'Z' || c == 'B' || c == 'C' || c == 'S' || c == 'I' || c == 'J' || c == 'F' || c == 'D' || c == 'V') {
				parameters.add(String.valueOf(inner.read()));
			} else if (c == '[') {
				// Handle arrays - read the '[' and then the component type
				StringBuilder arrayParam = new StringBuilder();
				while (inner.peek() == '[') {
					arrayParam.append(inner.read());
				}
				char component = inner.peek();
				if (component == 'Z' || component == 'B' || component == 'C' || component == 'S' || component == 'I' || component == 'J' || component == 'F' || component == 'D') {
					arrayParam.append(inner.read());
					parameters.add(arrayParam.toString());
				} else {
					// Reference type array
					arrayParam.append(inner.readUntil(';')).append(';');
					parameters.add(arrayParam.toString());
				}
			} else {
				// Reference types end with ';'
				parameters.add(inner.readUntil(';') + ";");
			}
		}
		return parameters;
	}
	
	public static @NotNull ActualType parseSignatureParameter(@NotNull Map<String, GenericDeclaration> generics, @NotNull String parameter) {
		return parseSignatureParameter(generics, parameter, false);
	}
	
	private static @NotNull ActualType parseSignatureParameter(@NotNull Map<String, GenericDeclaration> generics, @NotNull String parameter, boolean escaped) {
		if (!parameter.contains("<") && !parameter.contains(">")) {
			char first = parameter.charAt(0);
			if (first == '[') {
				int dimensions = 0;
				while (parameter.charAt(dimensions) == '[') {
					dimensions++;
				}
				return ActualType.flatDown("[".repeat(dimensions), parseSignatureParameter(generics, parameter.substring(dimensions), false));
			} else if (first == '-' || first == '+') {
				return ActualType.flatDown(parseSignatureParameter(generics, parameter.substring(1), false));
			} else if (first == 'T') {
				String name = parameter.substring(1, parameter.length() - 1);
				GenericDeclaration generic = generics.get(name);
				if (generic == null) {
					throw new IllegalArgumentException("Found generic parameter which was not previously declared: '" + name + "'");
				}
				return parseGenericSignatureParameter(generic);
			} else if (first == 'L') {
				return ActualType.of(Type.getType(parameter));
			} else if (first == 'Z' || first == 'B' || first == 'C' || first == 'S' || first == 'I' || first == 'J' || first == 'F' || first == 'D' || first == 'V') {
				// Handle primitive types
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
	
	private static @NotNull ActualType parseGenericSignatureParameter(@NotNull GenericDeclaration generic) {
		if (generic.nested().isEmpty()) {
			return ActualType.of(generic.type());
		}
		Type type = generic.type();
		List<ActualType> nested = new LinkedList<>();
		for (GenericDeclaration declaration : generic.nested()) {
			nested.add(parseGenericSignatureParameter(declaration));
		}
		return ActualType.of(type, nested);
	}
	//endregion
	
	//region Parsing generic declaration
	public static @NotNull Map<String, GenericDeclaration> parseGenericDeclarations(@NotNull String signature) {
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
			generics.put(genericName, parseGenericDeclaration(generics, reader.readUntil(';') + ";"));
		}
		return generics;
	}
	
	private static @NotNull GenericDeclaration parseGenericDeclaration(@NotNull Map<String, GenericDeclaration> generics, @NotNull String signature) {
		if (!signature.contains("<") && !signature.contains(">")) {
			return GenericDeclaration.of(Type.getType(signature));
		}
		ScopedStringReader reader = new ScopedStringReader(signature.substring(0, signature.length() - 2).replace("[", "\\["));
		Type type = Type.getType(reader.readUntil('<') + ";");
		List<GenericDeclaration> nested = new LinkedList<>();
		while (reader.canRead()) {
			nested.add(parseGenericDeclarationPart(generics, reader.readUntil(';') + ";"));
		}
		return GenericDeclaration.of(type, nested);
	}
	
	private static @NotNull GenericDeclaration parseGenericDeclarationPart(@NotNull Map<String, GenericDeclaration> generics, @NotNull String part) {
		if (part.charAt(0) == '-') {
			return parseGenericDeclarationPart(generics, part.substring(1));
		} else if (part.charAt(0) == '[') {
			int dimensions = 0;
			while (part.charAt(dimensions) == '[') {
				dimensions++;
			}
			Type inner = parseGenericDeclarationPart(generics, part.substring(dimensions)).type();
			return GenericDeclaration.of(Type.getType("[".repeat(dimensions) + inner.getDescriptor()));
		} else if (part.charAt(0) == 'T') {
			String name = part.substring(1, part.length() - 1);
			GenericDeclaration generic = generics.get(name);
			if (generic == null) {
				throw new IllegalArgumentException("Found generic parameter which was not previously declared: '" + name + "'");
			}
			return GenericDeclaration.of(generic.type());
		} else if (part.charAt(0) == 'L') {
			if (!part.contains("<") && !part.contains(">")) {
				return GenericDeclaration.of(Type.getType(part));
			}
			throw new UnsupportedOperationException("Nested generic types are not supported: '" + part + "'");
		} else {
			throw new IllegalArgumentException("Invalid generic signature part: '" + part + "'");
		}
	}
	//endregion
}
