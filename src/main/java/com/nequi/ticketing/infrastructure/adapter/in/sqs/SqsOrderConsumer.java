package com.nequi.ticketing.infrastructure.adapter.in.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.in.ProcessOrderUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SqsOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsOrderConsumer.class);
    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 20;
    private static final int VISIBILITY_TIMEOUT_SECONDS = 30;

    private final SqsAsyncClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ProcessOrderUseCase processOrderUseCase;
    private final String queueUrl;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("sqs-consumer-", 0).factory()
    );

    public SqsOrderConsumer(SqsAsyncClient sqsClient,
                             ObjectMapper objectMapper,
                             ProcessOrderUseCase processOrderUseCase,
                             @Value("${aws.sqs.order-queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.processOrderUseCase = processOrderUseCase;
        this.queueUrl = queueUrl;
    }

    @PostConstruct
    public void startPolling() {
        running.set(true);
        log.info("Starting SQS consumer for queue: {}", queueUrl);
        executor.execute(this::pollLoop);
    }

    @PreDestroy
    public void stopPolling() {
        running.set(false);
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(MAX_MESSAGES)
                        .waitTimeSeconds(WAIT_TIME_SECONDS)
                        .visibilityTimeout(VISIBILITY_TIMEOUT_SECONDS)
                        .build();

                sqsClient.receiveMessage(request)
                        .thenAccept(response -> response.messages().forEach(this::handleMessage))
                        .join();

            } catch (Exception e) {
                log.error("Error in SQS polling loop", e);
                try { Thread.sleep(1000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void handleMessage(Message message) {
        log.info("Processing SQS message: {}", message.messageId());
        try {
            Order order = objectMapper.readValue(message.body(), Order.class);

            processOrderUseCase.execute(order.orderId())
                    .doOnSuccess(processed ->
                            deleteMessage(message.receiptHandle())
                                    .subscribe())
                    .doOnError(error ->
                            log.error("Failed to process order [{}], message will be requeued",
                                    order.orderId(), error))
                    .subscribe();

        } catch (Exception e) {
            log.error("Failed to deserialize SQS message: {}", message.messageId(), e);
        }
    }

    private Mono<Void> deleteMessage(String receiptHandle) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        return Mono.fromFuture(sqsClient.deleteMessage(request)).then();
    }
}
