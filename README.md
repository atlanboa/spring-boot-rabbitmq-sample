# Spring Boot 로 RabbitMQ Topic Exchange 적용해보기

로컬이나 도커에 RabbitMQ 를 설치하고 포트 번호는 5672에서 가동하고 있다는 전제조건하에 진행합니다.



이 과정에서는 RabbitMQ Topic Exchange 를 스프링 부트에 적용해보겠습니다.

# 의존성

```xml
<dependencies>
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-amqp</artifactId>
   </dependency>
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
   </dependency>

   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
   </dependency>
   <dependency>
      <groupId>org.springframework.amqp</groupId>
      <artifactId>spring-rabbit-test</artifactId>
      <scope>test</scope>
   </dependency>
   <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <version>1.18.18</version>
      <scope>provided</scope>
   </dependency>

</dependencies>
```

### spring-boot-starter-web

간단한 웹 요청을 위해서 포함합니다.

### spring-boot-starter-amqp

RabbitTemplate 과 AMQP 관련 설정이 필요합니다.





RabbitMQ 에서 메시지를 전송하는 사이드를 producer 라고 합니다.

메시지를 전송받는 사이드를 consumer 라고 합니다. 

다른 말로도 sender, receiver 라고도 하나 여기서는 위처럼 사용하겠습니다.

# Producer Application

***Producer* 에서 설정할 부분은 얼마되지 않습니다.**

1. 어떤 queue 를 사용할 것인가
2. 어떤 exchange 를 사용할 것인가
3. exchange 에서 메시지를 어떤 queue 로 전송하게 할 것인가



**위 3가지면 간단한 producer 어플리케이션 구현을 끝이 났습니다.**



### application.yml 설정

RabbitMQ 가 실행되면 **기본으로 생성되는 사용자**가 있습니다.

*guest / guest*  라는 아이디와 패스워드를 지닌 사용자가 생성이 됩니다.

따라서 Spring AMQP 에도 특정하게 설정하지 않으면 이 사용자를 이용해서 접속하는거 같아요.

우리는 새로운 사용자를 생성해서 진행해봅시다.



#### 사용자 추가

1. http://localhost:15672 에 접속하게 되면 RabbitMQ 매니지먼트 페이지를 확인할 수 있습니다.
2. 기본 사용자 guest / guest 로 로그인합니다.
3. Admin 탭으로 이동합니다.
4. 하단에서 Username 은 ops0 , Password 는 ops0 로 설정합니다.
5. 좌측 하단에 `Add User` 를 클릭합니다.

그럼 사용자가 추가됐습니다.



파일을 수정해줍시다.

```yaml
spring:
  rabbitmq:
    host: localhost
    username: ops0
    password: ops0
```

그럼 이제 RabbitMQ Connection 을 생성할 때 ops0 유저로 접속하게 됩니다.





### RabbitMQ Configuration

```java
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
```

이전에도 이야기했듯이 여기서는 3가지 초점을 맞춰야합니다.

1. Queue 생성
2. Exchange 생성
3. Queue 와 Exchange 바인딩



workerOneQueue , workerTwoQueue 를 생성했고 각 큐의 이름을 workerOne, workerTwo 으로 지정합니다.



TopicExchange Bean 을 정의해주고 이름은 task-exchange 로 생성합니다.



두 개의 바인딩을 만들어줍니다. 특정 큐가 특정 exchange 를 바라보게 하고 라우팅 키로 woker.one 을 사용하게 합니다.

workerTwoBinding 도 유사하게 이해하면 됩니다.



### MessageController

이제 메시지 생성 요청을 받고 exchange 로 메시지를 발행하는 Controller 코드를 보겠습니다.

```java
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final RabbitTemplate rabbitTemplate;

    public MessageController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity produceMessage(@PathVariable String workerId, @RequestBody Message message) {
        System.out.println(message + " message is delivering");
        rabbitTemplate.convertAndSend("task-exchange", "worker.".concat(workerId), message.getMessage());
        return ResponseEntity.ok(null);
    }

}
```

위 코드에서 엔드포인트 */worker/{workerId}* 에 요청을 보내면 exchange 에 라우팅 키를 함께 동봉해서 메시지를 발행하게 됩니다.



우리가 기존에 가지고 있던 큐를 생각해보겠습니다.

1. workerOne
2. workerTwo



이 각각의 큐에 라우팅 키 바인딩을 했습니다.

wokerOne : worker.one

workerTwo : worker.two



그럼 라우팅 키가 worker.one 인 경우는 exchange 에서 workerOne 에 메시지를 전달해줍니다.

worker.two 인 경우는 workerTwo 에 발행하겠죠.



이렇게 producer 코드는 끝이 났습니다.



> **정리**
>
> producer 는 consumer 가 메시지를 어떻게 사용하던 관심이 없다.
>
> 오직 exchange 에 전달하고 exchange 가 어디로 메시지를 전달할 것인가에 대해서 관심이 있다.





# Consumer Application

Consumer 또한 관심있는건 어떤 큐에 들어오는 메시지를 사용할 것인가에 대해서 관심합니다.

코드도 무척 간단합니다.

### 

스프링 부트 프로젝트를 아래 의존성을 추가하고 생성해줍니다.



### 의존성

```xml
<dependencies>
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-amqp</artifactId>
   </dependency>

   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
   </dependency>
   <dependency>
      <groupId>org.springframework.amqp</groupId>
      <artifactId>spring-rabbit-test</artifactId>
      <scope>test</scope>
   </dependency>
```

### applicaton.yml

```yaml
spring:
  rabbitmq:
    host: localhost
    username: ops0
    password: ops0
```

### Listener

```java
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
```

여기서 딱 하나 새롭게 생성되는 클래스입니다.

스프링 부트에서 제공하는 @RabbitListener 를 사용하여 쉽게 설정할 수 있습니다.

각 메소드가 producer 에서 생성한 큐 workerOne, workerTwo 를 관심하고 있죠.



이상입니다.


