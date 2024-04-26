package net.luis.preload.data;

import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public record FieldData(String name, Type type, String signature, TypeAccess access, Set<TypeModifier> modifiers, Map<Type, AnnotationData> annotations, /*Nullable*/ Object initialValue) implements ASMData {}
