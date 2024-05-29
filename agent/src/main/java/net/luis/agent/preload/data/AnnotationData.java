package net.luis.agent.preload.data;

import net.luis.agent.AgentContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings("unchecked")
public record AnnotationData(@NotNull Type type, boolean visible, @NotNull Map<String, Object> values) {
	
	public boolean is(@NotNull Type type) {
		return this.type.equals(type);
	}
	
	public boolean isAny(@NotNull Type... type) {
		return Arrays.stream(type).anyMatch(this::is);
	}
	
	public <X> @Nullable X get(@NotNull String key) {
		if (this.values.containsKey(key)) {
			return (X) this.values.get(key);
		}
		return null;
	}
	
	public <X> @Nullable X getDefault(@NotNull String key) {
		ClassData data = AgentContext.get().getClassData(this.type);
		List<MethodData> methods = data.getMethods(key);
		if (methods.size() != 1) {
			return null;
		}
		return (X) methods.getFirst().annotationDefault().get();
	}
	
	@SuppressWarnings("DataFlowIssue")
	public <X> @NotNull X getOrDefault(@NotNull String key) {
		return this.values.containsKey(key) ? this.get(key) : this.getDefault(key);
	}
}
