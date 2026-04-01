import java.net.*;
import java.io.*;

public class Server {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        DatagramSocket socket = new DatagramSocket(port);

        byte[] buf = new byte[2048];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);

        System.out.println("Server listening on port " + port);
        socket.receive(dp);

        Packet packet = Packet.fromBytes(dp.getData(), dp.getLength());
        System.out.println("Received:");
        System.out.println(packet);

        socket.close();
    }
}
