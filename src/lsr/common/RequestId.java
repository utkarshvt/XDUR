package lsr.common;

import java.io.Serializable;

/**
 * Represents the unique id of request. To ensure uniqueness, the id contains
 * the id of client and the request sequence number. Every client should have
 * assigned unique client id, so that unique requests id can be created. Clients
 * gives consecutive sequence numbers to every sent request. The sequence number
 * starts with 0.
 */
public class RequestId implements Serializable, Comparable<RequestId> {
    private static final long serialVersionUID = 1L;

    /** Represents the no-op request. */
    public static final RequestId NOP = new RequestId(-1, -1);

    private final long clientId;
    private final int seqNumber;

    /**
     * Creates new <code>RequestId</code> instance.
     * 
     * @param clientId - the id of client
     * @param seqNumber - the request sequence number
     */
    public RequestId(long clientId, int seqNumber) {
        this.clientId = clientId;
        this.seqNumber = seqNumber;
    }

    /**
     * Returns the id of client.
     * 
     * @return the id of client
     */
    public Long getClientId() {
        return clientId;
    }

    /**
     * Returns the request sequence number.
     * 
     * @return the request sequence number
     */
    public int getSeqNumber() {
        return seqNumber;
    }

    public int compareTo(RequestId requestId) {
       /* if (clientId != requestId.clientId) {
            	System.out.println("Orig client = " + requestId.clientId + " Curr client = " + clientId + "Seqnumber = " + requestId.getSeqNumber());
		int currhash = this.hashCode() ;
		int reqhash = requestId.hashCode();
		long reqdiv = requestId.clientId >>> 32;
		long currdiv = clientId >>> 32;
		long currxor = clientId ^ (currdiv + 1);
		long reqxor = requestId.clientId ^ (reqdiv + 1); 
		System.out.println("Current hash = " + currhash + " Request hash = " + reqhash + " current div = " + reqdiv + " Requet div = " + currdiv  + " Cuurxor = " + currxor + " reqxor = " + reqxor);
		throw new IllegalArgumentException("Cannot compare requests from diffrents clients.");
        }*/
        return seqNumber - requestId.seqNumber;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        RequestId requestId = (RequestId) obj;
        return clientId == requestId.clientId && seqNumber == requestId.seqNumber;
    }

    public int hashCode() {
        return (int) (clientId ^ ((clientId >>> 32) + 0)) ^ seqNumber;
    }

    public boolean isNop() {
        return clientId == -1 && seqNumber == -1;
    }

    public String toString() {
        return isNop() ? "nop" : clientId + ":" + seqNumber;
    }
}
