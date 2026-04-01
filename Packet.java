import java.nio.ByteBuffer;

public class Packet {
    public int connectionId;
    public int sequenceNumber;
    public byte messageType;
    public byte flags;
    public int payloadLength;
    public byte[] payload;

    public Packet(int connectionId, int sequenceNumber, byte messageType, byte flags, byte[] payload) {
        this.connectionId = connectionId;
        this.sequenceNumber = sequenceNumber;
        this.messageType = messageType;
        this.flags = flags;
        this.payloadLength = payload.length;
        this.payload = payload != null ? payload : new byte[0];
    }

    public boolean isFinal() {
        return (flags & Protocol.FLAG_FINAL) != 0;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 1 + 1 + 4 + payload.length);
        buffer.putInt(connectionId);
        buffer.putInt(sequenceNumber);
        buffer.put(messageType);
        buffer.put(flags);
        buffer.putInt(payloadLength);
        buffer.put(payload);
        return buffer.array();
    }

    public static Packet fromBytes(byte[] data, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);
        int connectionId = buffer.getInt();
        int sequenceNumber = buffer.getInt();
        byte messageType = buffer.get();
        byte flags = buffer.get();
        int payloadLength = buffer.getInt();

        byte[] payload = new byte[payloadLength];
        buffer.get(payload);

        return new Packet(connectionId, sequenceNumber, messageType, flags, payload);
    }
    @Override
    public String toString() {
        return "Packet{" +
                "connId=" + connectionId +
                ", seq=" + sequenceNumber +
                ", type=" + messageType +
                ", flags=" + flags +
                ", payloadLen=" + payload.length +
                ", payload=" + new String(payload) +
                '}';
    }
}