package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    /** Every book when {@code author} is null, otherwise those whose author contains it (case-insensitive). */
    public List<Book> findAll(String author) {
        return repository.findAll().stream()
                .filter(b -> author == null || b.author().toLowerCase().contains(author.toLowerCase()))
                .sorted(Comparator.comparing(Book::id))
                .toList();
    }

    /** Sorted by title, author or year; empty when the sort key is unknown. */
    public Optional<List<Book>> findAllSorted(String sort) {
        Comparator<Book> comparator = switch (sort.toLowerCase()) {
            case "title" -> Comparator.comparing(Book::title, String.CASE_INSENSITIVE_ORDER);
            case "author" -> Comparator.comparing(Book::author, String.CASE_INSENSITIVE_ORDER);
            case "year" -> Comparator.comparingInt(Book::year);
            default -> null;
        };
        if (comparator == null) {
            return Optional.empty();
        }
        return Optional.of(repository.findAll().stream().sorted(comparator).toList());
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    public Book create(Book book) {
        return repository.save(new Book(null, book.title(), book.author(), book.year()));
    }

    /** Replaces the book with the given id; empty when it does not exist. */
    public Optional<Book> replace(long id, Book book) {
        return repository.findById(id)
                .map(existing -> repository.save(new Book(id, book.title(), book.author(), book.year())));
    }

    public boolean delete(long id) {
        return repository.deleteById(id);
    }
}
