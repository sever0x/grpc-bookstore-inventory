package com.sever0x.bookstore.service;

import com.sever0x.bookstore.mapper.BookMapper;
import com.sever0x.bookstore.proto.AddBookRequest;
import com.sever0x.bookstore.proto.BookResponse;
import com.sever0x.bookstore.proto.BookServiceGrpc;
import com.sever0x.bookstore.repository.BookRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class BookServiceImpl extends BookServiceGrpc.BookServiceImplBase {

    private final BookMapper bookMapper;

    private final BookRepository bookRepository;

    @Override
    public void addBook(AddBookRequest addBookRequest, StreamObserver<BookResponse> streamObserver) {
        BookResponse bookResponse = bookMapper.bookToBookResponse(
                bookRepository.save(bookMapper.addBookRequestToBook(addBookRequest))
        );
        streamObserver.onNext(bookResponse);
        streamObserver.onCompleted();
    }
}
