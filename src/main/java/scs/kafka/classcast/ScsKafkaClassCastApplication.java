package scs.kafka.classcast;

import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootApplication
@Slf4j
public class ScsKafkaClassCastApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScsKafkaClassCastApplication.class, args);
    }

    private final Random random = new Random();

    @Bean
    public Supplier<Message<ClassA>> supplier() {
        return () -> MessageBuilder.createMessage(
            new ClassA(random.ints(0, 10).findFirst().getAsInt()),
            new MessageHeaders(
                Map.of(KafkaHeaders.MESSAGE_KEY, String.valueOf(random.ints(0, 10).findFirst().getAsInt())))
        );
    }

    @Bean
    public Function<KStream<String, ClassA>, KStream<String, ClassB>> streamFunction() {
        return stream -> stream.mapValues(classA -> new ClassB(String.valueOf(classA.getA())));
    }

    @Bean
    public Function<ClassB, Message<ClassC>> function() {
        return classB -> MessageBuilder.createMessage(
            new ClassC(Integer.parseInt(classB.getB())),
            new MessageHeaders(Map.of(KafkaHeaders.MESSAGE_KEY, classB.getB()))
        );
    }

    @Bean
    public Consumer<KStream<String, ClassC>> consumer1() {
        return stream -> stream.peek((k, v) -> log.info("No class cast - consumer1: {}, {}", k, v));
    }

    @Bean
    public Consumer<KStream<String, String>> consumer2() {
        return stream -> stream.peek((k, v) -> log.info("No class cast - consumer2: {}, {}", k, v));
    }

    @Bean
    public Consumer<ClassC> consumer3() {
        return classC -> log.info("No class cast - consumer3: {}", classC);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class ClassA {

        private int a;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ClassB {

        private String b;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ClassC {

        private int c;
    }
}
