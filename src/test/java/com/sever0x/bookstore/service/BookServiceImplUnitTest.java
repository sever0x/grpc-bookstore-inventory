package com.sever0x.bookstore.service;

import com.sever0x.bookstore.mapper.BookMapper;
import com.sever0x.bookstore.model.Book;
import com.sever0x.bookstore.proto.*;
import com.sever0x.bookstore.repository.BookRepository;
import com.sever0x.bookstore.validation.BookValidator;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplUnitTest {

    private BookServiceImpl bookService;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BookValidator bookValidator;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StreamObserver<BookResponse> responseObserver;

    @Mock
    private StreamObserver<GetBooksResponse> getBooksResponseObserver;

    @Mock
    private StreamObserver<DeleteBookResponse> deleteBookResponseObserver;

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl(bookMapper, bookValidator, bookRepository);
    }

    @Test
    void addBook_shouldAddBookSuccessfully() {
        AddBookRequest request = AddBookRequest.newBuilder()
                .setTitle("Book Title")
                .setAuthor("Book Author")
                .setIsbn("1234567890123")
                .setQuantity(10)
                .build();

        Book book = new Book();
        book.setTitle("Book Title");
        book.setAuthor("Book Author");
        book.setIsbn("1234567890123");
        book.setQuantity(10);

        BookResponse expectedResponse = BookResponse.newBuilder()
                .setTitle("Book Title")
                .setAuthor("Book Author")
                .setIsbn("1234567890123")
                .setQuantity(10)
                .build();

        when(bookValidator.validateRequest(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new BookValidator.ValidationResult());
        when(bookMapper.addBookRequestToBook(request)).thenReturn(book);
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.bookToBookResponse(book)).thenReturn(expectedResponse);

        bookService.addBook(request, responseObserver);

        verify(responseObserver, times(1)).onNext(expectedResponse);
        verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    void addBook_shouldHandleInvalidRequest() {
        AddBookRequest request = AddBookRequest.newBuilder()
                .setTitle("")
                .setAuthor("Book Author")
                .setIsbn("1234567890123")
                .setQuantity(10)
                .build();

        BookValidator.ValidationResult validationResult = new BookValidator.ValidationResult();
        validationResult.addError("Title must not be blank");

        when(bookValidator.validateRequest(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(validationResult);

        try {
            bookService.addBook(request, responseObserver);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
            assertEquals("Title must not be blank", e.getStatus().getDescription());
        }
    }

    @Test
    void getBook_shouldReturnBookSuccessfully() {
        String bookId = UUID.randomUUID().toString();
        GetBookRequest request = GetBookRequest.newBuilder().setId(bookId).build();

        Book book = new Book();
        book.setId(UUID.fromString(bookId));
        book.setTitle("Book Title");
        book.setAuthor("Book Author");
        book.setIsbn("1234567890123");
        book.setQuantity(10);

        BookResponse expectedResponse = BookResponse.newBuilder()
                .setId(bookId)
                .setTitle("Book Title")
                .setAuthor("Book Author")
                .setIsbn("1234567890123")
                .setQuantity(10)
                .build();

        when(bookRepository.findById(UUID.fromString(bookId))).thenReturn(Optional.of(book));
        when(bookMapper.bookToBookResponse(book)).thenReturn(expectedResponse);

        bookService.getBook(request, responseObserver);

        verify(responseObserver, times(1)).onNext(expectedResponse);
        verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    void getBook_shouldHandleBookNotFound() {
        String bookId = UUID.randomUUID().toString();
        GetBookRequest request = GetBookRequest.newBuilder().setId(bookId).build();

        when(bookRepository.findById(UUID.fromString(bookId))).thenReturn(Optional.empty());

        try {
            bookService.getBook(request, responseObserver);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            assertEquals("Book not found with id: " + bookId, e.getStatus().getDescription());
        }
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    void getBooks_shouldReturnBooksSuccessfully() {
        int pageNumber = 0;
        int pageSize = 10;
        String sortBy = "title";
        String direction = "ASC";

        GetBooksRequest request = GetBooksRequest.newBuilder()
                .setPageNumber(pageNumber)
                .setPageSize(pageSize)
                .setSortBy(sortBy)
                .setDirection(direction)
                .build();

        Book book1 = new Book(UUID.randomUUID(), "Book 1", "Author 1", "1234567890123", 5);
        Book book2 = new Book(UUID.randomUUID(), "Book 2", "Author 2", "2345678901234", 3);
        List<Book> books = Arrays.asList(book1, book2);

        BookResponse bookResponse1 = BookResponse.newBuilder()
                .setId(book1.getId().toString())
                .setTitle(book1.getTitle())
                .setAuthor(book1.getAuthor())
                .setIsbn(book1.getIsbn())
                .setQuantity(book1.getQuantity())
                .build();

        BookResponse bookResponse2 = BookResponse.newBuilder()
                .setId(book2.getId().toString())
                .setTitle(book2.getTitle())
                .setAuthor(book2.getAuthor())
                .setIsbn(book2.getIsbn())
                .setQuantity(book2.getQuantity())
                .build();

        Page<Book> booksPage = new PageImpl<>(books);

        GetBooksResponse expectedResponse = GetBooksResponse.newBuilder()
                .setCurrentPage(pageNumber)
                .setTotalPages(booksPage.getTotalPages())
                .addAllBooks(Arrays.asList(bookResponse1, bookResponse2))
                .build();

        when(bookRepository.findAll(any(Pageable.class))).thenReturn(booksPage);
        when(bookMapper.bookToBookResponse(book1)).thenReturn(bookResponse1);
        when(bookMapper.bookToBookResponse(book2)).thenReturn(bookResponse2);

        bookService.getBooks(request, getBooksResponseObserver);

        verify(getBooksResponseObserver, times(1)).onNext(expectedResponse);
        verify(getBooksResponseObserver, times(1)).onCompleted();
    }

    @Test
    void updateBook_shouldUpdateBookSuccessfully() {
        String bookId = UUID.randomUUID().toString();
        UpdateBookRequest request = UpdateBookRequest.newBuilder()
                .setId(bookId)
                .setTitle("Updated Title")
                .setAuthor("Updated Author")
                .setIsbn("5678901234567")
                .setQuantity(20)
                .build();

        Book book = new Book();
        book.setId(UUID.fromString(bookId));
        book.setTitle("Updated Title");
        book.setAuthor("Updated Author");
        book.setIsbn("5678901234567");
        book.setQuantity(20);

        BookResponse expectedResponse = BookResponse.newBuilder()
                .setId(bookId)
                .setTitle("Updated Title")
                .setAuthor("Updated Author")
                .setIsbn("5678901234567")
                .setQuantity(20)
                .build();

        when(bookValidator.validateRequest(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new BookValidator.ValidationResult());
        when(bookMapper.updateBookRequestToBook(request)).thenReturn(book);
        when(bookMapper.bookToBookResponse(book)).thenReturn(expectedResponse);

        bookService.updateBook(request, responseObserver);

        verify(bookRepository, times(1)).save(book);
        verify(responseObserver, times(1)).onNext(expectedResponse);
        verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    void updateBook_shouldHandleInvalidRequest() {
        UpdateBookRequest request = UpdateBookRequest.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTitle("")
                .setAuthor("Updated Author")
                .setIsbn("5678901234567")
                .setQuantity(20)
                .build();

        BookValidator.ValidationResult validationResult = new BookValidator.ValidationResult();
        validationResult.addError("Title must not be blank");

        when(bookValidator.validateRequest(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(validationResult);

        try {
            bookService.updateBook(request, responseObserver);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
            assertEquals("Title must not be blank", e.getStatus().getDescription());
        }
    }

    @Test
    void deleteBook_shouldDeleteBookSuccessfully() {
        // Arrange
        String bookId = UUID.randomUUID().toString();
        DeleteBookRequest request = DeleteBookRequest.newBuilder().setId(bookId).build();

        DeleteBookResponse expectedResponse = DeleteBookResponse.newBuilder()
                .setId(bookId)
                .setSuccess(true)
                .build();

        when(bookRepository.existsById(UUID.fromString(bookId))).thenReturn(true);

        bookService.deleteBook(request, deleteBookResponseObserver);

        verify(bookRepository, times(1)).deleteById(UUID.fromString(bookId));
        verify(deleteBookResponseObserver, times(1)).onNext(expectedResponse);
        verify(deleteBookResponseObserver, times(1)).onCompleted();
    }

    @Test
    void deleteBook_shouldHandleBookNotFound() {
        String bookId = UUID.randomUUID().toString();
        DeleteBookRequest request = DeleteBookRequest.newBuilder().setId(bookId).build();

        when(bookRepository.existsById(UUID.fromString(bookId))).thenReturn(false);

        try {
            bookService.deleteBook(request, deleteBookResponseObserver);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            assertEquals("Book not found with id: " + bookId, e.getStatus().getDescription());
        }
    }
}
