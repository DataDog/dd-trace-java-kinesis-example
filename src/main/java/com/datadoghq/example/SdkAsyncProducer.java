package com.datadoghq.example;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class SdkAsyncProducer {
	private final KinesisAsyncClient kinesisClient;

	public SdkAsyncProducer(KinesisAsyncClient kinesisClient) {
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
			CompletableFuture<PutRecordResponse> future = kinesisClient.putRecord(request);
			future.whenComplete(new AsyncSpanCloser(span));
		} catch (Exception e) {
			// An exception happened outside of kinesis
			// AsyncSpanCloser will not be called so set the error tags and close the span

			span.setTag(Tags.ERROR, true);
			span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));
			span.finish();
		}
	}

	private static class AsyncSpanCloser implements BiConsumer<PutRecordResponse, Throwable> {
		private final Span span;

		private AsyncSpanCloser(Span span) {
			this.span = span;
		}

		@Override
		public void accept(PutRecordResponse putRecordResponse, Throwable throwable) {
			try (Scope scope = GlobalTracer.get().activateSpan(span)) {
				if (throwable != null) {
					// Set error tags if there is an issue
					span.setTag(Tags.ERROR, true);
					span.log(Collections.singletonMap(Fields.ERROR_OBJECT, throwable));
				} else {
					// Do something with PutRecordResponse if applicable
					// Potentially creating a child span under "span" if that makes business sense
				}
			} catch (Exception e) {
				// Exception happened in the else block
				span.setTag(Tags.ERROR, true);
				span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));
			} finally {
				span.finish();
			}
		}
	}
}
