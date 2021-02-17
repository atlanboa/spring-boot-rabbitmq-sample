package com.akmj.worker;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Listener {

    @RabbitListener(queues = "workerOne")
    public void workerOneMessage(String message) {
        System.out.println("workerOne : "+message);
    }

    @RabbitListener(queues = "workerTwo")
    public void workerTwoMessage(String message) {
        System.out.println("workerTwo : "+message);
    }




}
