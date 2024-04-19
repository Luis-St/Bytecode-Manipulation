package net.luis.preload.data;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ClassContentData(List<AnnotationData> annotations, List<FieldData> fields, List<MethodData> methods) {}
