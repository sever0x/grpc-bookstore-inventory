package com.sever0x.bookstore.service;

import com.sever0x.bookstore.mapper.BookMapper;
import com.sever0x.bookstore.model.Book;
import com.sever0x.bookstore.proto.*;
import com.sever0x.bookstore.repository.BookRepository;
import com.sever0x.bookstore.validation.BookValidator;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class BookServiceImpl extends BookServiceGrpc.BookServiceImplBase {

    private final BookMapper bookMapper;

    private final BookValidator bookValidator;

    private final BookRepository bookRepository;

    @Override
    @Transactional
    public void addBook(AddBookRequest request, StreamObserver<BookResponse> responseObserver) {
        handleInvalidRequest(request.getTitle(), request.getAuthor(), request.getIsbn(), request.getQuantity(), responseObserver);
        BookResponse bookResponse = bookMapper.bookToBookResponse(
                bookRepository.save(bookMapper.addBookRequestToBook(request))
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
                        () -> handleNotFound(bookId, responseObserver)
                );
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getBooks(GetBooksRequest request, StreamObserver<GetBooksResponse> responseObserver) {
        Page<Book> books = bookRepository.findAll(getBooksPageable(request));
        GetBooksResponse response = GetBooksResponse.newBuilder()
                .setCurrentPage(request.getPageNumber())
                .setTotalPages(books.getTotalPages())
                .addAllBooks(books.map(bookMapper::bookToBookResponse))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void updateBook(UpdateBookRequest request, StreamObserver<BookResponse> responseObserver) {
        handleInvalidRequest(request.getTitle(), request.getAuthor(), request.getIsbn(), request.getQuantity(), responseObserver);
        Book book = bookMapper.updateBookRequestToBook(request);
        bookRepository.save(book);

        responseObserver.onNext(bookMapper.bookToBookResponse(book));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void deleteBook(DeleteBookRequest request, StreamObserver<DeleteBookResponse> responseObserver) {
        UUID bookId = UUID.fromString(request.getId());
        if (!bookRepository.existsById(bookId)) {
            handleNotFound(bookId, responseObserver);
            return;
        }
        bookRepository.deleteById(bookId);
        DeleteBookResponse response = DeleteBookResponse.newBuilder()
                .setId(request.getId())
                .setSuccess(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private Pageable getBooksPageable(GetBooksRequest request) {
        return PageRequest.of(request.getPageNumber(), request.getPageSize(),
                Sort.by(Sort.Direction.fromString(request.getDirection()), request.getSortBy()));
    }

    private void handleInvalidRequest(String title, String author, String isbn, int quantity,
                                      StreamObserver<?> responseObserver) {
        BookValidator.ValidationResult validationResult = bookValidator.validateRequest(title, author, isbn, quantity);

        if (validationResult.hasErrors()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(String.join(", ", validationResult.getErrors()))
                    .asRuntimeException());
        }
    }

    private void handleNotFound(UUID bookId, StreamObserver<?> responseObserver) {
        responseObserver.onError(Status.NOT_FOUND
                .withDescription("Book not found with id: " + bookId)
                .asRuntimeException());
    }
}