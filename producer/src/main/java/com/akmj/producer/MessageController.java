package com.akmj.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.akmj.producer.config.RabbitMQValue.*;

@RestController
public class MessageController {

    private final RabbitTemplate rabbitTemplate;

    public MessageController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity produceMessage(@PathVariable String workerId, @RequestBody Message message) {
        System.out.println(message + " sending is delivering");
        rabbitTemplate.convertAndSend(TOPIC_EXCHANGE_NAME, "worker.".concat(workerId),message.getMessage());
        return ResponseEntity.ok(null);
    }

}
