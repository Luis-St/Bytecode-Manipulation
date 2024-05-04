package net.luis.agent.preload.data;

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
	
	public <X> X get(String key) {
		return (X) this.values.get(key);
	}
	
	public <X> List<X> getArray(String key) {
		return (List<X>) this.values.get(key);
	}
	
	public AnnotationData getAnnotation(String key) {
		return (AnnotationData) this.values.get(key);
	}
}
