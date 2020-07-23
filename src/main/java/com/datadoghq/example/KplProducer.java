package com.datadoghq.example;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

import java.nio.ByteBuffer;
import java.util.Collections;

public class KplProducer {
	private final KinesisProducer kinesis;

	public KplProducer(KinesisProducer kinesis) {
		this.kinesis = kinesis;
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

			ListenableFuture<UserRecordResult> future = kinesis.addUserRecord(streamName, partitionKey, ByteBuffer.wrap(message.toBytes()));
			Futures.addCallback(future, new AsyncSpanCloser(span));
		} catch (Exception e) {
			// An exception happened outside of kinesis
			// AsyncSpanCloser will not be called so set the error tags and close the span

			span.setTag(Tags.ERROR, true);
			span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));
			span.finish();
		}
	}

	private static class AsyncSpanCloser implements FutureCallback<UserRecordResult> {
		private final Span span;

		private AsyncSpanCloser(Span span) {
			this.span = span;
		}

		@Override
		public void onSuccess(UserRecordResult result) {
			try (Scope scope = GlobalTracer.get().activateSpan(span)) {
				// Do something with UserRecordResult
				// if applicable, potentially creating a child span under "span" if that makes business sense
			} catch (Exception e) {
				span.setTag(Tags.ERROR, true);
				span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));
			} finally {
				span.finish();
			}

		}

		@Override
		public void onFailure(Throwable throwable) {
			try (Scope scope = GlobalTracer.get().activateSpan(span)) {
				span.setTag(Tags.ERROR, true);
				span.log(Collections.singletonMap(Fields.ERROR_OBJECT, throwable));
			} finally {
				span.finish();
			}

		}
	}
}
