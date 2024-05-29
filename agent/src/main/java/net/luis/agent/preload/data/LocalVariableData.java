package net.luis.agent.preload.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

/**
 *
 * @author Luis-St
 *
 */

public record LocalVariableData(@NotNull MethodData owner, int index, @NotNull String name, @NotNull Type type, @Nullable String signature) {}
