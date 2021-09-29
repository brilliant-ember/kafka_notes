package com.kafka.course.pkg1;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerWithKeysDemo {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(ProducerWithKeysDemo.class);


        String bootstrapServer = "127.0.0.1:9092";
        String serializerName = StringSerializer.class.getName(); //org.apache.kafka.common.serialization.StringSerializer

        // steps 1. create Producer properties

        // get the producer configs from here https://kafka.apache.org/documentation/#producerconfigs
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializerName);
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializerName);

        // step 2. create producer

        // <String, String> means that the key is String and Value is String
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        // step 3 . send data
        for (int i = 0; i < 10; i++) {
            String key = "id_"+Integer.toString(i);
            ProducerRecord<String, String> record = new ProducerRecord<String, String>(
                    "first_topic", key,"hello tout le monde " + Integer.toString(i));
            // note this is async, meaning if your program exits the data will never get sent lol, that's why you have to
            // use the flush method, or the close method (it has flush built in)
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if (e == null) {
                        // null error means msg sent successfully
                        logger.info("Recived new meta data. \n" +
                                "Topic: " + recordMetadata.topic() + "\n" +
                                "Partition: " + recordMetadata.partition() + "\n" +
                                "Offset: " + recordMetadata.offset() + "\n" +
                                "Timestamp: " + recordMetadata.timestamp() + "\n");
                    } else {
                        // deal with the error
                        logger.error("Error while producing", e);
                    }

                }
            });
        }
        producer.close();
    }
}
