import java.io.PrintWriter;

public class BookEntry {
    // locating data
    private long zobrist;
    private long red;
    private long yellow;
    // retrieved information
    int depth;
    int move;
    float confidence;
    // entry for getting from map
    public BookEntry(long z, long r, long y) {
        zobrist=z;
        red=r;
        yellow=y;
    }
    // entry for book moves, already max accuracy
    public BookEntry(long z, long r, long y, int m) {
        zobrist=z;
        red=r;
        yellow=y;
        move = m;
    }
    // entry for transposition table, variable accuracy
    public BookEntry(long z, long r, long y, int d, int m, float c) {
        zobrist=z;
        red=r;
        yellow=y;
        depth = d;
        move = m;
        confidence = c;
    }
    // strong fingerprint, little collisions
    public int hashCode() {
        return Long.hashCode(zobrist);
    }
    // absolute positional identity
    public boolean equals(Object obj) {
        BookEntry other = (BookEntry)obj;
        return (this.red == other.red) && (this.yellow == other.yellow);
    }
    // writes all data to file
    public void write(PrintWriter out) {
        out.print(zobrist + " " + red + " " + yellow + " " + move + "\n");
    }
    public String toString() {
        return zobrist + " " + red + " " + yellow + " " + move;
    }
}