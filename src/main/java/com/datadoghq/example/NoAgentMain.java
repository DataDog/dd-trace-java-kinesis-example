package com.datadoghq.example;

import datadog.opentracing.DDTracer;
import io.opentracing.util.GlobalTracer;

public class NoAgentMain {
	public static void main(String[] args) {
		// These lines are only necessary if running without the java agent
		// When running with the agent, this is automatic
		DDTracer tracer = DDTracer.builder().serviceName("KinesisService").build();
		GlobalTracer.registerIfAbsent(tracer);
		datadog.trace.api.GlobalTracer.registerIfAbsent(tracer);
	}
}
