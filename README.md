
# gRPC Bookstore Inventory Service

This is a gRPC service for managing a bookstore inventory. It provides the following functionality:

- Adding a new book
- Retrieving information about a book by its identifier
- Retrieving a list of books with pagination and sorting capabilities
- Updating information about an existing book
- Deleting a book by its identifier

## Prerequisites

- Java 21 or later
- Gradle
- Docker

## Setup

1. Clone the repository:

```  
git clone https://github.com/sever0x/grpc-bookstore-inventory.git  
```  

2. Build the project:

```  
./gradlew build  
```  

## Running the Application

To run the application without Docker, you need to set the following environment variables:

- `DB_URL`: The URL for the PostgreSQL database (e.g., `jdbc:postgresql://localhost:5432/bookstore`)
- `DB_USERNAME`: The username for the PostgreSQL database
- `DB_PASSWORD`: The password for the PostgreSQL database

In IntelliJ IDEA, you can set these environment variables through the "Edit Configuration" for the `GrpcBookstoreInventoryApplication`:

## Running Tests

To run the unit tests, execute the following command:

```  
./gradlew test  
```  

This will run all the tests in the project.  (Docker required)

## Docker

The project includes a Docker Compose configuration for running the application and its dependencies (PostgreSQL database) in containers.

To build and run the containers, follow these steps:

1. Make sure you have Docker and Docker Compose installed.

2. Create an `.env` file in the project root directory with the following environment variables:

```  
DB_URL=jdbc:postgresql://postgres:5432/bookstore  
DB_USERNAME=your_db_username  
DB_PASSWORD=your_db_password  
```  

Replace `your_db_username` and `your_db_password` with the desired values.

3. Build and start the containers:

```  
docker-compose up --build  
```  

This will build the application Docker image and start the gRPC service and PostgreSQL containers.

The gRPC service will be available on `http://localhost:9090`.

## Working with bookstore-inventory

You can use Postman to interact with the gRPC service.

### Adding a Book
```json
{
  "title": "Book Title",
  "author": "Book Author",
  "isbn": "1234567890123",
  "quantity": 10
}
```

### Retrieving a Book

```json
{
  "id": "63c1bed0-b60a-4e4f-8cd6-ef77055b908a"
}
```

### Retrieving a List of Books

```json
{
  "direction": "asc",
  "pageNumber": 0,
  "pageSize": 5,
  "sortBy": "title"
}
```

### Updating a Book
```json
{
  "id": "book_id",
  "title": "Updated Book Title",
  "author": "Updated Book Author",
  "isbn": "9876543210987",
  "quantity": 15
}
```

### Deleting a Book


```json
{
  "id": "book_id"
}
```
