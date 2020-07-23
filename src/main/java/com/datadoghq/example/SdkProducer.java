package com.datadoghq.example;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.Collections;

public class SdkProducer {
	private final KinesisClient kinesisClient;

	public SdkProducer(KinesisClient kinesisClient) {
		this.kinesisClient = kinesisClient;
	}

	public void sendMessage(SomeMessage message, String streamName, String partitionKey) {
		Tracer tracer = GlobalTracer.get();

		// Create the span, tags here are optional and shown for example purposes
		Span span = tracer.buildSpan("kinesis.send")
				.withTag("kinesis.stream", streamName)
				.withTag("kinesis.partition", partitionKey).start();

		try (Scope scope = tracer.activateSpan(span)) {
			// Inject the headers into the message
			tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, message);

			PutRecordRequest request = PutRecordRequest.builder()
					.partitionKey(partitionKey)
					.streamName(streamName)
					.data(SdkBytes.fromByteArray(message.toBytes()))
					.build();
			PutRecordResponse response = kinesisClient.putRecord(request);
			// Do something with response ...
		} catch (Exception e) {
			// Set error tags if there is an issue
			span.setTag(Tags.ERROR, true);
			span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));
		} finally {
			// Finish the span
			span.finish();
		}

	}
}
