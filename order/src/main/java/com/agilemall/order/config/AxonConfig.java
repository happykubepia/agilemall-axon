package com.agilemall.order.config;

import com.agilemall.common.config.Constants;
import com.thoughtworks.xstream.XStream;
import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.WeakReferenceCache;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.eventhandling.async.SequentialPerAggregatePolicy;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonConfig {
    @Bean
    public XStream xStream() {
        XStream xStream = new XStream();

        xStream.allowTypesByWildcard(new String[] {
                "com.agilemall.**"
        });
        return xStream;
    }

    @Bean
    public SnapshotTriggerDefinition snapshotTrigger(Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, Constants.SNAPSHOT_COUNT);
    }

    @Bean
    public Cache snapshotCache() {
        return new WeakReferenceCache();
    }

    @Autowired
    public void configure(EventProcessingConfigurer configurer) {
        //참고: https://cla9.tistory.com/17
        //-- 한번에 처리하는 Event 갯수(1개 -> 100개)와 동시 수행 Thread 수를 지정함
        configurer.registerTrackingEventProcessor(
                "accounts",
                org.axonframework.config.Configuration::eventStore,
                c -> TrackingEventProcessorConfiguration.forParallelProcessing(3)
                        .andBatchSize(100)
        );

        //-- 동일 Aggregate는 동일 Thread에서 처리되게 하여 처리 순서 보장을 함
        configurer.registerSequencingPolicy("orders",
                configuration -> SequentialPerAggregatePolicy.instance());
    }
}
