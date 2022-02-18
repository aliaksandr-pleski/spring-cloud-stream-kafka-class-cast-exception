# spring-cloud-stream-kafka-class-cast-exception

Potentially bug in spring-cloud-stream kafka/kafka-streams binder

This is a `spring-cloud-stream` application. Spring Boot is 2.6.2 and Spring Cloud is 2021.0.0.

Working with Kafka and Kafka Streams.

The app has the following data pipeline:

```java
@Bean
public Supplier<Message<ClassA>>supplier(){
    return()->MessageBuilder.createMessage(
    new ClassA(random.ints(0,10).findFirst().getAsInt()),
    new MessageHeaders(
    Map.of(KafkaHeaders.MESSAGE_KEY,String.valueOf(random.ints(0,10).findFirst().getAsInt())))
    );
    }

@Bean
public Function<KStream<String, ClassA>,KStream<String, ClassB>>streamFunction(){
    return stream->stream.mapValues(classA->new ClassB(String.valueOf(classA.a)));
    }

@Bean
public Function<ClassB, Message<ClassC>>function(){
    return classB->MessageBuilder.createMessage(
    new ClassC(Integer.parseInt(classB.b)),
    new MessageHeaders(Map.of(KafkaHeaders.MESSAGE_KEY,classB.b))
    );
    }
```

There are 3 consumers that expect `ClassC` objects from Kafka:

```java
@Bean
public Consumer<KStream<String, ClassC>>consumer1(){
    return stream->stream.peek((k,v)->log.info("No class cast: {}, {}",k,v));
    }

@Bean
public Consumer<KStream<String, String>>consumer2(){
    return stream->stream.peek((k,v)->log.info("No class cast - consumer2: {}, {}",k,v));
    }

@Bean
public Consumer<ClassC> consumer3(){
    return classC->log.info("No class cast - consumer3: {}",classC);
    }
```

Consumers 2 and 3 work fine:

```log
2022-02-18 11:57:04.782  INFO 19855 --- [-StreamThread-1] s.k.c.ScsKafkaClassCastApplication       : No class cast - consumer2: 9, {"c":9}
2022-02-18 11:57:04.783  INFO 19855 --- [container-0-C-1] s.k.c.ScsKafkaClassCastApplication       : No class cast - consumer3: ScsKafkaClassCastApplication.ClassC(c=9)
2022-02-18 11:57:05.499  INFO 19855 --- [-StreamThread-1] s.k.c.ScsKafkaClassCastApplication       : No class cast - consumer2: 3, {"c":3}
2022-02-18 11:57:05.500  INFO 19855 --- [container-0-C-1] s.k.c.ScsKafkaClassCastApplication       : No class cast - consumer3: ScsKafkaClassCastApplication.ClassC(c=3)
```

Where Consumer 1 gets following `ClassCastException`:

```log
2022-02-18 12:25:08.257 ERROR 22910 --- [-StreamThread-1] o.a.k.s.processor.internals.TaskManager  : stream-thread [consumer1-applicationId-72da01fb-de90-4133-bcc4-d63e09f3f8eb-StreamThread-1] Failed to process stream task 0_0 due to the following error:

org.apache.kafka.streams.errors.StreamsException: ClassCastException invoking Processor. Do the Processor's input types match the deserialized types? Check the Serde setup and change the default Serdes in StreamConfig or provide correct Serdes via method parameters. Make sure the Processor can accept the deserialized input of type key: java.lang.String, and value: scs.kafka.classcast.ScsKafkaClassCastApplication$ClassB.
Note that although incorrect Serdes are a common cause of error, the cast exception might have another cause (in user code, for example). For example, if a processor wires in a store, but casts the generics incorrectly, a class cast exception could be raised during processing, but the cause would not be wrong Serdes.
	at org.apache.kafka.streams.processor.internals.ProcessorNode.process(ProcessorNode.java:150) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.ProcessorContextImpl.forwardInternal(ProcessorContextImpl.java:253) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.ProcessorContextImpl.forward(ProcessorContextImpl.java:232) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.ProcessorContextImpl.forward(ProcessorContextImpl.java:191) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.SourceNode.process(SourceNode.java:84) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamTask.lambda$process$1(StreamTask.java:731) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl.maybeMeasureLatency(StreamsMetricsImpl.java:769) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamTask.process(StreamTask.java:731) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.TaskManager.process(TaskManager.java:1193) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamThread.runOnce(StreamThread.java:753) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamThread.runLoop(StreamThread.java:583) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamThread.run(StreamThread.java:555) ~[kafka-streams-3.0.0.jar:na]
Caused by: java.lang.ClassCastException: class scs.kafka.classcast.ScsKafkaClassCastApplication$ClassB cannot be cast to class scs.kafka.classcast.ScsKafkaClassCastApplication$ClassC (scs.kafka.classcast.ScsKafkaClassCastApplication$ClassB and scs.kafka.classcast.ScsKafkaClassCastApplication$ClassC are in unnamed module of loader 'app')
	at org.apache.kafka.streams.kstream.internals.KStreamPeek$KStreamPeekProcessor.process(KStreamPeek.java:41) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.ProcessorNode.process(ProcessorNode.java:146) ~[kafka-streams-3.0.0.jar:na]
	... 11 common frames omitted

2022-02-18 12:25:08.257 ERROR 22910 --- [-StreamThread-1] org.apache.kafka.streams.KafkaStreams    : stream-client [consumer1-applicationId-72da01fb-de90-4133-bcc4-d63e09f3f8eb] Encountered the following exception during processing and the registered exception handler opted to SHUTDOWN_CLIENT. The streams client is going to shut down now. 

org.apache.kafka.streams.errors.StreamsException: ClassCastException invoking Processor. Do the Processor's input types match the deserialized types? Check the Serde setup and change the default Serdes in StreamConfig or provide correct Serdes via method parameters. Make sure the Processor can accept the deserialized input of type key: java.lang.String, and value: scs.kafka.classcast.ScsKafkaClassCastApplication$ClassB.
Note that although incorrect Serdes are a common cause of error, the cast exception might have another cause (in user code, for example). For example, if a processor wires in a store, but casts the generics incorrectly, a class cast exception could be raised during processing, but the cause would not be wrong Serdes.
	at org.apache.kafka.streams.processor.internals.ProcessorNode.process(ProcessorNode.java:150) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.ProcessorContextImpl.forwardInternal(ProcessorContextImpl.java:253) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.ProcessorContextImpl.forward(ProcessorContextImpl.java:232) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.ProcessorContextImpl.forward(ProcessorContextImpl.java:191) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.SourceNode.process(SourceNode.java:84) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamTask.lambda$process$1(StreamTask.java:731) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl.maybeMeasureLatency(StreamsMetricsImpl.java:769) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamTask.process(StreamTask.java:731) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.TaskManager.process(TaskManager.java:1193) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamThread.runOnce(StreamThread.java:753) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamThread.runLoop(StreamThread.java:583) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.StreamThread.run(StreamThread.java:555) ~[kafka-streams-3.0.0.jar:na]
Caused by: java.lang.ClassCastException: class scs.kafka.classcast.ScsKafkaClassCastApplication$ClassB cannot be cast to class scs.kafka.classcast.ScsKafkaClassCastApplication$ClassC (scs.kafka.classcast.ScsKafkaClassCastApplication$ClassB and scs.kafka.classcast.ScsKafkaClassCastApplication$ClassC are in unnamed module of loader 'app')
	at org.apache.kafka.streams.kstream.internals.KStreamPeek$KStreamPeekProcessor.process(KStreamPeek.java:41) ~[kafka-streams-3.0.0.jar:na]
	at org.apache.kafka.streams.processor.internals.ProcessorNode.process(ProcessorNode.java:146) ~[kafka-streams-3.0.0.jar:na]
	... 11 common frames omitted
```