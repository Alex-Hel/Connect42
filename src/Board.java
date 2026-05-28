import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Board extends JPanel {
    // board
    private long red;
    private long yellow;
    private boolean redMove;
    private byte outcome;

    // handcrafted weights
    private float openThree = .1f;
    private float openTwo = .1f*.413f;
    private float center = .1f*.0167f;

    // neural network
    private static Network network;
    static {
        //network = new Network(.05f,84,64,64,1);
        network = new Network("weights.txt");
    }

    // book moves
    private static Book book;
    private Book transpositionTable;
    static {
        book = new Book("book.txt");
    }
    private static ThreadPoolExecutor pool =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors()-2
            );
    private static final int threads = Runtime.getRuntime().availableProcessors()-2;
    private static AtomicInteger pendingTasks = new AtomicInteger();
    // depth setting for minimax
    private static final int maxDepth = 12;
    private static int bookDepth;

    // zobrist hashing
    private static final int key = 67;
    private long hash;
    private static final long[] redHashes;
    private static final long[] yellowHashes;
    private static final Random rand = new Random(key);
    static {
        redHashes = new long[42];
        yellowHashes = new long[42];
        for (int i=0; i<42; i++) {
            redHashes[i] = rand.nextLong();
            yellowHashes[i] = rand.nextLong();
        }
    }

    // for move check ordering optimization
    private int previousMove = 3;

    // hold mirror board for pruning
    private long mirrorRed;
    private long mirrorYellow;

    private long mirrorHash;

    // vfx
    private int posX;
    private int posY;

    // starting board
    public Board() {
        red = 0;
        yellow = 0;
        hash = 0;
        redMove = true;
        mirrorRed = 0;
        mirrorYellow = 0;
        mirrorHash = 0;
        posX = -100;
        posY = -100;
        transpositionTable = new Book();
    }

    public Board(Board other) {
        this.red = other.red;
        this.yellow = other.yellow;
        this.hash = other.hash;
        this.mirrorRed = other.mirrorRed;
        this.mirrorYellow = other.mirrorYellow;
        this.mirrorHash = other.mirrorHash;
        this.redMove = other.redMove;
        this.outcome = other.outcome;
        this.transpositionTable = other.transpositionTable;
    }

    // play move
    private boolean placeTile(int r) {
        long occupied = red | yellow;
        int base = (6 - r) * 7;
        int mirrorBase = (r) * 7;
        if (((occupied >> base) & 0b111111L) == 0b111111L)
            return false;

        int row = Long.numberOfTrailingZeros(~((occupied >> base) & 0b111111L));
        long move = 1L << (base + row);
        long mirrorMove = 1L << (mirrorBase + row);

        if (redMove) {
            red |= move;
            mirrorRed |= mirrorMove;
            if (checkWin(red)) outcome = 1;
            hash ^= redHashes[r * 6 + row];
            mirrorHash ^= redHashes[(6-r) * 6 + row];
        } else {
            yellow |= move;
            mirrorYellow |= mirrorMove;
            if (checkWin(yellow)) outcome = -1;
            hash ^= yellowHashes[r * 6 + row];
            mirrorHash ^= yellowHashes[(6-r) * 6 + row];
        }
        redMove = !redMove;
        previousMove = r;
        return true;
    }

    // undo move
    private void undoMove(int r) {
        int base = (6 - r) * 7;
        int mirrorBase = (r) * 7;
        if (!redMove) { // undo red
            long col = (red >> base) & 0b111111L;
            long top = Long.highestOneBit(col);
            int row = Long.numberOfTrailingZeros(top);

            long mirrorMove = 1L << (mirrorBase + row);

            red ^= top << base;
            mirrorRed ^= mirrorMove;
            hash ^= redHashes[r * 6 + row];
            mirrorHash ^= redHashes[(6-r) * 6 + row];
        }
        else { // undo yellow
            long col = (yellow >> base) & 0b111111L;
            long top = Long.highestOneBit(col);
            int row = Long.numberOfTrailingZeros(top);

            long mirrorMove = 1L << (mirrorBase + row);

            yellow ^= top << base;
            mirrorYellow ^= mirrorMove;
            hash ^= yellowHashes[r * 6 + row];
            mirrorHash ^= yellowHashes[(6-r) * 6 + row];
        }

        outcome = 0;
        redMove = !redMove;
    }

    // returns if board is a win
    private boolean checkWin(long board) {
        long m;

        // vertical
        m = board & (board >> 1);
        if ((m & (m >> 2)) != 0) return true;

        // horizontal
        m = board & (board >> 7);
        if ((m & (m >> 14)) != 0) return true;

        // diagonal /
        m = board & (board >> 6);
        if ((m & (m >> 12)) != 0) return true;

        // diagonal \
        m = board & (board >> 8);
        if ((m & (m >> 16)) != 0) return true;

        return false;
    }

    // executes visual updates
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // clears old drawings


        g.setColor(Color.BLUE);
        /*/
        g.drawLine(100, 0, 100, 600);
        g.drawLine(200, 0, 200, 600);
        g.drawLine(300, 0, 300, 600);
        g.drawLine(400, 0, 400, 600);
        g.drawLine(500, 0, 500, 600);
        g.drawLine(600, 0, 600, 600);

        g.drawLine(0, 100, 700, 100);
        g.drawLine(0, 200, 700, 200);
        g.drawLine(0, 300, 700, 300);
        g.drawLine(0, 400, 700, 400);
        g.drawLine(0, 500, 700, 500);
        g.drawLine(0, 600, 700, 600);
         */
        g.fillRect(0,0,700,600);

        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 6; c++) {
                int bit = (6 - r) * 7 + c;
                long mask = 1L << bit;

                if ((red & mask) != 0) {
                    g.setColor(Color.RED);
                } else if ((yellow & mask) != 0) {
                    g.setColor(Color.YELLOW);
                } else {
                    g.setColor(Color.WHITE);
                }

                int x = r * 100;
                int y = (5 - c) * 100;

                g.fillOval(x, y, 100, 100);
            }
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Ariel",Font.BOLD,24));
        g.drawString((outcome == 0) ? (redMove) ? "REDS MOVE" : "YELLOWS MOVE" : (outcome == 1) ? "RED WINS!" : "YELLOW WINS!",275,50);
    }

    // play against in person other human
    public void playLocal() {
        JFrame frame = new JFrame("board");
        frame.add(this);
        frame.setSize(700,650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int row = e.getX() / 100;
                    if (!placeTile(row)) throw new RuntimeException("ILLEGAL MOVE");
                    repaint();
                    if (outcome != 0) {
                        removeMouseListener(this);
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        });
    }

    // play against either AI
    public void playAI(boolean startRed) {
        JFrame frame = new JFrame("board");
        frame.add(this);
        frame.setSize(700,650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // bot first move if it is red
        if (!startRed) {
            botTurn(redMove);
            paintImmediately(getBounds());
        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int row = e.getX() / 100;
                    if (!placeTile(row)) throw new InvalidParameterException("ILLEGAL MOVE");
                    paintImmediately(getBounds());
                    if (outcome != 0) {
                        throw new RuntimeException();
                    }
                    botTurn(redMove);
                    repaint();
                    if (outcome != 0) {
                        throw new RuntimeException();
                    }
                } catch (InvalidParameterException ex) {
                    System.out.println(ex);
                }catch (Exception ex) {
                    removeMouseListener(this);
                }
            }
        });
    }
    public void spectateAI() {
        JFrame frame = new JFrame("board");
        frame.add(this);
        frame.setSize(700,650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        while (true) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {} //sleepin
            botTurn(redMove);
            paintImmediately(getBounds());
            if (outcome != 0) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (Exception e) {} //sleepin
            botTurn(redMove);
            paintImmediately(getBounds());
            if (outcome != 0) {
                break;
            }
        }
    }
    private void botTurn(boolean isRed) {
        int bookMove = retrieveBookMove(book);
        if (bookMove != -1) {
            placeTile(bookMove);
            return;
        }
        int bestMove = -1;
        float bestScore = Float.NEGATIVE_INFINITY;

        float alpha = Float.NEGATIVE_INFINITY;
        float beta = Float.POSITIVE_INFINITY;

        for (int move : legalMoves()) {
            placeTile(move);
            float score = minimax(maxDepth - 1, false, isRed, alpha, beta);

            undoMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            // Update alpha for the root level
            alpha = Math.max(alpha, score);
        }

        if (bestMove != -1) {
            placeTile(bestMove);
        }
    }

    private float minimax(int depth, boolean maximizing, boolean isRed, float alpha, float beta) {
        if (outcome != 0) { // result of ended game adjusted to minimize depth
            return (outcome == ((isRed) ? 1 : -1)) ? 1-(maxDepth-depth)/1000f : -1+(maxDepth-depth)/1000f;
        }
        BookEntry be = transpositionTable.getOrNull(toShallowBookEntry());
        if (be != null && be.depth >= depth) {
            return be.confidence;
        }
        if (depth == 0) {
            return evaluate(isRed); // handcrafted non ending leaf node
        }
        List<Integer> legal = legalMoves();
        if (legal.isEmpty()) {
            return 0; // draw
        }
        boolean exact = true;
        if (maximizing) {
            float bestScore = Float.NEGATIVE_INFINITY;
            int bestMove = -1;
            for (int move : legal) {
                placeTile(move);
                float score = minimax(depth - 1, false, isRed, alpha, beta);
                undoMove(move);

                if (bestScore < score) {
                    bestScore = score;
                    bestMove = move;
                }
                alpha = Math.max(alpha, bestScore);

                if (beta <= alpha) {
                    exact = false;
                    break; // beta
                }
            }
            if (exact) {
                be = transpositionTable.getOrNull(toShallowBookEntry()); // race condition;
                if (be == null) {
                    transpositionTable.put(toDeepBookEntry(depth,bestMove,bestScore));
                } else if (be.depth < depth) {
                    transpositionTable.replace(toDeepBookEntry(depth,bestMove,bestScore));
                }
            }
            return bestScore;
        } else {
            float bestScore = Float.POSITIVE_INFINITY;
            int bestMove = -1;
            for (int move : legal) {
                placeTile(move);
                float score = minimax(depth - 1, true, isRed, alpha, beta);
                undoMove(move);

                if (bestScore > score) {
                    bestScore = score;
                    bestMove = move;
                }
                beta = Math.min(beta, bestScore);

                if (beta <= alpha) {
                    exact = false;
                    break; // alpha
                }
            }
            if (exact) {
                be = transpositionTable.getOrNull(toShallowBookEntry()); // race condition
                if (be == null) {
                    transpositionTable.put(toDeepBookEntry(depth,bestMove,bestScore));
                } else if (be.depth < depth) {
                    transpositionTable.replace(toDeepBookEntry(depth,bestMove,bestScore));
                }
            }
            return bestScore;
        }
    }
    public List<Integer> legalMoves() {
        long occupied = red | yellow;
        List<Integer> list = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            int base = (6 - i) * 7;

            if (((occupied >> base) & 0b111111L) != 0b111111L)
                list.add(i);
        }
        list.sort(Comparator.comparingInt(a -> Math.abs(previousMove - a))); // assume important to address new threats
        return list;
    }
    private float evaluate(boolean isRed) {
        float score = 0;

        score -= openThree * countOpenThrees(isRed ? yellow : red);
        score -= openTwo * countOpenTwos(isRed ? yellow : red);
        score -= center * centerBonus(isRed ? yellow : red);

        score += openThree * countOpenThrees(isRed ? red : yellow);
        score += openTwo * countOpenTwos(isRed ? red : yellow);
        score += center * centerBonus(isRed ? red : yellow);

        return score;
    }
    private int countOpenThrees(long board) {
        return countPatterns(board, 3);
    }
    private int countOpenTwos(long board) {
        return countPatterns(board, 2);
    }
    private int countPatterns(long board, int target) {
        int count = 0;
        long occupied = red | yellow;

        // horizontal
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 6; c++) {
                if (matchesWindow(board, occupied, r, c, 1, 0, target))
                    count++;
            }
        }

// vertical
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 3; c++) {
                if (matchesWindow(board, occupied, r, c, 0, 1, target))
                    count++;
            }
        }

        // diagonal /
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                if (matchesWindow(board, occupied, r, c, 1, 1, target))
                    count++;
            }
        }

        // diagonal \
        for (int r = 3; r < 7; r++) {
            for (int c = 0; c < 3; c++) {
                if (matchesWindow(board, occupied, r, c, -1, 1, target))
                    count++;
            }
        }

        return count;
    }
    private boolean matchesWindow(long board, long occupied, int r, int c, int dr, int dc, int target) {

        int mine = 0;
        int empty = 0;

        for (int i = 0; i < 4; i++) {
            int col = r + dr * i;
            int row = c + dc * i;

            int bit = (6 - col) * 7 + row;
            long mask = 1L << bit;

            if ((board & mask) != 0) {
                mine++;
            } else if ((occupied & mask) == 0) {
                empty++;
            } else {
                return false; // opponent piece
            }
        }

        return mine == target && empty == 4 - target;
    }
    private int centerBonus(long board) {
        int score = 0;

        // center column = column 3
        for (int c = 0; c < 6; c++) {
            int bit = (6 - 3) * 7 + c;
            long mask = 1L << bit;

            if ((board & mask) != 0) {
                score += 3;
            }
        }

        return score;
    }

    public float[] toData() {
        float[] data = new float[84];

        int idx = 0;
        for (int col = 0; col < 7; col++) {
            int base = (6 - col) * 7;
            for (int row = 0; row < 6; row++) {
                int bit = base + row;
                data[idx]      = (float)((red  >> bit) & 1L);
                data[idx + 42] = (float)((yellow >> bit) & 1L);
                idx++;
            }
        }

        return data;
    }
    private int retrieveBookMove(Book b) {
        BookEntry e = b.getOrNull(toShallowBookEntry());
        if (e == null) return -1;
        else if (hash < mirrorHash) {
            return e.move;
        }
        else {
            return 6-e.move;
        }
    }
    public BookEntry toShallowBookEntry() {
        if (hash < mirrorHash) {
            return new BookEntry(hash,red,yellow);
        } else {
            return new BookEntry(mirrorHash,mirrorRed,mirrorYellow);
        }
    }
    public BookEntry toDeepBookEntry(int depth, int move, float confidence) {
        if (hash < mirrorHash) {
            return new BookEntry(hash,red,yellow,depth,move,confidence);
        } else {
            return new BookEntry(mirrorHash,mirrorRed,mirrorYellow,depth,6-move,confidence);
        }
    }

    static AtomicInteger cnt;
    public void generateBook(int toDepth) {
        Scanner s = new Scanner(System.in);
        System.out.println("are you sure you want to generate book moves? ");
        String str = s.next();
        if (!str.toLowerCase().equals("yes")) {
            return;
        }
        s.close();

        cnt = new AtomicInteger();
        bookDepth = toDepth;
        pendingTasks.set(1);
        pool.submit(() -> {
            try {
                genBook(0);
            } finally {
                pendingTasks.decrementAndGet();
            }
        });

        // wait until no tasks remain
        while (pendingTasks.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        pool.shutdown();

        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void genBook(int depth) {
        List<Integer> moves = legalMoves();
        if (depth > bookDepth) return;
        else {
            int c = cnt.incrementAndGet();
            if ((c & 1023) == 0) {
                Runtime rt = Runtime.getRuntime();
                long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                long max  = rt.maxMemory() / (1024 * 1024);

                System.out.println(
                        "nodes=" + cnt.get()
                                + " TT=" + transpositionTable.size()
                                + " mem=" + used + "/" + max + " MB"
                                + " book=" + book.size()
                                + " replaces=" + Book.replacements.get()
                );
            }
            if (outcome != 0) return;
            else if (moves.isEmpty()) return;
        }

        BookEntry entry = toShallowBookEntry();
        if (!book.containsKey(entry)) { // only recurse to finish, don't minimax
            book.put(entry);
            BookEntry temp = getBookEntry(redMove);
            book.put(temp);
        }

        // Normal recursive search
        for (int move : moves) {
            if (depth < 4 && pool.getActiveCount() + pool.getQueue().size() < threads * 2) {
                pendingTasks.incrementAndGet();
                Board copy = new Board(this);
                pool.submit(() -> {
                    try {
                        copy.placeTile(move);
                        copy.genBook(depth + 1);
                    } finally {
                        pendingTasks.decrementAndGet();
                    }
                });
            } else {
                placeTile(move);
                genBook(depth + 1);
                undoMove(move);
            }
        }
    }
    private BookEntry getBookEntry(boolean isRed) {
        int bestMove = -1;
        float bestScore = Float.NEGATIVE_INFINITY;

        float alpha = Float.NEGATIVE_INFINITY;
        float beta = Float.POSITIVE_INFINITY;

        for (int move : legalMoves()) {
            placeTile(move);
            float score = minimax(12 - 1, false, isRed, alpha, beta);

            undoMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            // Update alpha for the root level
            alpha = Math.max(alpha, score);
        }
        if (hash < mirrorHash) {
            return new BookEntry(hash,red,yellow,12,bestMove,bestScore);
        } else {
            return new BookEntry(mirrorHash,mirrorRed,mirrorYellow,12,6-bestMove,bestScore);
        }
    }
    public static void saveWeights() {
        network.write("weights.txt");
    }
    public static void saveBook() {
        book.write("book.txt");
    }
}
