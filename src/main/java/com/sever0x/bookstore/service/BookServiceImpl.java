package com.sever0x.bookstore.service;

import com.sever0x.bookstore.mapper.BookMapper;
import com.sever0x.bookstore.model.Book;
import com.sever0x.bookstore.proto.*;
import com.sever0x.bookstore.repository.BookRepository;
import com.sever0x.bookstore.validation.BookValidator;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class BookServiceImpl extends BookServiceGrpc.BookServiceImplBase {

    private final BookMapper bookMapper;

    private final BookValidator bookValidator;

    private final BookRepository bookRepository;

    @Override
    @Transactional
    public void addBook(AddBookRequest addBookRequest, StreamObserver<BookResponse> responseObserver) {
        validateRequest(addBookRequest, responseObserver);
        BookResponse bookResponse = bookMapper.bookToBookResponse(
                bookRepository.save(bookMapper.addBookRequestToBook(addBookRequest))
        );
        responseObserver.onNext(bookResponse);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getBook(GetBookRequest request, StreamObserver<BookResponse> responseObserver) {
        UUID bookId = UUID.fromString(request.getId());
        bookRepository.findById(bookId)
                .map(bookMapper::bookToBookResponse)
                .ifPresentOrElse(
                        responseObserver::onNext,
                        () -> responseObserver.onError(Status.NOT_FOUND
                                .withDescription("Book not found with id: " + bookId)
                                .asRuntimeException())
                );
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getBooks(GetBooksRequest getBooksRequest, StreamObserver<GetBooksResponse> responseObserver) {
        Page<Book> books = bookRepository.findAll(getBooksPageable(getBooksRequest));
        GetBooksResponse getBooksResponse = GetBooksResponse.newBuilder()
                .setCurrentPage(getBooksRequest.getPageNumber())
                .setTotalPages(books.getTotalPages())
                .addAllBooks(books.map(bookMapper::bookToBookResponse))
                .build();

        responseObserver.onNext(getBooksResponse);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void updateBook(UpdateBookRequest updateBookRequest, StreamObserver<BookResponse> responseObserver) {
        validateRequest(updateBookRequest, responseObserver);
        Book book = bookMapper.updateBookRequestToBook(updateBookRequest);
        bookRepository.save(book);

        responseObserver.onNext(bookMapper.bookToBookResponse(book));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void deleteBook(DeleteBookRequest request, StreamObserver<DeleteBookResponse> responseObserver) {
        UUID bookId = UUID.fromString(request.getId());
        if (!bookRepository.existsById(bookId)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Book not found with id: " + request.getId())
                    .asRuntimeException());
        }
        bookRepository.deleteById(bookId);
        DeleteBookResponse deleteBookResponse = DeleteBookResponse.newBuilder()
                .setId(request.getId())
                .setSuccess(true)
                .build();
        responseObserver.onNext(deleteBookResponse);
        responseObserver.onCompleted();
    }

    private Pageable getBooksPageable(GetBooksRequest getBooksRequest) {
        return PageRequest.of(getBooksRequest.getPageNumber(), getBooksRequest.getPageSize(),
                Sort.by(Sort.Direction.fromString(getBooksRequest.getDirection()), getBooksRequest.getSortBy()));
    }

    private void validateRequest(Object request, StreamObserver<?> responseObserver) {
        if (!bookValidator.isValidRequest(request)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid request data").asRuntimeException());
        }
    }
}
