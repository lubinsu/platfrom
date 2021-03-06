package com.changtu.util.kafka

import java.util.Properties
import java.util.concurrent.Executors

import com.changtu.util.host.Configuration
import com.changtu.util.{ClusterEnv, Logging}
import kafka.consumer.{Consumer, ConsumerConfig, KafkaStream}
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

/**
  * Created by lubinsu on 8/29/2016.
  *
  * Kafka通用工具类
  *
  */
object KafkaUtils extends Logging with ClusterEnv {

  val brokers = Configuration("platform.properties").getString("brokers")
  val zookeeper = Configuration("platform.properties").getString("zookeeper")

  /**
    * producer 发送消息
    *
    * @param topic 主题
    */
  def send(topic: String, key: String, msg: String) = {
    val producer = new Producer[String, String](createProducerConfig)
    producer.send(new KeyedMessage[String, String](topic, key, msg))
    producer.close()
  }

  /**
    * 消费消息
    *
    * @param topic      主题
    * @param groupId    group
    * @param numThreads 线程数
    * @param runnable   数据处理代码
    */
  def consume(topic: String, groupId: String, numThreads: Int, runnable: (KafkaStream[Array[Byte], Array[Byte]]) => Runnable): Unit = {

    val config = createConsumerConfig(groupId)

    val consumerMap = Consumer.create(config).createMessageStreams(Map(topic -> numThreads))
    val streams = consumerMap.get(topic).get

    val executor = Executors.newFixedThreadPool(numThreads)

    // 创建多线程消费
    streams.foreach( stream => {
      executor.submit(runnable(stream))
    })
  }

  /**
    * 创建 producer config
    *
    * @return config
    */
  def createProducerConfig: ProducerConfig = {
    val props = new Properties()
    props.put("metadata.broker.list", brokers)
    props.put("bootstrap.servers", brokers)
    props.put("serializer.class", "kafka.serializer.StringEncoder")
    props.put("producer.type", "async")

    props.put("acks", "all")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val config = new ProducerConfig(props)
    config
  }

  /**
    * 创建 consumer config
    *
    * @param groupId group
    * @return 配置实例
    */
  def createConsumerConfig(groupId: String): ConsumerConfig = {
    val props = new Properties()
    props.put("zookeeper.connect", zookeeper)
    props.put("group.id", groupId)
    props.put("auto.offset.reset", "largest")
    props.put("zookeeper.session.timeout.ms", "400")
    props.put("zookeeper.sync.time.ms", "200")
    props.put("auto.commit.interval.ms", "1000")
    val config = new ConsumerConfig(props)
    config
  }

}
