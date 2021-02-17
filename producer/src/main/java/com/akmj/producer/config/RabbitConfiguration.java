package com.akmj.producer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    @Bean
    Queue workerOneQueue() {
        return new Queue("workerOne", false);
    }

    @Bean
    Queue workerTwoQueue() {
        return new Queue("workerTwo", false);
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange("task-exchange");
    }

    @Bean
    Binding workerOneBinding(Queue workerOneQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(workerOneQueue).to(topicExchange).with("worker.one");
    }

    @Bean
    Binding workerTwoBinding(Queue workerTwoQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(workerTwoQueue).to(topicExchange).with("worker.two");
    }

}