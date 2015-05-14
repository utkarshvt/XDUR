package lsr.paxos.client.network;

import java.util.BitSet;

import lsr.paxos.client.messages.ClientMessage;

public class ClientMessageHandlerAdapter implements ClientMessageHandler {

    public void onMessageReceived(ClientMessage msg, int sender) {
    }

    public void onMessageSent(ClientMessage message, BitSet destinations) {
    }
}
