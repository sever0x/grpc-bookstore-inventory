package com.sever0x.bookstore.service;

import com.sever0x.bookstore.config.GrpcClientTestConfiguration;
import com.sever0x.bookstore.proto.*;
import com.sever0x.bookstore.repository.BookRepository;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.citrusframework.actions.ExecuteSQLQueryAction.Builder.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "grpc.server.inProcessName=test",
                "grpc.server.port=9090",
                "grpc.client.testClient.address=in-process:test"
        }
)
@Testcontainers
@ExtendWith(CitrusExtension.class)
@Import(GrpcClientTestConfiguration.class)
class BookServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.0")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    private final String dataSourceUrl = postgres.getJdbcUrl();

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookServiceGrpc.BookServiceBlockingStub client;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", postgres::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", postgres::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void connectionEstablished() {
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    @CitrusTest
    void testAddBook(@CitrusResource TestCaseRunner runner) {
        AddBookRequest request = AddBookRequest.newBuilder()
                .setTitle("New Book Title")
                .setAuthor("New Book Author")
                .setIsbn("1234567890123")
                .setQuantity(5)
                .build();

        BookResponse response = client.addBook(request);
        assertEquals(request.getTitle(), response.getTitle());
        assertEquals(request.getAuthor(), response.getAuthor());
        assertEquals(request.getIsbn(), response.getIsbn());
        assertEquals(request.getQuantity(), response.getQuantity());

        runner.$(query(dataSource())
                .statement("SELECT count(*) FROM books WHERE id = '" + response.getId() +"'")
                .validate("count", "1"));
    }

    @Test
    @CitrusTest
    void testGetBook(@CitrusResource TestCaseRunner runner) {
        AddBookRequest addBookRequest = AddBookRequest.newBuilder()
                .setTitle("Book Title")
                .setAuthor("Book Author")
                .setIsbn("1234567890123")
                .setQuantity(13)
                .build();
        BookResponse book = client.addBook(addBookRequest);

        GetBookRequest request = GetBookRequest.newBuilder()
                .setId(book.getId())
                .build();

        BookResponse response = client.getBook(request);
        assertEquals(request.getId(), response.getId());

        runner.$(query(dataSource())
                .statement("SELECT title, author, isbn, quantity FROM books WHERE id = '" + response.getId() + "'")
                .validate("title", addBookRequest.getTitle())
                .validate("author", addBookRequest.getAuthor())
                .validate("isbn", addBookRequest.getIsbn())
                .validate("quantity", String.valueOf(addBookRequest.getQuantity()))
        );
    }

    @Test
    @CitrusTest
    void testUpdateBook(@CitrusResource TestCaseRunner runner) {
        AddBookRequest addRequest = AddBookRequest.newBuilder()
                .setTitle("Old Book Title")
                .setAuthor("Old Book Author")
                .setIsbn("0123456789012")
                .setQuantity(10)
                .build();
        BookResponse bookResponse = client.addBook(addRequest);

        UpdateBookRequest updateRequest = UpdateBookRequest.newBuilder()
                .setId(bookResponse.getId())
                .setTitle("Updated Book Title")
                .setAuthor("Updated Book Author")
                .setIsbn("9876543210987")
                .setQuantity(15)
                .build();

        BookResponse response = client.updateBook(updateRequest);
        assertEquals(updateRequest.getId(), response.getId());
        assertEquals(updateRequest.getTitle(), response.getTitle());
        assertEquals(updateRequest.getAuthor(), response.getAuthor());
        assertEquals(updateRequest.getIsbn(), response.getIsbn());
        assertEquals(updateRequest.getQuantity(), response.getQuantity());

        runner.$(query(dataSource())
                .statement("SELECT title, author, isbn, quantity FROM books WHERE id = '" + response.getId() + "'")
                .validate("title", updateRequest.getTitle())
                .validate("author", updateRequest.getAuthor())
                .validate("isbn", updateRequest.getIsbn())
                .validate("quantity", String.valueOf(updateRequest.getQuantity())));
    }

    @Test
    @CitrusTest
    void testDeleteBook(@CitrusResource TestCaseRunner runner) {
        AddBookRequest addRequest = AddBookRequest.newBuilder()
                .setTitle("Book to Delete")
                .setAuthor("Delete Author")
                .setIsbn("1111111111111")
                .setQuantity(20)
                .build();
        BookResponse bookResponse = client.addBook(addRequest);

        DeleteBookRequest deleteRequest = DeleteBookRequest.newBuilder()
                .setId(bookResponse.getId())
                .build();

        DeleteBookResponse response = client.deleteBook(deleteRequest);
        assertEquals(deleteRequest.getId(), response.getId());
        assertTrue(response.getSuccess());

        runner.$(query(dataSource())
                .statement("SELECT COUNT(*) FROM books WHERE id = '" + response.getId() + "'")
                .validate("count", "0"));
    }

    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(dataSourceUrl);
        dataSource.setUsername("test");
        dataSource.setPassword("test");
        return dataSource;
    }
}