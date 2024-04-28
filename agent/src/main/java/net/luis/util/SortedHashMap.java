package net.luis.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public class SortedHashMap<K, V> extends LinkedHashMap<K, V> {
	
	public void putBefore(K target, K key, V value) {
		Map<K, V> temp = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : this.entrySet()) {
			if (entry.getKey().equals(target)) {
				temp.put(key, value);
			}
			temp.put(entry.getKey(), entry.getValue());
		}
		this.clear();
		this.putAll(temp);
	}
	
	public void putAfter(K targetKey, K key, V value) {
		Map<K, V> temp = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : this.entrySet()) {
			temp.put(entry.getKey(), entry.getValue());
			if (entry.getKey().equals(targetKey)) {
				temp.put(key, value);
			}
		}
		this.clear();
		this.putAll(temp);
	}
}
