package net.luis.preload.data;

import net.luis.preload.data.type.*;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ClassData(Type type, /*Nullable*/ String signature, TypeAccess access, ClassType classType, List<TypeModifier> modifiers, Type superType, List<Type> interfaces) {

}
