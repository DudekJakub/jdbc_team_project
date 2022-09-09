package com.ormanager.orm;

import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.jdbc.ConnectionToDB;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static com.ormanager.orm.mapper.ObjectMapper.mapperToList;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class UnitTests {

    private static OrmManager ormManager;

    @BeforeAll
    static void setUp() throws SQLException, NoSuchFieldException {
        ormManager = OrmManager.withPropertiesFrom("src/test/resources/application_test.properties");
        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("drop table if exists books, publishers;")) {
            preparedStatement.execute();
        }
        var entityClassesAsSet = ClassScanner.getClassesMarkedAsEntity();
        var entityClassesAsArray = new Class<?>[entityClassesAsSet.size()];
        entityClassesAsSet.toArray(entityClassesAsArray);
        ormManager.register(entityClassesAsArray);
        ormManager.createRelationships(entityClassesAsArray);
    }

    @Test
    void whenUsingFindAllAsIterableTest_ShouldBeLazyLoading() throws Exception {
        //GIVEN
        Publisher publisher1 = new Publisher("saveTestPublisher1");
        Publisher publisher2 = new Publisher("saveTestPublisher2");
        Publisher publisher3 = new Publisher("saveTestPublisher3");

        //WHEN
        ormManager.getOrmCache().clearCache();
        ormManager.save(publisher1);
        ormManager.save(publisher2);
        ormManager.save(publisher3);
        ormManager.getOrmCache().deleteFromCache(publisher1);
        ormManager.getOrmCache().deleteFromCache(publisher2);
        ormManager.getOrmCache().deleteFromCache(publisher3);
        var iterator = ormManager.findAllAsIterable(Publisher.class);
        int counter=0;
        while (iterator.hasNext() && counter<1){
            counter++;
            iterator.next();
        }
        iterator.close();

        //THEN
        assertEquals(ormManager.getOrmCache().count(Publisher.class), counter);
    }

    @Test
    void save_ShouldReturnAutoGeneratedIdOfPublisherFromDatabase() throws SQLException {
        //GIVEN
        Publisher publisher = new Publisher("test");
        Long expectedId;

        //WHEN
        ormManager.save(publisher);
        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Publishers WHERE id = (SELECT MAX(id) from Publishers);")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = resultSet.getLong(1);
        }

        //THEN
        assertEquals(expectedId, publisher.getId());
    }

    @Test
    void save_ShouldReturnAutoGeneratedIdOfBookFromDatabase() throws SQLException {
        //GIVEN
        Book book = new Book("test", LocalDate.now());

        var pub = new Publisher("test");
        ormManager.save(pub);
        book.setPublisher(pub);
        Long expectedId;

        //WHEN
        ormManager.save(book);
        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Books WHERE id = (SELECT MAX(id) from Books);")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = resultSet.getLong(1);
        }

        //THEN
        assertEquals(expectedId, book.getId());
    }

    @Test
    void findById_ShouldReturnPublisherFromDatabaseByGivenId() {
        //GIVEN
        Publisher publisher = new Publisher("test");
        Long expectedId = 0L;

        //WHEN
        ormManager.save(publisher);
        var id = publisher.getId();

        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Publishers WHERE id = " + id + ";")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = resultSet.getLong(1);
        } catch (SQLException e) {
            LOGGER.info(e.toString());
        }

        //THEN
        assertEquals(publisher, ormManager.findById(publisher.getId(), Publisher.class).orElseThrow());
        assertEquals(expectedId, publisher.getId());
    }

    @Test
    void findById_ShouldReturnBookFromDatabaseByGivenId() {
        //GIVEN
        Book book = new Book("test", LocalDate.now());
        Long expectedId = 0L;

        //WHEN
        var pub = new Publisher("test");
        ormManager.save(pub);
        book.setPublisher(pub);
        ormManager.save(book);

        var id = book.getId();

        try (Connection connection = ConnectionToDB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Books WHERE id = " + id + ";")) {
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            resultSet.next();
            expectedId = resultSet.getLong(1);
        } catch (SQLException e) {
            LOGGER.info(e.toString());
        }

        //THEN
        assertEquals(book, ormManager.findById(book.getId(), Book.class).orElseThrow());
        assertEquals(expectedId, book.getId());
    }

    @Test
    void findByIdPublisherNullValue_ShouldReturnNoSuchElementException() {
        //GIVEN
        Publisher publisher = new Publisher();

        //THEN
        assertThrows(NoSuchElementException.class, () -> ormManager.findById(publisher.getId(), Publisher.class));
    }

    @Test
    void findByIdBookNullValue_ShouldReturnNoSuchElementException() {
        //GIVEN
        Book book = new Book();

        //THEN
        assertThrows(NoSuchElementException.class, () -> ormManager.findById(book.getId(), Book.class));
    }

    @Test
    void findAllPublishersTest() throws SQLException {
        //GIVEN
        Publisher publisher = new Publisher("saveTestPublisher");
        List<Publisher> publishers;

        //WHEN
        ormManager.save(publisher);
        try (Connection connection = ConnectionToDB.getConnection()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM Publishers;");
            publishers = mapperToList(resultSet, Publisher.class);
        }
        var findAllList = ormManager.findAll(Publisher.class);

        //THEN
        assertTrue(publishers.size() > 0);
        assertEquals(findAllList.size(), publishers.size());
    }

    @Test
    void findAllBooksTest() throws SQLException {
        //GIVEN
        Book book = new Book("test", LocalDate.now());
        List<Book> books;

        //WHEN
        var pub = new Publisher("test");
        ormManager.save(pub);
        book.setPublisher(pub);
        ormManager.save(book);

        try (Connection connection = ConnectionToDB.getConnection()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM Books;");
            books = mapperToList(resultSet, Book.class);
        }
        var findAllList = ormManager.findAll(Book.class);

        //THEN
        assertTrue(books.size() > 0);
        assertEquals(findAllList.size(), books.size());
    }

    @Test
    void givenPublisherIsUpdated_thenAssertId() throws IllegalAccessException {
        //GIVEN
        Publisher publisher = new Publisher("Test1");

        //WHEN
        ormManager.save(publisher);
        var id = publisher.getId();
        Publisher publisher1 = ormManager.findById(id, Publisher.class).get();

        //THEN
        assertEquals(id, ((Publisher) ormManager.update(publisher1)).getId());
    }

    @Test
    void givenBookIsUpdated_thenAssertId() throws IllegalAccessException {
        //GIVEN
        Publisher publisher = new Publisher("Test1");
        ormManager.save(publisher);
        Book book = new Book("Harry Potter", LocalDate.now());
        book.setPublisher(publisher);

        //WHEN
        ormManager.save(book);
        var id = book.getId();
        Book book1 = ormManager.findById(id, Book.class).get();

        //THEN
        assertEquals(id, ((Book) ormManager.update(book1)).getId());
    }

    @Test
    void givenPublisherSetNewName_whenUpdatePublisher_thenAssertName() throws IllegalAccessException {
        //GIVEN
        Publisher publisher = new Publisher("Test1");

        //WHEN
        ormManager.save(publisher);
        var id = publisher.getId();
        Publisher publisher1 = ormManager.findById(id, Publisher.class).get();

        publisher1.setName("Test2");
        var name = ((Publisher) ormManager.update(publisher1)).getName();

        //THEN
        assertEquals("Test1", name);
    }

    @Test
    void givenBookSetNewTitle_whenUpdatePublisher_thenAssertTitle() throws IllegalAccessException {
        //GIVEN
        Publisher publisher = new Publisher("Test2");
        ormManager.save(publisher);
        Book book = new Book("Lord of the rings", LocalDate.now());
        book.setPublisher(publisher);

        //WHEN
        ormManager.save(book);
        var id = book.getId();
        Book book1 = ormManager.findById(id, Book.class).get();

        book1.setTitle("Alice in the wonderland");
        var title = ((Book) ormManager.update(book1)).getTitle();

        //THEN
        assertEquals("Lord of the rings", title);
    }

    @Test
    void whenDeletingPublisher_ShouldDeletePublisherAndBooksAndSetIdToNull() {
        //GIVEN
        Publisher publisher = new Publisher("testPub");
        ormManager.save(publisher);
        Book book = new Book("testBook", LocalDate.now());
        book.setPublisher(publisher);
        ormManager.save(book);
        publisher.getBooks().add(book);
        //WHEN
        ormManager.delete(publisher);
        //THEN
        assertNull(publisher.getId());
        assertNull(book.getId());
    }

    @Test
    void whenDeletingBook_ShouldDeleteBookAndSetIdToNull() {
        //GIVEN
        Publisher publisher = new Publisher("testPub");
        ormManager.save(publisher);
        Book book = new Book("testBook", LocalDate.now());
        book.setPublisher(publisher);
        ormManager.save(book);
        publisher.getBooks().add(book);
        //WHEN
        ormManager.delete(book);
        //THEN
        assertNull(book.getId());
    }
}
