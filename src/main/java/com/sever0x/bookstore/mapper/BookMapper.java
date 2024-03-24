package com.sever0x.bookstore.mapper;

import com.sever0x.bookstore.model.Book;
import com.sever0x.bookstore.proto.AddBookRequest;
import com.sever0x.bookstore.proto.BookResponse;
import com.sever0x.bookstore.proto.UpdateBookRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookMapper {

    BookResponse bookToBookResponse(Book book);

    Book addBookRequestToBook(AddBookRequest addBookRequest);

    Book updateBookRequestToBook(UpdateBookRequest updateBookRequest);
}
