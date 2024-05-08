package net.luis.agent.preload.data;

import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings({"unchecked", "ReturnOfNull", "DataFlowIssue"})
public record AnnotationData(@NotNull Type type, @NotNull Map<String, Object> values) {
	
	public boolean is(@NotNull Type type) {
		return this.type.equals(type);
	}
	
	public boolean isAny(@NotNull Type... type) {
		return Arrays.stream(type).anyMatch(this::is);
	}
	
	public <X> @NotNull X get(@NotNull String key) {
		if (this.values.containsKey(key)) {
			return (X) this.values.get(key);
		}
		return null;
	}
	
	public <X> @NotNull X getDefault(@NotNull PreloadContext context, @NotNull String key) {
		ClassContent content = context.getClassContent(this.type);
		List<MethodData> methods = content.getMethods(key);
		if (methods.size() != 1) {
			return null;
		}
		return (X) methods.getFirst().annotationDefault().get();
	}
	
	public <X> @NotNull X getOrDefault(@NotNull PreloadContext context, @NotNull String key) {
		return this.values.containsKey(key) ? this.get(key) : this.getDefault(context, key);
	}
}
