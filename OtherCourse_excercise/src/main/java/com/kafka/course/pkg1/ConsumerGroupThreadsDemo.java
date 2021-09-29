package com.kafka.course.pkg1;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerGroupThreadsDemo {

    public class ConsumerThread implements Runnable {
        private CountDownLatch latch;
        private KafkaConsumer<String, String> consumer;
        private String topic, bootstrapServer, groupId;
        private Logger logger = LoggerFactory.getLogger(ConsumerThread .class.getName());


        public ConsumerThread(CountDownLatch latch, String topic, String bootstrapServer, String groupId){
            this.latch = latch;
            this.topic = topic;
            this.bootstrapServer = bootstrapServer;
            this.groupId = groupId;
        }

        @Override
        public void run() {
            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
            // kafka sends bytes, not a string. So we have to deserialize those bytes to get the datatype we know and love
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            // create consumer and subscribe to topic(s)
            consumer = new KafkaConsumer<String, String>(properties);
            consumer.subscribe(Collections.singleton(topic));
            // poll for data
            try {
                while(true){
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record: records){
                        logger.info("Key " + record.key() + ", Value: " + record.value());
                        logger.info("Parition " + record.partition() + ", Offset: " + record.offset());

                    }
                }
            } catch(WakeupException wokenException){
                logger.info("Received shutdown signal!", wokenException);
            } finally {
                consumer.close();
                latch.countDown(); // tell main that we're done with consumer
            }
        }
        public void shutdown(){
            // shuts our consumer thread
            // the wakeup method is meant to interrupt consumer.poll(), it will throw WakeUpException
            consumer.wakeup();

        }
    }


    public static void main(String[] args) {
        ConsumerGroupThreadsDemo instance = new ConsumerGroupThreadsDemo();
        instance.run();
    }

    private void run(){
        Logger logger = LoggerFactory.getLogger(ConsumerGroupThreadsDemo.class.getName());
        logger.info("createing consuemr thread");

        String bootstrapServer = "127.0.0.1:9092";
        String groupId = "my-fifth-application";
        String topic = "first_topic";
        CountDownLatch latch = new CountDownLatch(1);

        Runnable myConsumerThread = new ConsumerThread(latch, topic, bootstrapServer, groupId);

        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logger.info("Caught shutdown hook");
            ((ConsumerThread) myConsumerThread).shutdown();
        }));

        // start the tread
        Thread myThread = new Thread(myConsumerThread);
        myThread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.info("Application got intrerupted", e);
        } finally {
            logger.info("Application is closing");
        }

    };

}
