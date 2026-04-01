public class Protocol {
    public static final byte REQUEST = 1;
    public static final byte DATA = 2;
    public static final byte ACK = 3;
    public static final byte ERROR = 4;

    public static final byte FLAG_FINAL = 1;
    public static final int HEADER_SIZE = 14;
    public static final int DEFAULT_TIMEOUT_MS = 1000;
}