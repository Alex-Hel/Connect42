import java.io.*;
import java.util.*;


public class FastScanner {
    private BufferedReader br;
    private StringTokenizer st;


    private String delimiters = " \t\n\r\f";


    private String cachedToken = null;
    private Integer cachedInt = null;
    private Long cachedLong = null;
    private Double cachedDouble = null;
    private Boolean cachedBoolean = null;


    public FastScanner() {
        br = new BufferedReader(new InputStreamReader(System.in));
    }


    public FastScanner(File file) {
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public FastScanner(String s) {
        br = new BufferedReader(new StringReader(s));
    }


    public void useDelimiter(String d) {
        this.delimiters = d;
    }


    private boolean ensureToken() {
        if (cachedToken != null) return true;
        while (st == null || !st.hasMoreTokens()) {
            try {
                String line = br.readLine();
                if (line == null) return false;
                st = new StringTokenizer(line, delimiters);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }


    private String peekToken() {
        if (!ensureToken()) return null;
        if (cachedToken == null) {
            cachedToken = st.nextToken();
        }
        return cachedToken;
    }


    private void resetCaches() {
        cachedToken = null;
        cachedInt = null;
        cachedLong = null;
        cachedDouble = null;
        cachedBoolean = null;
    }


    public boolean hasNext() {
        return ensureToken();
    }


    public boolean hasNextInt() {
        if (cachedInt != null) return true;
        String token = peekToken();
        if (token == null) return false;
        try {
            cachedInt = Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public boolean hasNextLong() {
        if (cachedLong != null) return true;
        String token = peekToken();
        if (token == null) return false;
        try {
            cachedLong = Long.parseLong(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public boolean hasNextDouble() {
        if (cachedDouble != null) return true;
        String token = peekToken();
        if (token == null) return false;
        try {
            cachedDouble = Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public boolean hasNextBoolean() {
        if (cachedBoolean != null) return true;
        String token = peekToken();
        if (token == null) return false;
        if (token.equalsIgnoreCase("true") || token.equalsIgnoreCase("false")) {
            cachedBoolean = Boolean.parseBoolean(token);
            return true;
        }
        return false;
    }


    public String next() {
        if (cachedToken != null) {
            String ret = cachedToken;
            resetCaches();
            return ret;
        }
        if (!ensureToken()) return null;
        return st.nextToken();
    }


    public int nextInt() {
        if (cachedInt != null) {
            int ret = cachedInt;
            resetCaches();
            return ret;
        }
        return Integer.parseInt(next());
    }


    public long nextLong() {
        if (cachedLong != null) {
            long ret = cachedLong;
            resetCaches();
            return ret;
        }
        return Long.parseLong(next());
    }


    public double nextDouble() {
        if (cachedDouble != null) {
            double ret = cachedDouble;
            resetCaches();
            return ret;
        }
        return Double.parseDouble(next());
    }


    public boolean nextBoolean() {
        if (cachedBoolean != null) {
            boolean ret = cachedBoolean;
            resetCaches();
            return ret;
        }
        return Boolean.parseBoolean(next());
    }


    public String nextLine() {
        String str = "";
        try {
            if (cachedToken != null) {
                str = cachedToken;
                if (st.hasMoreTokens()) str += st.nextToken("\n");
                resetCaches();
                return str;
            }
            if (st != null && st.hasMoreTokens()) {
                str = st.nextToken("\n");
            } else {
                str = br.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return str;
    }
    public void close() {
        try {
            if (br != null) {
                br.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

