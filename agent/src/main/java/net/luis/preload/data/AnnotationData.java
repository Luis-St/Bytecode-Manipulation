package net.luis.preload.data;

import java.util.Map;

public record AnnotationData(String descriptor, Map<String, Object> values) {
/**
 *
 * @author Luis-St
 *
 */

	
	@SuppressWarnings("unchecked")
	public <X> X get(String key) {
		return (X) this.values.get(key);
	}
}
