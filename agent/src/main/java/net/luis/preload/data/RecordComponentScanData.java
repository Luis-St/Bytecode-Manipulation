package net.luis.preload.data;

import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record RecordComponentScanData(Type type, String name, /*Nullable*/ String signature, List<AnnotationScanData> annotations) {}
