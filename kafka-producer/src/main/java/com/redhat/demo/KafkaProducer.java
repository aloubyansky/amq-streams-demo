package com.redhat.demo;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class KafkaProducer {

    private static Long lastKey = 0L;

    @ConfigProperty(name = "producer.tick-frequency", defaultValue = "1000")
    private Long tickFrequency;

    @ConfigProperty(name = "producer.partitions", defaultValue = "")
    private String partitions;

    @ConfigProperty(name = "producer.parted", defaultValue = "false")
    private Boolean parted;

    List<Integer> partitionList = null;

    @Outgoing("event-out")
    public Multi<KafkaRecord<Long, String>> generate() {
        if (parted && partitions != null && partitions.length() > 0 )
            partitionList = Arrays.stream(partitions.split(",")).map(Integer::parseInt).collect(Collectors.toList());

        return Multi.createFrom().ticks().every(Duration.ofMillis(tickFrequency))
                .map(x -> {
                    // workaround to avoid other events to surpass the first
                    if (lastKey == 1)
                        try {
                            Thread.sleep(3000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    lastKey++;
                    if ( parted == false ) {
                        System.out.println("Generating message key: " + lastKey);
                        return KafkaRecord.of(lastKey, "demo message "+lastKey);
                    } else {
                        int iteration = lastKey.intValue() % partitionList.size();
                        int partition = partitionList.get(iteration);
                        System.out.println("Generating message counter: " + lastKey + " on partition: " +partition);
                        return KafkaRecord.of(null, Long.valueOf(partition), "demo message " + lastKey, null, partition);
                    }
                });
    }
}
