package com.nequi.ticketing.infrastructure.adapter.out.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.out.OrderQueuePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * SQS adapter that enqueues purchase orders for async processing.
 */
@Component
public class SqsOrderProducer implements OrderQueuePort {

    private static final Logger log = LoggerFactory.getLogger(SqsOrderProducer.class);

    private final SqsAsyncClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsOrderProducer(SqsAsyncClient sqsClient,
                             ObjectMapper objectMapper,
                             @Value("${aws.sqs.order-queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public Mono<Void> enqueue(Order order) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(order))
                .flatMap(messageBody -> {
                    SendMessageRequest request = SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(messageBody)
                            .messageGroupId(order.orderId())
                            .messageDeduplicationId(order.orderId())
                            .build();

                    return Mono.fromFuture(sqsClient.sendMessage(request));
                })
                .doOnSuccess(r -> log.info("Order [{}] enqueued to SQS. MessageId: {}",
                        order.orderId(), r.messageId()))
                .doOnError(error -> log.error("Failed to enqueue order [{}]", order.orderId(), error))
                .then();
    }
}
