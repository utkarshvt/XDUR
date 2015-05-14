package lsr.paxos.client.network;

import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import lsr.paxos.client.messages.ClientMessage;
import lsr.paxos.client.messages.ClientMessageType;


/**
 * This class provides methods to communicate with other processes (replicas).
 * It allows to send the message to one or many replicas, and provides listeners
 * called every time new message is received or sent.
 * 
 */
public abstract class ClientNetwork {

    // // // Public interface - send, send to all and add / remove listeners //
    // // //

    /**
     * Sends the message to process with specified id.
     * 
     * @param message the message to send
     * @param destination the id of replica to send message to
     */
    public abstract void sendMessage(ClientMessage message, int destination);

    /**
     * Sends the message to process with specified id.
     * 
     * @param message the message to send
     * @param destination bit set with marked replica id's to send message to
     */
    public abstract void sendMessage(ClientMessage message, BitSet destination);

    /**
     * Sends the message to all processes.
     * 
     * @param message the message to send
     */
    public abstract void sendToAll(ClientMessage message);

    public abstract void start();

    /**
     * Adds a new message listener for a certain type of message or all messages
     * ( see {@link MessageType}). The listener cannot be added twice for the
     * same message - this causes a {@link RuntimeException}.
     */
    final public static void addMessageListener(ClientMessageType mType, ClientMessageHandler handler) {
        CopyOnWriteArrayList<ClientMessageHandler> handlers = msgListeners.get(mType);
        boolean wasAdded = handlers.addIfAbsent(handler);
        if (!wasAdded) {
            throw new RuntimeException("Handler already registered");
        }
    }

    /**
     * Removes a previously registered listener. Throws {@link RuntimeException}
     * if the listener is not on list.
     */
    final public static void removeMessageListener(ClientMessageType mType, ClientMessageHandler handler) {
        CopyOnWriteArrayList<ClientMessageHandler> handlers = msgListeners.get(mType);
        boolean wasPresent = handlers.remove(handler);
        if (!wasPresent) {
            throw new RuntimeException("Handler not registered");
        }
    }

    public static void removeAllMessageListeners() {
        msgListeners.clear();
        for (ClientMessageType ms : ClientMessageType.values()) {
            msgListeners.put(ms, new CopyOnWriteArrayList<ClientMessageHandler>());
        }
    }

    // // // Protected part - for implementing the subclasses // // //

    /**
     * For each message type, keeps a list of it's listeners.
     * 
     * The list is shared between networks
     */
    protected static final Map<ClientMessageType, CopyOnWriteArrayList<ClientMessageHandler>> msgListeners;
    static {
        msgListeners = Collections.synchronizedMap(
                new EnumMap<ClientMessageType, CopyOnWriteArrayList<ClientMessageHandler>>(ClientMessageType.class));
        for (ClientMessageType ms : ClientMessageType.values()) {
            msgListeners.put(ms, new CopyOnWriteArrayList<ClientMessageHandler>());
        }
    }

    /**
     * Notifies all active network listeners that new message was received.
     */
    protected final void fireReceiveMessage(ClientMessage message, int sender) {
        assert message.getType() != ClientMessageType.SENT && message.getType() != ClientMessageType.ANY;
        boolean handled = broadcastToListeners(message.getType(), message, sender);
        handled |= broadcastToListeners(ClientMessageType.ANY, message, sender);
        if (!handled) {
            logger.warning("Unhandled message: " + message);
        }
    }

    /**
     * Notifies all active network listeners that message was sent.
     */
    protected final void fireSentMessage(ClientMessage msg, BitSet dest) {
        List<ClientMessageHandler> handlers = msgListeners.get(ClientMessageType.SENT);
        for (ClientMessageHandler listener : handlers) {
            listener.onMessageSent(msg, dest);
        }
    }

    /**
     * Informs all listeners waiting for the message type about the message.
     * Parameter type is needed in order to support MessageType.ANY value.
     * Returns if there was at least one listener.
     */
    private final boolean broadcastToListeners(ClientMessageType type, ClientMessage msg, int sender) {
        List<ClientMessageHandler> handlers = msgListeners.get(type);
        boolean handled = false;
        for (ClientMessageHandler listener : handlers) {
            listener.onMessageReceived(msg, sender);
            handled = true;
        }
        return handled;
    }

    private final static Logger logger = Logger.getLogger(ClientNetwork.class.getCanonicalName());
}
