import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws Exception {
        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String filename = args[2];
        int segmentSize = Integer.parseInt(args[3]);

        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(serverIp);

        int connectionId = 12345;

        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);

        ByteBuffer payloadBuffer = ByteBuffer.allocate(4 + filenameBytes.length);
        payloadBuffer.putInt(segmentSize);
        payloadBuffer.put(filenameBytes);

        Packet request = new Packet(
                connectionId,
                0,
                Protocol.REQUEST,
                (byte) 0,
                payloadBuffer.array()
        );

        byte[] data = request.toBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, address, serverPort);
        socket.send(dp);

        System.out.println("Sent request:");
        System.out.println(request);

        socket.close();
    }
}
