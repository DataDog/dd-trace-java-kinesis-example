### Kinesis Context Propagation Examples

This repository contains example producers and consumers using the AWS sdk in additon to KPL/KCL.  It leaves out the details of setting up the specific clients

The `SomeMessage` class implements `TextMap` which is the holder for propagation headers and values
An alternative approach to using a `Map` inside the message would be to specify fields for all of the propagation headers separately inside the class and implementing iterator on those

The headers for datadog propagation are:
```
  TRACE_ID_KEY = "x-datadog-trace-id";
  SPAN_ID_KEY = "x-datadog-parent-id";
  SAMPLING_PRIORITY_KEY = "x-datadog-sampling-priority";
  ORIGIN_KEY = "x-datadog-origin";
``` 

from [DatadogHttpCode](https://github.com/DataDog/dd-trace-java/blob/master/dd-trace-core/src/main/java/datadog/trace/core/propagation/DatadogHttpCodec.java#L17)
with `"ot-baggage-{name}" used for baggage`

The headers for B3 propagation are: 
```
  TRACE_ID_KEY = "X-B3-TraceId";
  SPAN_ID_KEY = "X-B3-SpanId";
  SAMPLING_PRIORITY_KEY = "X-B3-Sampled";
```
from [B3HttpCode](https://github.com/DataDog/dd-trace-java/blob/master/dd-trace-core/src/main/java/datadog/trace/core/propagation/B3HttpCodec.java#L23)

A third approach would be using a separate bridge class that implements `TextMap` and in turn set/gets items from message
