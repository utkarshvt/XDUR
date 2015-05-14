package stm.benchmark.vacation;

import java.io.IOException;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import lsr.common.ClientRequest;
import lsr.common.ProcessDescriptor;
import lsr.common.RequestId;
import lsr.paxos.ReplicationException;
import lsr.paxos.replica.IdGenerator;
import lsr.paxos.replica.SimpleIdGenerator;

public class VacationMultiClient {
	private Vector<ClientThread> clients = new Vector<ClientThread>();
    private final int clientCount;
    private final int requests;
    private final Manager manager;
    private final int percentReserve;

    public double latencyFinal;
    
	ConcurrentHashMap<Long, RequestId> pendingClientRequestMap = new ConcurrentHashMap<Long, RequestId>();
	
    private int writeCountFinal = 0;
    public double writeLatencyFinal = 0;

    class ClientThread extends Thread {
        private ArrayBlockingQueue<Integer> sends;
        private final Random random;

        public int writeCount = 0;
        public long writeLatency =0;
        
        private final long clientId;
        private int sequenceId = 0;        
        
        public ClientThread(long clientId) throws IOException {
            sends = new ArrayBlockingQueue<Integer>(128);
            this.random = new Random();
            
            this.clientId = clientId;
        }
        
        public void resetCounts() {
            writeCount = 0;
            writeLatency = 0;
        }
        
        @Override
        public void run() {
            try {
                Integer count;
                count = sends.take();
                
                Thread.sleep(20000);
                
                byte[] reqBytes;
            	ClientRequest request;
            	RequestId requestId;

                for (int i = 0; i < count; i++) {
                    long startTime = System.currentTimeMillis();
                    int percent = random.nextInt(100);
                    if(percent < percentReserve) {
                		reqBytes = manager.createRequest(percent, Vacation.ACTION_MAKE_RESERVATION);  
                    } else if ((percent & 1) == 1) {
                    	reqBytes = manager.createRequest(percent, Vacation.ACTION_DELETE_CUSTOMER);
                    } else {
                    	reqBytes = manager.createRequest(percent, Vacation.ACTION_UPDATE_TABLES);
                    }
                    request = new ClientRequest(nextRequestId(), reqBytes);
					requestId = request.getRequestId();
            		pendingClientRequestMap.put(requestId.getClientId(),
							requestId);
            		
            		synchronized(requestId) {
            			manager.executeWriteRequest(request, false);
            			requestId.wait(3000);
            		}
            		
                    writeCount++;
            		writeLatency += System.currentTimeMillis() - startTime;
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

    public VacationMultiClient(int clientCount, int requests, int replicaId, int readPercentage, Manager manager) {
    	this.manager = manager;
        this.clientCount = clientCount;
        this.requests = requests;
        this.percentReserve = readPercentage;
        
        manager.initClient(this);
    }

    public void run() throws IOException, ReplicationException, InterruptedException {

            execute(this.clientCount, this.requests);
    }

    private void execute(int clientCount, int requests)
            throws ReplicationException, IOException, InterruptedException {
    	
    	IdGenerator idGenerator = new SimpleIdGenerator(ProcessDescriptor.getInstance().localId, 
    			ProcessDescriptor.getInstance().numReplicas);

        for (int i = 0; i < clientCount; i++) {
            ClientThread client = new ClientThread(idGenerator.next());
            clients.add(client);
            client.start();
        }
        
        // prime the STM and OS-Paxos
        for (int i = 0; i < clientCount; i++) {
            clients.get(i).execute(requests);
        }

//        System.exit(1);

    }

    public void collectLatencyData() {
		for(int i=0; i<clients.size(); i++) {
			this.writeCountFinal += clients.get(i).writeCount;
			this.writeLatencyFinal += clients.get(i).writeLatency;
			clients.get(i).writeCount = 0;
			clients.get(i).writeLatency = 0;
		}
		this.writeLatencyFinal = this.writeLatencyFinal / this.writeCountFinal; 
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
}
