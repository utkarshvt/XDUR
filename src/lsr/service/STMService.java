package lsr.service;

import java.io.IOException;

import lsr.common.ClientRequest;
import lsr.common.Request;
import lsr.common.RequestId;
import lsr.paxos.replica.Replica;
import stm.transaction.TransactionContext;

//import lsr.paxos.replica.SnapshotListener;

/**
 * Abstract class which can be used to simplify creating new services. It adds
 * implementation for handling snapshot listeners.
 */
public abstract class STMService implements Service {
	
    /**
     * Informs the service that the recovery process has been finished, i.e.
     * that the service is at least at the state later than by crashing.
     * 
     * Please notice, for some crash-recovery approaches this can mean that the
     * service is a lot further than by crash.
     * 
     * For many applications this has no real meaning.
     */
	
	public final byte READ_ONLY_TX = 0;
	public final byte READ_WRITE_TX = 1;
	
    public void recoveryFinished() {
    }
    
    public abstract Replica getReplica();	
    
    public abstract void commitBatchOnDecision(RequestId rId, TransactionContext txContext) ;	
    
    // This method needs to be implemented in all the benchmark classes which extend STMService
    public abstract void executeReadRequest(ClientRequest cRequest);

	public abstract void notifyCommitManager(Request request);
    
	public abstract byte[] serializeTransactionContext(TransactionContext ctx) throws IOException;

	public abstract TransactionContext deserializeTransactionContext(byte[] bytes) throws IOException;
    
}
