package net.luis.preload.data;

import net.luis.preload.ClassFileScanner;
import net.luis.preload.type.*;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ClassInfo(Type type, String signature, TypeAccess access, ClassType classType, List<TypeModifier> modifiers, /*Nullable*/ Type superType, List<Type> interfaces, List<AnnotationData> annotations) {}
