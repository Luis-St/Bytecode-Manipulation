package net.luis.preload.data;

import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import net.luis.preload.scanner.ParameterScanData;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record MethodData(String name, Type type, TypeAccess access, List<TypeModifier> modifiers, List<AnnotationData> annotations, List<ParameterScanData> parameters, List<Type> exceptions) {}
