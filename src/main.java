public class main {
    public static void main(String[] args) {
        Board b = new Board();
        long start = System.currentTimeMillis();
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    System.out.println("Program stopping...");
                    //Board.saveWeights();
                    Board.saveBook();
                    System.out.println(((System.currentTimeMillis()-start)/1000)/60 +" Minutes.");
                })
        );
        b.generateBook(6);
    }
}