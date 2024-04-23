package net.luis.preload.data;

import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public record FieldData(String name, Type type, String signature, TypeAccess access, List<TypeModifier> modifiers, Map<Type, AnnotationData> annotations, /*Nullable*/ Object initialValue) implements ASMData {}
