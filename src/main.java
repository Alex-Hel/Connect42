public class main {
    public static void main(String[] args) {
        Board b = new Board();
        long start = System.currentTimeMillis();
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    System.out.println("Program stopping...");
                    System.out.println((System.currentTimeMillis()-start)/1000.0/b.book.size()*130_000);
                    Board.saveWeights();
                    Board.saveBook();
                })
        );
        b.generateBook(0);
    }
}