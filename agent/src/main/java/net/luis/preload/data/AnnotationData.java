package net.luis.preload.data;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public record AnnotationData(@NotNull Type type, @NotNull Map<String, Object> values) {
	
	@SuppressWarnings("unchecked")
	public <X> X get(String key) {
		return (X) this.values.get(key);
	}
	
	public boolean has(String key) {
		return this.values.containsKey(key);
	}
	
	public <T> boolean has(String key, Class<T> type) {
		return this.values.containsKey(key) && type.isInstance(this.values.get(key));
	}
}
