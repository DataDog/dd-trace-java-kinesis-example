package com.datadoghq.example;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.Record;

import java.util.Collections;

public class SdkConsumer {
	private static final int READ_LIMIT = 1000;

	private final KinesisClient kinesisClient;

	public SdkConsumer(KinesisClient kinesisClient) {
		this.kinesisClient = kinesisClient;
	}

	public void readRecords(String streamName) {
		Tracer tracer = GlobalTracer.get();

		GetRecordsRequest recordsRequest = GetRecordsRequest.builder()
				.limit(READ_LIMIT).build();

		GetRecordsResponse response = kinesisClient.getRecords(recordsRequest);

		for (Record record : response.records()) {
			SdkBytes byteBuffer = record.data();
			SomeMessage message = SomeMessage.fromBytes(byteBuffer.asByteArray());

			SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS, message);

			Span span = tracer.buildSpan("kinesis.consume")
					.withTag("kinesis.stream", streamName)
					.asChildOf(extractedContext)
					.start();
			try (Scope scope = tracer.activateSpan(span)) {
				// TODO Do something with message
				// if applicable, potentially creating a child span under "span" if that makes business sense
			} catch (Exception e) {
				// Set error tags if there is an issue
				span.setTag(Tags.ERROR, true);
				span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));
			} finally {
				span.finish();
			}

		}
	}
}
