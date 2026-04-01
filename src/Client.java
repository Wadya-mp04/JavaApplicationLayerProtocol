import java.io.FileOutputStream;
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

        int connectionId = (int)(Math.random() * 1000000);

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

        byte[] buffer = new byte[2048];
        DatagramPacket responseDp = new DatagramPacket(buffer, buffer.length);
        int expectedSeq = 0;
        boolean notDone = true;
        FileOutputStream os = new FileOutputStream("received_" + filename);

        while(notDone) {
            socket.receive(responseDp);
            Packet packet = Packet.fromBytes(responseDp.getData(), responseDp.getLength());
            System.out.println("Received:");
            System.out.println(packet);

            if (packet.messageType == Protocol.ERROR){
                System.out.println("Error:" + new String(packet.payload));
                break;
            }

            if(packet.messageType == Protocol.DATA) {
                if (packet.connectionId != connectionId){
                    continue;
                }
                if (packet.sequenceNumber == expectedSeq) {
                    os.write(packet.payload);

                    Packet ack = new Packet(
                        connectionId,
                        packet.sequenceNumber,
                        Protocol.ACK,
                        (byte) 0,
                        new byte[0]
                    );

                    byte[] ackBytes = ack.toBytes();
                    DatagramPacket ackdp = new DatagramPacket(
                        ackBytes,
                        ackBytes.length,
                        responseDp.getAddress(),
                        responseDp.getPort()
                    );
                    socket.send(ackdp);

                    expectedSeq = 1 - expectedSeq;

                    if(packet.isFinal()) {
                        notDone = false;
                    }
                }
                    else {
                        int lastexp = 1 - expectedSeq;
                        Packet Ack = new Packet (
                            connectionId,
                            lastexp,
                            Protocol.ACK,
                            (byte) 0,
                            new byte[0]
                        );

                        byte[] ackbytes = Ack.toBytes();
                        DatagramPacket ackdp = new DatagramPacket(
                            ackbytes,
                            ackbytes.length,
                            responseDp.getAddress(),
                            responseDp.getPort()
                        );
                        socket.send(ackdp);
                    }
                }
               
            }
            os.close();
            socket.close();
            System.out.println("transer complete");

            
        }

    
    }

  

