package stm.benchmark.bank;

import java.io.IOException;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import lsr.common.ClientRequest;
import lsr.common.ProcessDescriptor;
import lsr.common.Request;
import lsr.common.RequestId;
import lsr.paxos.ReplicationException;
import lsr.paxos.replica.IdGenerator;
import lsr.paxos.replica.SimpleIdGenerator;

/**
 * This class provides multiple threads to create clients which gets connected
 * to Replicas hosting the Server side application. Clients issue read/read-write 
 * requests to replicas and wait for the reply from replica. If the reply is not 
 * received in the defined time, it assumes the replica to be down and connects to 
 * other replica.
 *  
 * @author sachin
 *
 */
public class BankMultiClient {
	private Vector<ClientThread> clients = new Vector<ClientThread>();
    private final int clientCount;
    private final int requests;
    private final Bank bank;
    private final int readPercentage;
    
	ConcurrentHashMap<Long, RequestId> pendingClientRequestMap = new ConcurrentHashMap<Long, RequestId>();
    
    private int readCountFinal = 0;
    private int writeCountFinal = 0;
    public double readLatencyFinal = 0;
    public double writeLatencyFinal = 0;
    
    class ClientThread extends Thread {
        private final long clientId;
        private int sequenceId = 0;
        
        private ArrayBlockingQueue<Integer> sends;
        private final Random random;
        
        public int readCount = 0;
        public int writeCount = 0;
        public long readLatency =0; 
        public long writeLatency =0;
        
        public ClientThread(long clientId) throws IOException {
        	this.clientId = clientId;
        	
            sends = new ArrayBlockingQueue<Integer>(128);
            this.random = new Random();
        }

        public void resetCounts() {
            readCount = 0;
            writeCount = 0;
            readLatency = 0;
            writeLatency = 0;
        }
        
        public int getReadCount() {
        	return readCount;
        }

        public int getWriteCount() {
        	return writeCount;
        }

        @Override
        public void run() {
        	boolean readOnly = true;
        	boolean readWrite = !readOnly; 
        	boolean timeSampled = false;
            try {
                Integer count;

                Thread.sleep(20000);

                // Start the main benchmark execution now
                count = sends.take();
		for (int i = 0; i < count; i++) {               	
                	long start = System.currentTimeMillis();
                	byte[] reqBytes;
                	ClientRequest request;
                	RequestId requestId;
                	if(random.nextInt(100) < readPercentage) {
                		reqBytes = bank.createRequest(readOnly);
                		request = new ClientRequest(nextRequestId(), reqBytes);
    					requestId = request.getRequestId();
                		pendingClientRequestMap.put(requestId.getClientId(),
    							requestId);
                		
            			readCount++;
            			
            			synchronized(requestId) {
            				bank.executeReadRequest(request);
            				//requestId.wait(300);
            			}
                        
                        this.readLatency += System.currentTimeMillis() - start;
                	} else {
                		reqBytes = bank.createRequest(readWrite);
                		request = new ClientRequest(nextRequestId(), reqBytes);  
                		requestId = request.getRequestId();
                		pendingClientRequestMap.put(requestId.getClientId(),
    							requestId);
                		
                		writeCount++;
            			synchronized(requestId) {
            				bank.executeRequest(request, false);
//            				System.out.print(":");
            				//requestId.wait(300);
//            				System.out.print("+");
            			}                        
                        this.writeLatency += System.currentTimeMillis() - start;
                	}

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void execute(int count) throws InterruptedException {
            sends.put(count);
        }
        
        private RequestId nextRequestId() {
            return new RequestId(clientId, ++sequenceId);
        }

    }


    public BankMultiClient(int clientCount, int requests, int numberObjects, 
    		int readPercentage, Bank bank) {
    	this.bank = bank;
        this.clientCount = clientCount;
        this.requests = requests;
        this.readPercentage = readPercentage;
        
        bank.initClient(numberObjects, this);
    }

    public void run() throws IOException, ReplicationException, InterruptedException {

            execute(this.clientCount, this.requests);
    }
    
    private void execute(int clientCount, int requests)
            throws ReplicationException, IOException, InterruptedException {
    	
    	IdGenerator idGenerator = new SimpleIdGenerator(ProcessDescriptor.getInstance().localId, 
    			ProcessDescriptor.getInstance().numReplicas);
        for (int i = clients.size(); i < clientCount; i++) {
            ClientThread client = new ClientThread(idGenerator.next());
            clients.add(client);
            client.start();
        }
        
        // prime the STM and OS-Paxos
        for (int i = 0; i < clientCount; i++) {
            clients.get(i).execute(requests);
        }
    }
    
    public void collectLatencyData() {
		for(int i=0; i<clients.size(); i++) {
			this.readCountFinal += clients.get(i).readCount;
			this.writeCountFinal += clients.get(i).writeCount;
			this.readLatencyFinal += clients.get(i).readLatency;
			this.writeLatencyFinal += clients.get(i).writeLatency;
			clients.get(i).readCount = 0;
			clients.get(i).writeCount = 0;
			clients.get(i).readLatency = 0;
			clients.get(i).writeLatency = 0;
		}
		this.readLatencyFinal = this.readLatencyFinal / this.readCountFinal;
		this.writeLatencyFinal = this.writeLatencyFinal / this.writeCountFinal; 
	}
	
	public double getReadLatency() {
		double latency = this.readLatencyFinal;
		this.readLatencyFinal = 0;
		this.readCountFinal = 0;
		return latency;
	}
	
	public double getWriteLatency() {
		double latency = this.writeLatencyFinal;
		this.writeLatencyFinal = 0;
		this.writeCountFinal = 0;
		return latency;	
	}
    
    public void replyToClient(RequestId rId) {
		RequestId requestId = pendingClientRequestMap.get(rId.getClientId());
		if (requestId != null) {
			synchronized (requestId) {
				requestId.notify();
			}
		}
	}
	public int getReadCount() {
		return this.readCountFinal;
	}

	public int getWriteCount() {
		return this.writeCountFinal;
	}



}
