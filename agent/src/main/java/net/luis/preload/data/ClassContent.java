package net.luis.preload.data;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ClassContent(List<RecordComponentData> recordComponents, List<FieldData> fields, List<MethodData> methods) {}
