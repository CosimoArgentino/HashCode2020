import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

class Book {
    private final int id;
    private final int score;

    public Book(int id, int score) {
        this.id = id;
        this.score = score;
    }

    public int getId() {
        return id;
    }

    public int getScore() {
        return score;
    }
}

class Library {
    private final int id;
    private final int signupDays;
    private final int booksShippedPerDay;
    private final Set<Book> availableBooks;
    private double computedScore;

    public Library(int id, int signupDays, int booksShippedPerDay, Set<Book> availableBooks) {
        this.id = id;
        this.signupDays = signupDays;
        this.booksShippedPerDay = booksShippedPerDay;
        this.availableBooks = availableBooks;
    }

    public double getComputedScore() {
        return this.computedScore;
    }

    public void updateComputedScore(Set<Integer> alreadyScannedBooks) {
        this.computedScore =
                this.availableBooks
                        .stream()
                        .filter(b -> !alreadyScannedBooks.contains(b.getId()))
                        .map(Book::getScore)
                        .reduce(0, Integer::sum)
                        * (
                        1
                                -
                                (double) this.signupDays
                                        * this.booksShippedPerDay
                                        / this.availableBooks.size()
                );
    }

    public int getId() {
        return id;
    }

    public int getSignupDays() {
        return signupDays;
    }

    public int getBooksShippedPerDay() {
        return booksShippedPerDay;
    }

    public Set<Book> getAvailableBooks() {
        return availableBooks;
    }
}

class LibraryOrderPlan {
    private final Library library;
    private final List<Book> orderedBooks;

    public LibraryOrderPlan(Library library, List<Book> orderedBooks) {
        this.library = library;
        this.orderedBooks = orderedBooks;
    }

    public String buildHumanReadableRepresentation() {
        return String.format(
                "Library %d orders the following books:%n%s%n%n",
                this.library.getId(),
                this.library.getAvailableBooks()
                        .stream()
                        .sorted(Comparator.comparing(Book::getScore).reversed())
                        .map(b -> String.valueOf(b.getId()))
                        .collect(Collectors.joining(", "))
        );
    }

    public String buildRepresentation() {
        return String.format(
                "%d %d%n%s%n",
                this.library.getId(),
                this.library.getAvailableBooks().size(),
                this.library.getAvailableBooks()
                        .stream()
                        .sorted(Comparator.comparing(Book::getScore).reversed())
                        .map(b -> String.valueOf(b.getId()))
                        .collect(Collectors.joining(" "))
        );
    }

    public Library getLibrary() {
        return library;
    }

    public List<Book> getOrderedBooks() {
        return orderedBooks;
    }
}

class Solution {
    private final List<LibraryOrderPlan> orderPlans;

    public Solution(List<LibraryOrderPlan> orderPlans) {
        this.orderPlans = orderPlans;
    }

    public String buildHumanReadableRepresentation() {
        return String.format(
                "%d libraries%n%s",
                this.orderPlans.size(),
                this.orderPlans
                        .stream()
                        .map(LibraryOrderPlan::buildHumanReadableRepresentation)
                        .collect(Collectors.joining())
        );
    }

    public String buildRepresentation() {
        return String.format(
                "%d%n%s",
                this.orderPlans.size(),
                this.orderPlans
                        .stream()
                        .map(LibraryOrderPlan::buildRepresentation)
                        .collect(Collectors.joining())
        );
    }

    public List<LibraryOrderPlan> getOrderPlans() {
        return orderPlans;
    }
}

public class Main {
    private static Solution findSimpleSolution(
            int days,
            Map<Integer, Book> booksById,
            List<Library> libraries
    ) {
        return new Solution(
                libraries
                        .stream()
                        .sorted(Comparator.comparing(Library::getComputedScore))
                        .map(l -> new LibraryOrderPlan(
                                l,
                                l.getAvailableBooks()
                                        .stream()
                                        .sorted(Comparator.comparing(Book::getScore).reversed())
                                        .collect(Collectors.toList())
                        ))
                        .collect(Collectors.toList())
        );
    }

    private static Solution findSolution(
            int days,
            Map<Integer, Book> booksById,
            List<Library> libraries
    ) {
        Set<Library> librariesClone = new HashSet<>(libraries);
        Set<Integer> alreadyScannedBooks = new HashSet<>();
        List<LibraryOrderPlan> orderPlan = new ArrayList<>();
        int bogus = 1000;
        while (!librariesClone.isEmpty()) {
            Library bestLibrary =
                    librariesClone
                            .stream()
                            .sorted(Comparator.comparing(Library::getComputedScore).reversed())
                            .findFirst()
                            .orElseThrow(IllegalStateException::new);
            bestLibrary.getAvailableBooks().forEach(b -> alreadyScannedBooks.remove(b.getId()));
            orderPlan.add(
                    new LibraryOrderPlan(
                            bestLibrary,
                            bestLibrary.getAvailableBooks()
                                    .stream()
                                    .sorted(Comparator.comparing(Book::getScore).reversed())
                                    .collect(Collectors.toList())
                    )
            );
            librariesClone.remove(bestLibrary);
            librariesClone.forEach(l -> l.updateComputedScore(alreadyScannedBooks));
            bogus--;
            if (bogus == 0) {
                bogus = 1000;
            }
        }
        return new Solution(orderPlan);
    }

    private static int simulate(Solution solution, int days) {
        int totalScore = 0;
        int actualDay = 0;
        Set<Integer> scannedBooks = new HashSet<>();
        Iterator<LibraryOrderPlan> libraryIterator = solution.getOrderPlans().iterator();
        while (libraryIterator.hasNext()) {
            LibraryOrderPlan library = libraryIterator.next();
            actualDay += library.getLibrary().getSignupDays();
            if (actualDay >= days) {
                break;
            }
            long booksToOrder =
                    library.getLibrary().getBooksShippedPerDay()
                            * (long) (days - actualDay);
            totalScore +=
                    library.getOrderedBooks()
                            .stream()
                            .limit(booksToOrder)
                            .filter(b -> !scannedBooks.contains(b.getId()))
                            .map(Book::getScore)
                            .reduce(0, Integer::sum);
            library.getOrderedBooks()
                    .stream()
                    .forEach(b -> scannedBooks.add(b.getId()));
        }

        return totalScore;
    }

    public static void main(String[] args) {
        int booksCount;
        int librariesCount;
        int days;
        Map<Integer, Book> booksById;
        List<Library> libraries;
        try (Scanner in = new Scanner(System.in)) {
            booksCount = in.nextInt();
            librariesCount = in.nextInt();
            days = in.nextInt();
            booksById = new HashMap<>();
            for (int i = 0; i < booksCount; i++) {
                booksById.put(i, new Book(i, in.nextInt()));
            }
            libraries = new ArrayList<>();
            for (int i = 0; i < librariesCount; i++) {
                int booksCountPerLibrary = in.nextInt();
                int signupDays = in.nextInt();
                int booksShippedPerDay = in.nextInt();
                Set<Book> availableBooks = new HashSet<>();
                for (int j = 0; j < booksCountPerLibrary; j++) {
                    availableBooks.add(booksById.get(in.nextInt()));
                }
                libraries.add(new Library(i, signupDays, booksShippedPerDay, availableBooks));
            }
        }
        Solution solution = findSolution(days, booksById, libraries);
        if (false) {
            // DEBUG ONLY
            System.out.print(solution.buildHumanReadableRepresentation());
            System.out.println(simulate(solution, days));
        } else {
            System.out.print(solution.buildRepresentation());
        }
    }
}
