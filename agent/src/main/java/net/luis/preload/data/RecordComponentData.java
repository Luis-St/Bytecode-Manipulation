package net.luis.preload.data;

import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record RecordComponentData(String name, Type type,  /*Nullable*/ String signature, List<AnnotationData> annotations) {}
