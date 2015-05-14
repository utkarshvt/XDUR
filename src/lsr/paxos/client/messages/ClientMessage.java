package lsr.paxos.client.messages;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

import lsr.common.Request;

/**
 * Base class for all messages. Every message requires to know the view number
 * of the sender.
 * <p>
 * To implement new message, it is required to override the
 * <code>byteSize()</code> method and implement <code>write()</code> method. See
 * subclasses implementation for details how they should be implemented.
 */
public abstract class ClientMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public long clientId;
    public int sequenceNum;
    public long sentTime;

    /**
     * Creates message from specified view number and current time.
     * 
     * @param view - current view number
     */
    protected ClientMessage(long clientId, int sequenceNum) {
    	this.clientId = clientId;
    	this.sequenceNum = sequenceNum;
    	this.sentTime = System.currentTimeMillis();
    }

    /**
     * Creates new message from input stream with serialized message inside.
     * 
     * @param input - the input stream with serialized message
     * @throws IOException if I/O error occurs
     */
    protected ClientMessage(DataInputStream input) throws IOException {

    	clientId = input.readLong();
    	sequenceNum = input.readInt();
        sentTime = input.readLong();
    }

    /**
     * Returns the time when the message was sent.
     * 
     * @return the time when the message was sent in milliseconds.
     */
    public long getSentTime() {
        return sentTime;
    }

    /**
     * The size of the message after serialization in bytes.
     * 
     * @return the size of the message in bytes
     */
    public int byteSize() {
        return 1 + 8 + 4 + 8;
    }

    /**
     * Returns a message as byte array. The size of the array is equal to
     * <code>byteSize()</code>.
     * 
     * @return serialized message to byte array
     */
    public final byte[] toByteArray() {
        // Create with the byte array of the exact size,
        // to prevent internal resizes
        ByteBuffer bb = ByteBuffer.allocate(byteSize());
        bb.put((byte) getType().ordinal());
        bb.putLong(clientId);
        bb.putInt(sequenceNum);
        bb.putLong(sentTime);
        write(bb);

        assert bb.remaining() == 0 : "Wrong sizes. Limit=" + bb.limit() + ",capacity=" +
                                     bb.capacity() + ",position=" + bb.position();
        return bb.array();
    }

    /**
     * Returns the type of the message. This method is implemented by subclasses
     * and should return correct message type.
     * 
     * @return the type of the message
     */
    public abstract ClientMessageType getType();

    public String toString() {
        return "C: " + this.clientId + " S: " + this.sequenceNum;
    }
    
    /**
     * When serializing message to byte array, this function is called on the
     * message. Implementation of message-specific fields serialization must go
     * there.
     * 
     * @param bb - the byte buffer to serialize fields to
     */
    protected abstract void write(ByteBuffer bb);
    
    public abstract Request createRequest();
}
