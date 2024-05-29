package net.luis.agent.preload.data;

import net.luis.agent.preload.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.Set;

/**
 *
 * @author Luis-St
 *
 */

public record InnerClassData(@NotNull Type owner, @Nullable String name, @NotNull Type type, @NotNull TypeAccess access, @NotNull InnerClassType classType, @NotNull Set<TypeModifier> modifier) {}
