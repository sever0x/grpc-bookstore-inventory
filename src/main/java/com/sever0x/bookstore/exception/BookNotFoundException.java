package com.sever0x.bookstore.exception;

public class BookNotFoundException extends RuntimeException {

    private String bookId;

    public BookNotFoundException(String bookId) {
        super("Book not found with id: " + bookId);
        this.bookId = bookId;
    }

    public String getBookId() {
        return bookId;
    }
}
