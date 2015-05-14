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
public class NotLeaderMessage extends ClientMessage {
    private static final long serialVersionUID = 1L;
    private final int leaderId;

    /**
     * Creates new <code>Propose</code> message to propose specified instance ID
     * and value.
     * 
     * @param view - sender view number
     * @param instanceId - the ID of instance to propose
     * @param value - the value of the instance
     */
    public NotLeaderMessage(long clientId, int sequencenum, int leaderId) {
        super(clientId, sequencenum);
        this.leaderId = leaderId;
    }

    /**
     * Creates new <code>Propose</code> message from serialized input stream.
     * 
     * @param input - the input stream with serialized message
     * @throws IOException if I/O error occurs while deserializing
     */
    public NotLeaderMessage(DataInputStream input) throws IOException {
        super(input);
        this.leaderId = input.readInt();
    }

    /**
     * Returns the ID of proposed instance.
     * 
     * @return the ID of proposed instance
     */
    public int getLeaderId() {
        return leaderId;
    }

    public ClientMessageType getType() {
        return ClientMessageType.NotLeader;
    }

    public int byteSize() {
        return super.byteSize() + 4;
    }

    public String toString() {
        return "NotLeader(" + super.toString() + "), L: " + getLeaderId();
    }

    protected void write(ByteBuffer bb) {
        bb.putInt(leaderId);
    }
    
    public Request createRequest(){
    	return null;
    }
    
}
