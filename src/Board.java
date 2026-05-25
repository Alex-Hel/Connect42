import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    // depth setting for minimax
    private static final int maxDepth = 10;

    // neural network
    private static Network network;
    static {
        //network = new Network(.05f,84,64,64,1);
        network = new Network("weights.txt");
    }

    // book moves
    static Book book;
    static {
        book = new Book();
    }

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

    // vfx
    private int posX;
    private int posY;

    // starting board
    public Board() {
        red = 0;
        yellow = 0;
        redMove = true;
        hash = 0;
        posX = -100;
        posY = -100;
    }

    public Board(Board other) {
        this.red = other.red;
        this.yellow = other.yellow;
        this.redMove = other.redMove;
        this.outcome = other.outcome;
        this.hash = other.hash;
    }

    // play move
    private boolean placeTile(int r) {
        long occupied = red | yellow;
        int base = (6 - r) * 7;

        if (((occupied >> base) & 0b111111L) == 0b111111L)
            return false;

        int row = Long.numberOfTrailingZeros(~((occupied >> base) & 0b111111L));
        long move = 1L << (base + row);

        if (redMove) {
            red |= move;
            if (checkWin(red)) outcome = 1;
            hash ^= redHashes[r * 6 + row];
        } else {
            yellow |= move;
            if (checkWin(yellow)) outcome = -1;
            hash ^= yellowHashes[r * 6 + row];
        }
        redMove = !redMove;
        return true;
    }

    // undo move
    private void undoMove(int r) {
        int base = (6 - r) * 7;

        if (!redMove) { // undo red
            long col = (red >> base) & 0b111111L;
            long top = Long.highestOneBit(col);
            int row = Long.numberOfTrailingZeros(top);

            red ^= top << base;
            hash ^= redHashes[r * 6 + row];
        }
        else { // undo yellow
            long col = (yellow >> base) & 0b111111L;
            long top = Long.highestOneBit(col);
            int row = Long.numberOfTrailingZeros(top);

            yellow ^= top << base;
            hash ^= yellowHashes[r * 6 + row];
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

        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 6; c++) {
                int bit = (6 - r) * 7 + c;
                long mask = 1L << bit;

                if ((red & mask) != 0) {
                    g.setColor(Color.RED);
                } else if ((yellow & mask) != 0) {
                    g.setColor(Color.YELLOW);
                } else {
                    continue;
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
        if (!startRed) botTurn(redMove);
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
                Thread.sleep(1);
            } catch (Exception e) {} //sleepin
            botTurn(redMove);
            paintImmediately(getBounds());
            if (outcome != 0) {
                break;
            }
            try {
                Thread.sleep(1);
            } catch (Exception e) {} //sleepin
            botTurn(redMove);
            paintImmediately(getBounds());
            if (outcome != 0) {
                break;
            }
        }
    }
    private void botTurn(boolean isRed) {
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
        List<Integer> legal = legalMoves();

        if (outcome != 0) { // result of ended game adjusted to minimize depth
            return (outcome == ((isRed) ? 1 : -1)) ? 1-(maxDepth-depth)/1000f : -1+(maxDepth-depth)/1000f;
        } else if (depth == 0) {
            return evaluate(isRed); // handcrafted not ending leaf node
        } else if (legal.isEmpty()) {
            return 0; // draw
        }

        if (maximizing) {
            float best = Float.NEGATIVE_INFINITY;
            for (int move : legal) {
                placeTile(move);
                float score = minimax(depth - 1, false, isRed, alpha, beta);
                undoMove(move);

                best = Math.max(best, score);
                alpha = Math.max(alpha, best);

                if (beta <= alpha) {
                    break; // beta
                }
            }
            return best;
        } else {
            float best = Float.POSITIVE_INFINITY;
            for (int move : legal) {
                placeTile(move);
                float score = minimax(depth - 1, true, isRed, alpha, beta);
                undoMove(move);

                best = Math.min(best, score);
                beta = Math.min(beta, best);

                if (beta <= alpha) {
                    break; // alpha
                }
            }
            return best;
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
        list.sort(Comparator.comparingInt(a -> Math.abs(3 - a))); //try center first
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
    public BookEntry toBookEntry(int depth) {
        return new BookEntry(hash,red,yellow,depth);
    }
    static AtomicInteger cnt = new AtomicInteger();
    public void generateBook(int depth) {
        List<Integer> moves = legalMoves();
        if (depth >= 7) return;
        else {
            int c = cnt.incrementAndGet();
            if ((c & 1023) == 0) System.out.println(c);
            if (outcome != 0) return;
            else if (moves.isEmpty()) return;
        }
        //System.out.println(depth);
        BookEntry entry = toBookEntry(depth);

        // Skip if already explored
        if (book.containsKey(entry)) return;
        book.putIfAbsent(entry,getBookMove(redMove));

        // Only parallelize root level
        if (depth == 0) {
            ExecutorService pool = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors()
            );

            for (int move : moves) {
                Board copy = new Board(this);

                pool.submit(() -> {
                    copy.placeTile(move);
                    copy.generateBook(depth + 1);
                });
            }

            pool.shutdown();

            try {
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return;
        }

        // Normal recursive search
        for (int move : moves) {
            placeTile(move);
            generateBook(depth + 1);
            undoMove(move);
        }
    }
    private int getBookMove(boolean isRed) {
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

        return bestMove;
    }
    public static void saveWeights() {
        network.write("weights.txt");
    }
    public static void saveBook() {
        book.write("book.txt");
    }
}
