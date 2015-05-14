package lsr.paxos.client.messages;

/**
 * Represents message type.
 */
public enum ClientMessageType {
    
    Request,
    Response,
    NotLeader,
    // Special markers used by the network implementation to raise callbacks
    // There are no classes with this messages types
    ANY, // any message
    SENT
}
