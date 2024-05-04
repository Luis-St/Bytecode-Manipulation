package net.luis.agent.preload.data;

import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
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
	
	public boolean has(String key) {
		return this.values.containsKey(key);
	}
	
	public <X> boolean hasDefault(@NotNull PreloadContext context, @NotNull String key) {
		ClassContent content = context.getClassContent(this.type);
		if (!content.hasMethod(key)) {
			return false;
		}
		List<MethodData> methods = content.getMethods(key);
		if (methods.size() != 1) {
			return false;
		}
		return methods.getFirst().annotationDefault().isPresent();
	}
	
	public <X> @NotNull X get(@NotNull String key) {
		return (X) this.values.get(key);
	}
	
	@SuppressWarnings({ "ReturnOfNull", "DataFlowIssue" })
	public <X> @NotNull X getDefault(@NotNull PreloadContext context, @NotNull String key) {
		ClassContent content = context.getClassContent(this.type);
		if (!content.hasMethod(key)) {
			return null;
		}
		List<MethodData> methods = content.getMethods(key);
		if (methods.size() != 1) {
			return null;
		}
		return (X) methods.getFirst().annotationDefault().get();
	}
	
	public <X> X getOrDefault(@NotNull PreloadContext context, @NotNull String key) {
		return this.has(key) ? this.get(key) : this.getDefault(context, key);
	}
}
