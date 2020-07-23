package com.datadoghq.example;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.nio.ByteBuffer;
import java.util.Collections;

public class KplConsumer implements ShardRecordProcessor {

	@Override
	public void processRecords(ProcessRecordsInput processRecordsInput) {
		Tracer tracer = GlobalTracer.get();

		for (KinesisClientRecord record : processRecordsInput.records()) {
			ByteBuffer byteBuffer = record.data();
			SomeMessage message = SomeMessage.fromBytes(byteBuffer.array());

			SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS, message);

			Span span = tracer.buildSpan("kinesis.consume")
					.asChildOf(extractedContext)
					.start();
			try (Scope scope = tracer.activateSpan(span)) {
				// TODO Do something with message
			} catch (Exception e) {
				// Set error tags if there is an issue
				span.setTag(Tags.ERROR, true);
				span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));
			} finally {
				span.finish();
			}
		}
	}

	@Override
	public void initialize(InitializationInput initializationInput) {
	}

	@Override
	public void leaseLost(LeaseLostInput leaseLostInput) {
	}

	@Override
	public void shardEnded(ShardEndedInput shardEndedInput) {
	}

	@Override
	public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
	}
}
