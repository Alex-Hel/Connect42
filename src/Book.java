import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Book {
    // entries
    private ConcurrentHashMap<BookEntry,BookEntry> map;
    // used to approximate approach of limit of transposition table usefulness
    static AtomicInteger replacements = new AtomicInteger();
    // empty book
    public Book() {
        map = new ConcurrentHashMap<>();
    }
    // read saved book data from file
    public Book(String fileName) {
        try {
            map = new ConcurrentHashMap<>();
            Scanner s = new Scanner(new File(fileName));
            long n = s.nextLong();
            for (long i=0L; i<n; i++) {
                BookEntry temp = new BookEntry(s.nextLong(),s.nextLong(),s.nextLong(),s.nextInt(), s.nextInt(), s.nextFloat());
                map.put(temp,temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // add entry, does nothing if full to avoid memory filling
    public void put(BookEntry be) {
        if (map.size() >= 20_000_000) { // for transposition tables (only thing that SHOULD have this many entries, and transposition table is safe to clear)
            return;
        }
        map.put(be,be);
    }
    // bypasses size limit by assuming parameter will replace another entry
    public void replace(BookEntry be) {
        replacements.incrementAndGet();
        map.put(be,be);
    }
    // returns 'equal' entry or null to avoid double hashing
    public BookEntry getOrNull(BookEntry be) {
        return map.getOrDefault(be,null);
    }
    // returns size of map
    public int size() {
        return map.size();
    }
    public boolean putIfAbsent(BookEntry be) {
        return map.putIfAbsent(be, be) == null;
    }
    // returns if the entry or an 'equal' entry is contained
    public boolean containsKey(BookEntry be) {
        return (map.containsKey(be));
    }
    public String toString() {
        return ""+map.size();
    }
    // writes the book data to file
    public boolean write(String fileName) {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            out.print(map.size() + "\n");
            for (BookEntry be : map.keySet()) {
                be.write(out);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    // clears all entries on a certain depth
    public void clearDepth(int depth) {
        for (BookEntry be : map.keySet()) {
            if (be.depth == depth) {
                map.remove(be);
            }
        }
    }
}
