import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.Arrays;

public class Server {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        int timeout = Integer.parseInt(args[1]);

        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(timeout);

        System.out.println("Server listening on port " + port);

        while (true) {
            try {
                byte[] buf = new byte[2048];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                socket.receive(dp);

                Packet request = Packet.fromBytes(dp.getData(), dp.getLength());

                if (request.messageType != Protocol.REQUEST) {
                    sendError(socket, request.connectionId,
                            "Invalid message type",
                            dp.getAddress(), dp.getPort());
                    continue;
                }

                if (request.payload.length < 4) {
                    sendError(socket, request.connectionId,
                            "Malformed request payload",
                            dp.getAddress(), dp.getPort());
                    continue;
                }

                int connectionId = request.connectionId;

                ByteBuffer reqPayload = ByteBuffer.wrap(request.payload);
                int segmentSize = reqPayload.getInt();

                if (segmentSize <= 0) {
                    sendError(socket, connectionId,
                            "Invalid segment size",
                            dp.getAddress(), dp.getPort());
                    continue;
                }

                byte[] filenameBytes = new byte[request.payload.length - 4];
                reqPayload.get(filenameBytes);
                String filename = new String(filenameBytes, StandardCharsets.UTF_8);

                System.out.println("Requested file: " + filename);
                System.out.println("Segment size: " + segmentSize);
                System.out.println("Client: " + dp.getAddress() + ":" + dp.getPort());

                File file = new File("files", filename);

                if (!file.exists() || !file.isFile()) {
                    sendError(socket, connectionId,
                            "File not found",
                            dp.getAddress(), dp.getPort());
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte sequence = 0;

                    while (true) {
                        byte[] chunk = new byte[segmentSize];
                        int bytesRead = fis.read(chunk);

                        if (bytesRead == -1) {
                            break;
                        }

                        byte[] actualData = Arrays.copyOf(chunk, bytesRead);

                        byte flags = 0;
                        if (bytesRead < segmentSize) {
                            flags |= Protocol.FLAG_FINAL;
                        }

                        Packet dataPacket = new Packet(
                                connectionId,
                                sequence,
                                Protocol.DATA,
                                flags,
                                actualData
                        );

                        byte[] dataBytes = dataPacket.toBytes();
                        DatagramPacket response = new DatagramPacket(
                                dataBytes,
                                dataBytes.length,
                                dp.getAddress(),
                                dp.getPort()
                        );

                        boolean acked = false;

                        while (!acked) {
                            socket.send(response);
                            System.out.println("Sent DATA seq=" + sequence +
                                               " len=" + bytesRead +
                                               " final=" + ((flags & Protocol.FLAG_FINAL) != 0));

                            try {
                                byte[] ackBuf = new byte[2048];
                                DatagramPacket ackDp = new DatagramPacket(ackBuf, ackBuf.length);
                                socket.receive(ackDp);

                                Packet ackPacket = Packet.fromBytes(ackDp.getData(), ackDp.getLength());

                                boolean validAck =
                                        ackPacket.messageType == Protocol.ACK &&
                                        ackPacket.connectionId == connectionId &&
                                        ackPacket.sequenceNumber == sequence;

                                if (validAck) {
                                    System.out.println("Received valid ACK for seq=" + sequence);
                                    acked = true;
                                } else {
                                    System.out.println("Received invalid ACK. Ignoring...");
                                }

                            } catch (SocketTimeoutException e) {
                                System.out.println("Timeout waiting for ACK seq=" + sequence + ". Retransmitting...");
                            }
                        }

                        if ((flags & Protocol.FLAG_FINAL) != 0) {
                            System.out.println("Final packet acknowledged. Transfer complete.");
                            break;
                        }

                        sequence = (byte) (1 - sequence);
                    }
                }

            } catch (Exception e) {
                System.out.println("Error handling request: " + e.getMessage());
            }
        }
    }
    private static void sendError(
            DatagramSocket socket,
            int connectionId,
            String message,
            InetAddress address,
            int port
    ) throws IOException {

        Packet errorPacket = new Packet(
                connectionId,
                0,
                Protocol.ERROR,
                (byte) 0,
                message.getBytes(StandardCharsets.UTF_8)
        );

        byte[] errorBytes = errorPacket.toBytes();

        DatagramPacket response = new DatagramPacket(
                errorBytes,
                errorBytes.length,
                address,
                port
        );

        socket.send(response);
        System.out.println("Sent ERROR: " + message);
    }
}
