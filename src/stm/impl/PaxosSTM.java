package stm.impl;

import java.lang.Object;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.*;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import stm.transaction.AbstractObject;
import stm.transaction.TransactionContext;
import stm.impl.GlobalCommitManager;
import stm.impl.executors.ReadTransactionExecutor;
import stm.impl.executors.WriteTransactionExecutor;
import lsr.common.ClientRequest;
import lsr.common.Request;
import lsr.common.RequestId;
import lsr.service.STMService;

public class PaxosSTM {
	
	SharedObjectRegistry sharedObjectRegistry;
	/* Added for parallel implementation */
	private AtomicInteger TransactionId;				/* TransactionId of the current transaction*/
	private AtomicInteger lastXCommit;				/* Last speculative commit */
	private int MaxSpec;						/* Max number of speculative threads */
	private AtomicIntegerArray abort_array;					/* Abort array */
	
	private WriteTransactionExecutor writeExecutor;
	private ReadTransactionExecutor readExecutor;
	private WriteTransactionExecutor commitExecutor;
	private GlobalCommitManager globalCommitManager;
	
	private ConcurrentHashMap<RequestId, TransactionContext> requestIdContextMap;
	private ConcurrentHashMap<RequestId, Integer> requestSnapshotMap;
	
	private BlockingQueue<ClientRequest> XQueue = new LinkedBlockingQueue<ClientRequest>();
	private int BatchSize;		

	Kryo kryo = new Kryo();
	KryoFactory factory = new KryoFactory() {
	 public Kryo create () {
			Kryo ykryo = new Kryo();
			// configure kryo instance, customize settings
			return ykryo;
  		}
		};
	KryoPool pool = new KryoPool.Builder(factory).softReferences().build();
	private STMService service;

	public final String TX_READ_MODE = "r"; 
	public final String TX_READ_WRITE_MODE = "rw";
	public final String OBJECT_READ_MODE = "r";
	public final String OBJECT_WRITE_MODE = "w";
	
	public PaxosSTM(SharedObjectRegistry sharedObjectRegistry, int readThreadCount, int MaxSpec) {
		this.sharedObjectRegistry = sharedObjectRegistry;
		
		TransactionId = new AtomicInteger(1);
		lastXCommit = new AtomicInteger();
		this.MaxSpec = MaxSpec;
		abort_array = new AtomicIntegerArray(MaxSpec);
		commitExecutor = new WriteTransactionExecutor();
		writeExecutor = new WriteTransactionExecutor(this.MaxSpec);
		readExecutor = new ReadTransactionExecutor(readThreadCount);
		requestIdContextMap = new ConcurrentHashMap<RequestId, TransactionContext>();
		requestSnapshotMap = new ConcurrentHashMap<RequestId, Integer>();
		BatchSize = 0;
              	}
	
	public void init(STMService service, int clientCount) {
		this.service = service;
		globalCommitManager = new GlobalCommitManager(this, service.getReplica().getPaxos(), clientCount);
		globalCommitManager.start();
	}

	/**************************************************************************
	 * Create a transaction context for requestId and store it on 
	 * requestId-context Map.
	 * @param requestId
	 */
	public void createTransactionContext(RequestId requestId, int Tid) {
		if(!requestIdContextMap.containsKey(requestId)) {
			requestIdContextMap.put(requestId, new TransactionContext(0));
		}
	}
	
	public void removeTransactionContext(RequestId requestId) {
		if(requestIdContextMap.containsKey(requestId)) {
			requestIdContextMap.remove(requestId);
		}
	}
	
	
	public long shutDownExecutors() {
		long failCount;
		failCount = readExecutor.shutDownWriteExecutor();
		failCount += writeExecutor.shutDownWriteExecutor();
		return failCount;
	}
	
	public long shutDownWriteExecutors(){
		long failCount = 0;
		failCount += writeExecutor.shutDownWriteExecutor();
		return failCount;
	}
	

	/**************************************************************************
	 * Execute the read-only transaction with multiple-thread read-executor
	 * @param request
	 */
	public void executeReadRequest(Runnable request) {
		readExecutor.execute(request);
	}

	/**************************************************************************
	 * Execute the read-write transaction with single thread write-executor
	 * @param request
	 */
	public void executeWriteRequest(Runnable request) {
		writeExecutor.execute(request);
	}
	
	public void executeCommitRequest(Runnable request) {
		commitExecutor.execute(request);
	}
	
	public void onExecuteComplete(ClientRequest request) {
		globalCommitManager.execute(request);
	}
	
	public void onCommit(RequestId rId, TransactionContext ctx) {
		service.commitBatchOnDecision(rId, ctx);
	}
	
	/**************************************************************************
	 * This method retrieves an object from the Shared object registry. It also
	 * creates the context for requestId if not created already. Finally it 
	 * adds it to read and write set according to the object access mode defined
	 * by programmer.
	 * 
	 * @param objId
	 * @param txMode
	 * @param requestId
	 * @param objectMode
	 * @return Abstractobject
	 */
	public AbstractObject open(String objId, String txMode, RequestId requestId, 
			String objectAccessMode, boolean retry, int Tid) {
		// Create the context for the request Id if it was not created before
		createTransactionContext(requestId, Tid);
		if(txMode == TX_READ_MODE)
		{
			//System.out.println("Read requestId.clientId =  " + requestId.getClientId() + " requestId.seqNumber = " + requestId.getSeqNumber());
			//System.out.println("Transaction Id for this request = " + requestIdContextMap.get(requestId).getTransactionId());	
		}
		// Check if transaction is read-only or read-write type. For read-write add 
		// the object to writeset if object access mode says so
		if(txMode == TX_READ_MODE) { 
			if(!requestSnapshotMap.containsKey(requestId)) {				
				requestSnapshotMap.put(requestId, (Integer)sharedObjectRegistry.getSnapshot());
			}
			
			AbstractObject object = sharedObjectRegistry.getObject(objId, txMode, requestSnapshotMap.get(requestId), retry);

			// ?? Is this necessary?
//			requestIdContextMap.get(requestId).addObjectToReadSet(objId, object);
			return object;
		} else {
			// only needed to create object deep copy for write transaction
			
			// last parameter does not matter for write Tx
			AbstractObject object = kryo.copy(sharedObjectRegistry.getObject(objId, txMode, 0, retry));
			
			requestIdContextMap.get(requestId).addObjectToReadSet(objId, object);
			
			// for a rw transaction there may be two modes to access an object "r" or "w"
			if(objectAccessMode == OBJECT_WRITE_MODE) {
				requestIdContextMap.get(requestId).addObjectToWriteSet(objId, object);
			}

			// increment the object version right away -- just for matching validation for read/write objects
			object.incrementVersion();
			return object;
		}
	}
	
	/**************************************************************************
	 * This method retrieves an object from the Shared object registry. It also
	 * creates the context for requestId if not created already. Finally it 
	 * adds it to read and write set according to the object access mode defined
	 * by programmer.
	 * 
	 * @param objId
	 * @param txMode
	 * @param requestId
	 * @param objectMode
	 * @return Abstractobject
	 */
	public AbstractObject Xopen(String objId, String txMode, RequestId requestId, 
			String objectAccessMode, boolean retry, int pTid) {
		// Create the context for the request Id if it was not created before
		createXTransactionContext(requestId, pTid);
		//System.out.println("requestId.clientId =  " + requestId.getClientId() + " requestId.seqNumber = " + requestId.getSeqNumber() + 
		//						"   Transaction Id for this request = " + requestIdContextMap.get(requestId).getTransactionId() + 
		//						"   ObjId = " + objId + " txMode = " + txMode);
			
		/* Check if it is possible to add the transaction in the object_array, wait if an aborted transaction is already present */
		TransactionContext context = requestIdContextMap.get(requestId);
		int Tid = context.getTransactionId();
		if(CheckXaborted(Tid))
		{
			Xabort(Tid, requestId);
			return null;
		}

			

		// Check if transaction is read-only or read-write type. For read-write add 
		// the object to writeset if object access mode says so
		if(txMode == TX_READ_MODE) { 
			if(!requestSnapshotMap.containsKey(requestId)) {				
				requestSnapshotMap.put(requestId, (Integer)sharedObjectRegistry.getSnapshot());
			}
			
			AbstractObject object = sharedObjectRegistry.getObject(objId, txMode, requestSnapshotMap.get(requestId), retry);

			// ?? Is this necessary?
//			requestIdContextMap.get(requestId).addObjectToReadSet(objId, object);
			return object;
		} else {
			// only needed to create object deep copy for write transaction
			
			// last parameter does not matter for write Tx
			AbstractObject sobject = sharedObjectRegistry.getXObject(objId, txMode, 0, this, Tid, retry);
			
			if( (sobject == null) || (CheckXaborted(Tid)))
                        {                        
                                Xabort(Tid, requestId);
                                return null;
                        }
			Kryo xkryo = pool.borrow();
			AbstractObject object = xkryo.copy(sobject);
			pool.release(xkryo);
			requestIdContextMap.get(requestId).addObjectToReadSet(objId, object);
			
			// for a rw transaction there may be two modes to access an object "r" or "w"
			if(objectAccessMode == OBJECT_WRITE_MODE) {
				requestIdContextMap.get(requestId).addObjectToWriteSet(objId, object);
			}

			// increment the object version right away -- just for matching validation for read/write objects
			object.incrementVersion();
			return object;
		}
	}
	/**************************************************************************
	 * Create a X transaction context for requestId and store it on 
	 * requestId-context Map.
	 * @param requestId
	 */
	public void createXTransactionContext(RequestId requestId, int Tid) {
		if(!requestIdContextMap.containsKey(requestId)) {
			//int tid = Tid.getAndIncrement();
			
			requestIdContextMap.put(requestId, new TransactionContext(Tid));
			ClearAbortArray(Tid);
				
		}
	}
	
	public void storeResultToContext(RequestId requestId, byte[] result) {
		requestIdContextMap.get(requestId).setResult(result);
	}
	
	public byte[] getResultFromContext(RequestId requestId) {
		return requestIdContextMap.get(requestId).getResult();
	}
	
	public void updateUnCommittedSharedCopy(RequestId requestId) {
		// Update the non-committed but completed object copy with the 
		// Write-set of this transaction - Request ID
		TransactionContext context = requestIdContextMap.get(requestId);
		Map<String, AbstractObject> writeset = context.getWriteSet();
		for(Map.Entry<String, AbstractObject> entry: writeset.entrySet()) {
			String objId = entry.getKey();
			AbstractObject object = entry.getValue();
			sharedObjectRegistry.updateCompletedObject(objId, object);
		}
	}
	
	public boolean validateReadset(TransactionContext context) {
		assert context != null;
		if(context == null) {
			return false;
		}

		Map<String, AbstractObject> readset = context.getReadSet();
		for(Map.Entry<String, AbstractObject> entry: readset.entrySet()) {
			String objId = entry.getKey();
			AbstractObject object = entry.getValue();
		
			System.out.println(" Validate: " + objId + " " + sharedObjectRegistry.getLatestCommittedObject(objId).hashCode() + " ");				
			if(sharedObjectRegistry.getLatestCommittedObject(objId).getVersion() != (object.getVersion()-1)) {
				System.out.print(" Validate: " + objId + " " + sharedObjectRegistry.getLatestCommittedObject(objId).hashCode() + " ");
				System.out.println("Failed for comparing version " + objId + " " + 
										sharedObjectRegistry.getLatestCommittedObject(objId).getVersion() + " != " + 
											(object.getVersion()-1));
				
				sharedObjectRegistry.updateCompletedObject(objId, null);
				return false;
			}
		}
		return true;
	}

	public boolean updateSharedObject(TransactionContext context) {
		boolean commit = true;
		
		Map<String, AbstractObject> writeset = context.getWriteSet();
		
		int timeStamp = sharedObjectRegistry.getNextSnapshot();
		// Update all shared objects with shadowcopy object values and versions 
		// Acquire lock over all objects - for multithreaded STM

		for(Map.Entry<String, AbstractObject> entry: writeset.entrySet()) {
			// update all objects
			sharedObjectRegistry.updateObject(entry.getKey(), entry.getValue(), timeStamp);
		}
		// release lock over all objects - for multithreaded STM

		return commit;
	}
	
	public TransactionContext getTransactionContext(RequestId requestId) {
		return requestIdContextMap.get(requestId);
	}

	public void notifyCommitManager(Request request) {
		globalCommitManager.notify(request);		
	}

	public STMService getSTMService() {
		return service;
	}
	
	
	/* Functions for parallel implementation */
	
	public int getTransactionId()
	{
		return this.TransactionId.get();
	}

	/* Abort the later readers in the readset */
	public void XabortReaders( int [] readers, int Tid)
	{
		for(int i = Tid ; i < MaxSpec; i++)
		{
			if(readers[i] > 0)
				SetAbortArray(readers[i]);
		}
	}
	/* XAbort a transaction by writing in the abort array */
	public void Xabort(int Tid, RequestId Id)
	{
		/* Clear the abort_array */
		int index = (Tid - 1)% MaxSpec;
		this.abort_array.set(index,0);
		
		//System.out.println("Tranaction Aborted " + Tid);
		TransactionContext context = requestIdContextMap.get(Id);
		/* Clear the readset and writeset of the Transaction Context */
	  	Map<String, AbstractObject> readset = context.getReadSet();
                Map<String, AbstractObject> writeset = context.getWriteSet();
		if(!readset.isEmpty())
		{	
			for(Map.Entry<String, AbstractObject> entry: readset.entrySet()) 
			{
				String objId = entry.getKey();
                        	//AbstractObject object = entry.getValue();
			
			
				context.readsetremove(objId);
				sharedObjectRegistry.clearReader(objId,Tid);
				if(sharedObjectRegistry.getOwner(objId) == Tid)
				{
					sharedObjectRegistry.compareAndSetOwner(objId,Tid,0);
				}
			}
		}
		if(!writeset.isEmpty())
		{
			for(Map.Entry<String, AbstractObject> entry: writeset.entrySet()) 
			{
				String objId = entry.getKey();
                        	//AbstractObject object = entry.getValue();
		       		// sharedObjectRegistry.updateCompletedObject(objId, null);
		       		context.writesetremove(objId);	
		       		sharedObjectRegistry.clearReader(objId,Tid);
				if(sharedObjectRegistry.getOwner(objId) == Tid)
                        	{
                                	sharedObjectRegistry.compareAndSetOwner(objId,Tid,0);
                        	}

			}
		}

                
		
		return;	
	}

	public boolean XCommitTransaction( RequestId Id)
	{
		/* Update the completed copy */
		// Update the non-committed but completed object copy with the 
		// Write-set of this transaction - Request ID
		
		TransactionContext context = requestIdContextMap.get(Id);
		int Tid = context.getTransactionId();
		int index = (Tid - 1) % MaxSpec;
		int min_Tid = 1;
		int lastXCommitted = 0;
		if(CheckXaborted(Tid) == true)
		{
			Xabort(Tid, Id);
			return false ;
		}	
		
		lastXCommitted = lastXCommit.get();
		//System.out.println("XCommiting tx = " + Tid + "Waiting for comp rule" + "last X was " + lastXCommitted);
		/* Comp rule, waiting for the last transaction */
		while(lastXCommitted != (Tid - 1))
		{
			/*if((Tid == min_Tid) && (lastXCommitted >= Tid))
			{
				System.out.println("Last batch did not have enough writers, need to reset lastXCommitted, last committed = " + lastXCommitted); 
				lastXCommit.compareAndSet(lastXCommitted, 0);
				break;
			}*/
			
			lastXCommitted = lastXCommit.get();
		}
		
		/* Checking for being aborted, just in case ... */
		if(CheckXaborted(Tid) == true)
                {
                       // System.out.println("XAboted tx = " + Tid + "Waiting for comp rule");
			Xabort(Tid, Id);
                        return false ;
                }



		/* Update the completedobject copy */
		Map<String, AbstractObject> writeset = context.getWriteSet();
		for(Map.Entry<String, AbstractObject> entry: writeset.entrySet()) 
		{
			String objId = entry.getKey();
			//System.out.println("ObjId committed = " + objId + " TransactionId = " + Tid);
			AbstractObject object = entry.getValue();
			sharedObjectRegistry.updateCompletedObject(objId, object);
			sharedObjectRegistry.clearReader(objId, Tid);
			sharedObjectRegistry.clearOwner(objId);		
		}
		//System.out.println("XCommiting tx = " + Tid + "lastxCommit = " + lastXCommit.get());
		/* Update lastXCommited value */
		if(lastXCommit.compareAndSet(Tid - 1, Tid))
                {
			/* Reset if reaches MaxSpec */
			/*if(Tid == this.MaxSpec)
			{
				if(lastXCommit.compareAndSet(Tid, 0))
				{
					return true;	
				}
				else
				{
					System.out.println("Tx greater than MaxSpec found, impossible");
					return false;
				}
			}*/
			return true;
		}
		else
		{
			System.out.println("Wrong Xcommit failed, Tid = " + Tid);
			return false;
		}
			
	}
	public boolean CheckXaborted(int Tid)
	{
		int index = (Tid - 1) % MaxSpec;
		if( index < 0 )
		{
			System.out.println("Index less than 1)");

		}
		if (abort_array.get(index)  == 1)
			return true;
		else
			return false;
	}

	public void SetAbortArray(int Tid)
	{
		int index = (Tid - 1) % MaxSpec;
                if ( index < 0)
		{
			System.out.println("Index less than 1)");
		}
		/* Wait for an aborted transaction to leave */
		while(!CheckXaborted(Tid))
                {
			if(abort_array.compareAndSet(index,0,1))
				return;	
		}
	}

	 public void ClearAbortArray(int Tid)
        {
                int index = (Tid - 1) % MaxSpec;
                if ( index < 0)
                {
                        System.out.println("Index less than 1)");
                }
                /* Wait for an aborted transaction to leave */
                while(CheckXaborted(Tid))
                {
                        if(abort_array.compareAndSet(index,1,0))
                                return;
                }
        }

	public void xqueue(ClientRequest Request)
	{
		XQueue.add(Request);
	}

	public void Xqueueclear()
	{
		XQueue.clear();
	}
	public int XqueuedrainTo(ArrayList<ClientRequest> array, int num)
	{
		int ret = XQueue.drainTo(array,num);
		return ret;
	}

	public int getMaxSpec()
	{
		return this.MaxSpec;
	}

	public void resetLastXcommit()
	{
		lastXCommit.set(0);
	}

	public int getBatchSize()
	{
		return this.BatchSize;
	}

}	
