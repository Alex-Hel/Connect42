import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Book {
    private ConcurrentHashMap<BookEntry,Integer> map;

    public Book() {
        map = new ConcurrentHashMap<>();
    }
    public Book(String fileName) {
        try {
            map = new ConcurrentHashMap<>();
            FastScanner s = new FastScanner(new File(fileName));
            long n = s.nextLong();
            for (long i=0L; i<n; i++) {
                map.put(new BookEntry(s.nextLong(),s.nextLong(),s.nextLong(),s.nextInt()),s.nextInt());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void put(BookEntry be, int m) {
        map.put(be,m);
    }
    public int size() {
        return map.size();
    }
    public boolean putIfAbsent(BookEntry be, int m) {
        return map.putIfAbsent(be, m) == null;
    }
    public boolean containsKey(BookEntry be) {
        return (map.containsKey(be));
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(map.size() + "\n");
        for (Map.Entry<BookEntry,Integer> e : map.entrySet()) {
            sb.append(e.getKey().toString() + " " + e.getValue() + "\n");
        }
        return sb.toString();
    }
    public boolean write(String fileName) {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            out.print(this.toString());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
class BookEntry {
    long zobrist;
    long red;
    long yellow;
    int depth;
    public BookEntry(long z, long r, long y, int d) {
        zobrist=z;
        red=r;
        yellow=y;
        depth = d;
    }
    public int hashCode() {
        return Long.hashCode(zobrist);
    }
    public boolean equals(Object obj) {
        BookEntry other = (BookEntry)obj;
        return (this.red == other.red) && (this.yellow == other.yellow);
    }
    public String toString() {
        return zobrist + " " + red + " " + yellow + " " + depth;
    }
}
