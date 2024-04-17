package net.luis.preload.data;

import java.util.Map;

public class AnnotationData {
	
	private final String name;
	private final String descriptor;
	private final Map<String, Object> values;
	
	public AnnotationData(String name, String descriptor, Map<String, Object> values) {
		this.name = name;
		this.descriptor = descriptor;
		this.values = values;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getDescriptor() {
		return this.descriptor;
	}
	
	public Map<String, Object> getValues() {
		return this.values;
	}
	
	public Object get(String key) {
		return this.values.get(key);
	}
	
	public boolean getBoolean(String key) {
		return (boolean) this.values.get(key);
	}
	
	public byte getByte(String key) {
		return (byte) this.values.get(key);
	}
	
	public char getChar(String key) {
		return (char) this.values.get(key);
	}
	
	public short getShort(String key) {
		return (short) this.values.get(key);
	}
	
	public int getInt(String key) {
		return (int) this.values.get(key);
	}
	
	public long getLong(String key) {
		return (long) this.values.get(key);
	}
	
	public float getFloat(String key) {
		return (float) this.values.get(key);
	}
	
	public double getDouble(String key) {
		return (double) this.values.get(key);
	}
}
