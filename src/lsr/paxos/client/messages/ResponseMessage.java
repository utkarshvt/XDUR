package lsr.paxos.client.messages;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import lsr.common.Request;

/**
 * Represents the <code>Propose</code> message sent by leader to vote on next
 * consensus instance. As every message it contains the view number of sender
 * process and additionally the id of new consensus instance and its value as
 * byte array.
 */
public class ResponseMessage extends ClientMessage {
    private static final long serialVersionUID = 1L;
    private final byte[] value;

    /**
     * Creates new <code>Propose</code> message to propose specified instance ID
     * and value.
     * 
     * @param view - sender view number
     * @param instanceId - the ID of instance to propose
     * @param value - the value of the instance
     */
    public ResponseMessage(long ClientId, int sequencenum, byte[] value) {
        super(ClientId, sequencenum);
        assert value != null;
        this.value = value;
    }

    /**
     * Creates new <code>Propose</code> message from serialized input stream.
     * 
     * @param input - the input stream with serialized message
     * @throws IOException if I/O error occurs while deserializing
     */
    public ResponseMessage(DataInputStream input) throws IOException {
        super(input);

        value = new byte[input.readInt()];
        input.readFully(value);
    }

    /**
     * Returns value of proposed instance.
     * 
     * @return value of proposed instance
     */
    public byte[] getValue() {
        return value;
    }

    public ClientMessageType getType() {
        return ClientMessageType.Response;
    }

    public int byteSize() {
        return super.byteSize() + 4 + value.length;
    }

    public String toString() {
        return "Response(" + super.toString() + ")";
    }

    protected void write(ByteBuffer bb) {
        
        bb.putInt(value.length);
        bb.put(value);
    }
    
    public Request createRequest(){
    	return null;
    }
}
