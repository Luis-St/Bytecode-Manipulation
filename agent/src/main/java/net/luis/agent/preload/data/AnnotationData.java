package net.luis.agent.preload.data;

import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings("unchecked")
public record AnnotationData(@NotNull Type type, @NotNull Map<String, Object> values) {
	
	public <X> @Nullable X get(@NotNull String key) {
		if (this.values.containsKey(key)) {
			return (X) this.values.get(key);
		}
		return null;
	}
	
	public <X> @Nullable X getDefault(@NotNull PreloadContext context, @NotNull String key) {
		ClassContent content = context.getClassContent(this.type);
		List<MethodData> methods = content.getMethods(key);
		if (methods.size() != 1) {
			return null;
		}
		return (X) methods.getFirst().annotationDefault().get();
	}
	
	@SuppressWarnings("DataFlowIssue")
	public <X> @NotNull X getOrDefault(@NotNull PreloadContext context, @NotNull String key) {
		return this.values.containsKey(key) ? this.get(key) : this.getDefault(context, key);
	}
}
