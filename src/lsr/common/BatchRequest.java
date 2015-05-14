package lsr.common;

import java.nio.ByteBuffer;
import java.util.Collection;

public class BatchRequest extends Request {
    private static final long serialVersionUID = 1L;
    
    private final Collection<byte[]> value;
    private final int valueLength;
    private final RequestId requestId;
    
    public BatchRequest(RequestId requestId, Collection<byte[]> value)
    {
        super(requestId, new byte[0]);
        this.requestId = requestId;
        this.value = value;
        int valueLength = 0;
        for (byte[] tab : value)
            valueLength += tab.length;
        this.valueLength = valueLength + 4 * value.size();
    }

    @Override
    public int byteSize() {
        return 8 + 4 + 4 + valueLength;
    }

    /**
     * Writes a message to specified byte buffer. The number of elements
     * remaining in specified buffer should be greater or equal than
     * <code>byteSize()</code>.
     * 
     * @param bb - the byte buffer to write message to
     */
    @Override
    public void writeTo(ByteBuffer bb) {
        bb.putLong(requestId.getClientId());
        bb.putInt(requestId.getSeqNumber());
        bb.putInt(valueLength);
        for (byte[] tab : value) {
            bb.putInt(tab.length);
            bb.put(tab);
        }
    }
}
