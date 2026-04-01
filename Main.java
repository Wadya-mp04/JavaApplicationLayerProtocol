public class Main {
    public static void main(String[] args) {

        Packet test = new Packet(1,0, (byte)1, (byte)0,"hello".getBytes());
        byte[] data = test.toBytes();

        System.out.println("Original:");
        System.out.println(test);
        Packet reconstructed = Packet.fromBytes(data, data.length);

        System.out.println("Reconstructed:");
        System.out.println(reconstructed);
    }
}