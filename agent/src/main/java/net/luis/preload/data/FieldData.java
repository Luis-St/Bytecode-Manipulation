package net.luis.preload.data;

import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record FieldData(String name, Type type, /*Nullable*/ String signature, TypeAccess access, List<TypeModifier> modifiers, List<AnnotationData> annotations, /*Nullable*/ Object initialValue) {}
