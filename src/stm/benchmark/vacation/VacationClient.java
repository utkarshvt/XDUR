//package stm.benchmark.vacation;
//
//
//import java.io.IOException;
//import java.util.Random;
//import java.util.Vector;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ConcurrentHashMap;
//
//import stm.benchmark.ProxyClient;
//
//import lsr.common.ProcessDescriptor;
//import lsr.common.Request;
//import lsr.common.RequestId;
//import lsr.paxos.NotLeaderException;
//import lsr.paxos.Paxos;
//import lsr.paxos.ReplicationException;
//import lsr.paxos.Proposer.ProposerState;
//import lsr.paxos.client.Client;
//import lsr.paxos.clientNetwork.ClientNetwork;
//import lsr.paxos.clientNetwork.clientMessages.RequestMessage;
//import lsr.paxos.replica.IdGenerator;
//import lsr.paxos.replica.SimpleIdGenerator;
//import lsr.paxos.replica.TimeBasedIdGenerator;
//
//public class VacationClient extends ProxyClient {
//	private Vector<ClientThread> clients = new Vector<ClientThread>();
//    private final byte[] request;
//    private final int clientCount;
//    private final int requestsPerClient;
//    private final Manager manager;
//    private final int percentReserve;
//    
//    private long startTime;
//    private int lastRequestCount;
//    
//    private int makeResCountFinal;
//    private int delCusCountFinal;
//    private int updateTableCountFinal;
//    public double makeResLatencyFinal;
//    public double delCusLatencyFinal;
//    public double updateTableLatencyFinal;
//    public double latencyFinal;
//    
//    private int replicaId;
//    private Paxos paxos;
//    private ClientNetwork clientNetwork;
//    
//    ConcurrentHashMap<Long, RequestId> pendingClientRequestMap = new ConcurrentHashMap<Long, RequestId>(); 
//    
//    BlockingQueue<Request> queue; 
//    
//    SenderThread senderThread;
//
//    class SenderThread extends Thread {
//    	 
//    	 @Override
//         public void run() {
//    		 while(true) {
//    			 try {
//					Request request = queue.take();
//					RequestId requestId = request.getRequestId();
//					
//					RequestMessage msg = new RequestMessage(requestId.getClientId(), requestId.getSeqNumber(), request.getValue());
//					//System.out.print("=" + paxos.getLeaderId() + "=");
//					clientNetwork.sendMessage(msg, paxos.getLeaderId());
//					
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//    			 
//    		 }
//    	 }
//    }
//    
//    class ClientThread extends Thread {
//
//        //final Client readClient;
//    	long clientId = -1;
//    	int sequenceNum = 0;
//    	
//        private ArrayBlockingQueue<Integer> sends;
//        private final Random random;
//        
//        public volatile int makeResCount = 0;
//        public volatile int delCusCount = 0;
//        public volatile int updateTableCount = 0;
//        private long startTime =0;
//        public volatile long makeResLatency =0; 
//        public volatile long delCusLatency =0;
//        public volatile long updateTableLatency =0;
//        
//        private volatile boolean startMonitoring = false;
//        
//        public ClientThread(long clientId) throws IOException {
//        	
//        	this.clientId = clientId;
//            sends = new ArrayBlockingQueue<Integer>(128);
//            this.random = new Random();
//            //this.random.random_seed((int) clientId);
//        }
//
//        public void setMonitoringFlag()  {
//        	startMonitoring = true;
//        }
//        
////        public int getReadCount() {
////        	return readCount;
////        }
////
////        public int getWriteCount() {
////        	return writeCount;
////        }
//
//        @Override
//        public void run() {
//        	boolean readOnly = true;
//        	boolean readWrite = !readOnly; 
//        	boolean timeSampled = false;
//            try {
//
//                Integer count;
//                
//                Thread.sleep(20000);
//                // Start the main benchmark execution now
//                count = sends.take();
//
//                for (int i = 0; i < count; i++) {
//                	byte[] bytes;
//                	int percent = random.nextInt(100); //posrandom_generate();
//                	if(percent < percentReserve) {
//                		bytes = manager.createRequest(percent, Vacation.ACTION_MAKE_RESERVATION);
//                		makeResCount++;
//                		
//            			Request request = new Request(new RequestId(clientId, ++sequenceNum), bytes);
//            			RequestId requestId = request.getRequestId();
//            			pendingClientRequestMap.put(requestId.getClientId(), requestId);
//            			
//            			startTime = System.currentTimeMillis();
//            			if(paxos.isLeader()) {
//	            			synchronized(requestId) {
//	            				paxos.propose(request);
//	            				//System.out.print(":");
//	            				requestId.wait(10000);
//	            			}
//            			} else {
//                			synchronized(requestId) {
//                				queue.add(request);
//                				//System.out.print(".");
//                				requestId.wait(10000);
//                			}
//            			}
//            			makeResLatency += System.currentTimeMillis() - startTime;
//            			
//                	} else if ((percent & 1) == 1) {
//                		bytes = manager.createRequest(percent, Vacation.ACTION_DELETE_CUSTOMER);
//                		delCusCount++;
//                		
//            			Request request = new Request(new RequestId(clientId, ++sequenceNum), bytes);
//            			RequestId requestId = request.getRequestId();
//            			pendingClientRequestMap.put(requestId.getClientId(), requestId);
//            			
//            			startTime = System.currentTimeMillis();
//            			if(paxos.isLeader()) {
//	            			synchronized(requestId) {
//	            				paxos.propose(request);
//	            				//System.out.print(":");
//	            				requestId.wait(10000);
//	            			}
//            			} else {
//                			synchronized(requestId) {
//                				queue.add(request);
//                				//System.out.print(".");
//                				requestId.wait(10000);
//                			}
//            			}
//            			delCusLatency += System.currentTimeMillis() - startTime;
//                	} else {
//                		bytes = manager.createRequest(percent, Vacation.ACTION_UPDATE_TABLES);
//                		updateTableCount++;
//                		
//            			Request request = new Request(new RequestId(clientId, ++sequenceNum), bytes);
//            			RequestId requestId = request.getRequestId();
//            			pendingClientRequestMap.put(requestId.getClientId(), requestId);
//            			
//            			startTime = System.currentTimeMillis();
//            			if(paxos.isLeader()) {
//	            			synchronized(requestId) {
//	            				paxos.propose(request);
//	            				//System.out.print(":");
//	            				requestId.wait(10000);
//	            			}
//            			} else {
//                			synchronized(requestId) {
//                				queue.add(request);
//                				//System.out.print(".");
//                				requestId.wait(10000);
//                			}
//            			}
//            			updateTableLatency += System.currentTimeMillis() - startTime;
//                	}
//
//                }
//                //timeTaken = System.currentTimeMillis() - timeTaken;
//                //latency = 1f * timeTaken / (readCount + writeCount);
//
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (NotLeaderException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        }
//
//        public void execute(int count) throws InterruptedException {
//            sends.put(count);
//        }
//
//    }
//
//    public VacationClient(int clientCount, int requestsPerClient, int replicaId, int numberObjects, int percentReserve, Manager manager) {
//    	this.manager = manager;
//        request = new byte[manager.DEFAULT_LENGTH];
//        this.clientCount = clientCount;
//        this.requestsPerClient = requestsPerClient;
//        this.replicaId = replicaId;
//        this.percentReserve = percentReserve;
//        //manager.initClient(numberObjects);
//        this.queue = new ArrayBlockingQueue<Request>(clientCount*2);
//        this.senderThread = new SenderThread();
//    }
//
//    public void run() throws IOException, ReplicationException, InterruptedException {
//
//            execute(this.clientCount, this.requestsPerClient);
//    }
//    
//    public void execute(int clientCount, int requests)
//            throws ReplicationException, IOException, InterruptedException {
//
//    	String generatorName = ProcessDescriptor.getInstance().clientIDGenerator;
//    	IdGenerator idGenerator = new SimpleIdGenerator(ProcessDescriptor.getInstance().localId, 
//    			ProcessDescriptor.getInstance().numReplicas);
//            
//        for (int i = clients.size(); i < clientCount; i++) {
//            ClientThread client = new ClientThread(idGenerator.next());
//            client.start();
//            clients.add(client);
//        }
//        
//        startTime = System.currentTimeMillis();
//
//        // Start the sender thread before we start client threads
//        senderThread.start();
//        
//        // prime the STM and OS-Paxos
//        for (int i = 0; i < clientCount; i++) {
//            clients.get(i).execute(requests);
//        }
//    }
//
//	@Override
//	public void setPaxosInstance(Paxos paxos) {
//		// TODO Auto-generated method stub
//		this.paxos = paxos;
//	}
//	
//	@Override
//	public void setClientNetwork(ClientNetwork clientNetwork) {
//		this.clientNetwork = clientNetwork;
//	}
//	
//	public void collectLatencyData() {
//		for(int i=0; i<clients.size(); i++) {
//			this.makeResCountFinal += clients.get(i).makeResCount;
//			this.delCusCountFinal += clients.get(i).delCusCount;
//			this.updateTableCountFinal += clients.get(i).updateTableCount;
//			
//			this.makeResLatencyFinal += clients.get(i).makeResLatency;
//			this.delCusLatencyFinal += clients.get(i).delCusLatency;
//			this.updateTableLatencyFinal += clients.get(i).updateTableLatency;
//			
//			clients.get(i).makeResCount = 0;
//			clients.get(i).delCusCount = 0;
//			clients.get(i).updateTableCount = 0;
//			clients.get(i).makeResLatency = 0;
//			clients.get(i).delCusLatency = 0;
//			clients.get(i).updateTableLatency = 0;
//		}
//		this.latencyFinal = (double) (this.makeResLatencyFinal + this.delCusLatencyFinal + this.updateTableLatencyFinal) / 
//				(this.makeResCountFinal + this.delCusCountFinal + this.updateTableCountFinal);
//		this.makeResLatencyFinal = this.makeResLatencyFinal / this.makeResCountFinal;
//		this.delCusLatencyFinal = this.delCusLatencyFinal / this.delCusCountFinal;
//		this.updateTableLatencyFinal = this.updateTableLatencyFinal / this.updateTableCountFinal;
//	}
//	
//	public double getMakeResLatency() {
//		double latency = this.makeResLatencyFinal;
//		this.makeResLatencyFinal = 0;
//		this.makeResCountFinal = 0;
//		return latency;
//	}
//	
//	public double getFinalLatency() {
//		double latency = this.latencyFinal;
//		this.latencyFinal = 0;
//		return latency;		
//	}
//	
//	public double getDelCusLatency() {
//		double latency = this.delCusLatencyFinal;
//		this.delCusLatencyFinal = 0;
//		this.delCusCountFinal = 0;
//		return latency;	
//	}
//
//	public double getUpdatetableLatency() {
//		double latency = this.updateTableLatencyFinal;
//		this.updateTableLatencyFinal = 0;
//		this.updateTableCountFinal = 0;
//		return latency;	
//	}
//
//	@Override
//	public void replyToClient(Request request) {
//		
//		RequestId requestId = pendingClientRequestMap.get(request.getRequestId().getClientId());
//		if(requestId != null) {
//			synchronized(requestId) {
//				requestId.notify();
//				//System.out.print(",");
//			}
//		}
//	}
//
//	@Override
//	public double getReadLatency() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public double getWriteLatency() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//}
