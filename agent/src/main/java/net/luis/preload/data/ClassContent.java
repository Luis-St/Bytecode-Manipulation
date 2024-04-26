package net.luis.preload.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ClassContent(@Unmodifiable @NotNull List<RecordComponentData> recordComponents, @Unmodifiable @NotNull List<FieldData> fields, @Unmodifiable @NotNull List<MethodData> methods) {}
