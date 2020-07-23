package com.datadoghq.example;

import io.opentracing.propagation.TextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SomeMessage implements TextMap {
	private Map<String, String> propagationHeaders = new HashMap<>();

	public static SomeMessage fromBytes(byte[] bytes) {
		// TODO implementation to create message from bytes
		return new SomeMessage();
	}

	public byte[] toBytes() {
		// TODO implementation convert message to actual bytes
		return new byte[0];
	}

	// TextMap methods for propagation
	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		return propagationHeaders.entrySet().iterator();
	}

	@Override
	public void put(String key, String value) {
		propagationHeaders.put(key, value);
	}
}
