package com.sever0x.bookstore.config;

import com.sever0x.bookstore.proto.BookServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class GrpcClientTestConfiguration {

    @Bean
    public BookServiceGrpc.BookServiceBlockingStub bookServiceStub() {
        return BookServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress("localhost", 9090)
                        .usePlaintext()
                        .build()
        );
    }
}