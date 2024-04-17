package net.luis.preload.data;

import java.util.Map;

public record AnnotationData(String name, String descriptor, Map<String, Object> values) {
	
	@SuppressWarnings("unchecked")
	public <X> X get(String key) {
		return (X) this.values.get(key);
	}
}
