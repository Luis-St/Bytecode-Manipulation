package net.luis.preload.data;

import net.luis.preload.scanner.ParameterScanData;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record MethodScanData(String name, Type type, TypeAccess access, List<TypeModifier> modifiers, List<AnnotationScanData> annotations, List<ParameterScanData> parameters, List<Type> exceptions) {}
