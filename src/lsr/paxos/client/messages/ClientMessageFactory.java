package lsr.paxos.client.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;

import lsr.common.Config;

/**
 * This class is responsible for serializing and deserializing messages to /
 * from byte array or input stream. The message has to be serialized using
 * <code>serialize()</code> method to deserialized it correctly.
 */
public class ClientMessageFactory {

    /**
     * Creates a <code>Message</code> from serialized byte array.
     * 
     * @param message - serialized byte array with message content
     * @return deserialized message
     */
    public static ClientMessage readByteArray(byte[] message) {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(message));

        return create(input);
    }

    /**
     * Creates a <code>Message</code> from input stream.
     * 
     * @param input - the input stream with serialized message
     * @return deserialized message
     */
    public static ClientMessage create(DataInputStream input) {
        return createMine(input);
    }

    /**
     * Reads byte array and creates message from it. Byte array must have been
     * written by Message::toByteArray().
     * 
     * @param input - the input stream with serialized message inside
     * @return correct object from one of message subclasses
     * 
     * @throws IllegalArgumentException if a correct message could not be read
     *             from input
     */
    private static ClientMessage createMine(DataInputStream input) {
    	ClientMessageType type;
        ClientMessage message;

        try {
            type = ClientMessageType.values()[input.readUnsignedByte()];
            message = createMessage(type, input);
        } catch (EOFException e) {
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception deserializing message occured!", e);
        }

        return message;
    }

    /**
     * Serializes message to byte array.
     * 
     * @param message - the message to serialize
     * @return serialized message as byte array.
     */
    public static byte[] serialize(ClientMessage message) {
        byte[] data;
        data = message.toByteArray();
        return data;
    }

    /**
     * Creates new message of specified type from given stream.
     * 
     * @param type - the type of message to create
     * @param input - the stream with serialized message
     * @return deserialized message
     * 
     * @throws IOException if I/O error occurs
     */
    private static ClientMessage createMessage(ClientMessageType type, DataInputStream input)
            throws IOException {

    	ClientMessage message;
        switch (type) {
            case Request:
                message = new RequestMessage(input);
                break;
            case Response:
                message = new ResponseMessage(input);
                break;
            case NotLeader:
                message = new NotLeaderMessage(input);
                break;
            default:
                throw new IllegalArgumentException("Unknown message type given to deserialize!");
        }
        return message;
    }
}
