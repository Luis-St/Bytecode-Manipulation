package net.luis.preload.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ClassContent(@NotNull List<RecordComponentData> recordComponents, @NotNull List<FieldData> fields, @NotNull List<MethodData> methods) {}
