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

public record MethodData(String name, Type type, String signature, TypeAccess access, List<TypeModifier> modifiers, List<AnnotationData> annotations, List<ParameterData> parameters, List<Type> exceptions) implements ASMData {}
