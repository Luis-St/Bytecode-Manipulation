package net.luis.preload.data;

import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record FieldScanData(String name, Type type,  /*Nullable*/ String signature, TypeAccess access, List<TypeModifier> modifiers, List<AnnotationScanData> annotations, Object initialValue) {}
