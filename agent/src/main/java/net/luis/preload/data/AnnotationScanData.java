package net.luis.preload.data;

import org.objectweb.asm.Type;

import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public record AnnotationScanData(Type type, Map<String, Object> values) {
	
	@SuppressWarnings("unchecked")
	public <X> X get(String key) {
		return (X) this.values.get(key);
	}
}
