//package stm.benchmark.vacation;
//
//import java.io.IOException;
//import java.util.ArrayDeque;
//import java.util.Deque;
//import java.util.Random;
//import java.util.concurrent.ExecutionException;
//
//import lsr.common.Configuration;
//import lsr.common.Request;
//import lsr.common.RequestId;
//import lsr.paxos.replica.Replica;
//import stm.impl.STMImpl;
//import stm.impl.SharedObjectRegistry;
//
//public class LocalVacationServer {
//
//	public static void main(String[] args) throws IOException, InterruptedException,
//    ExecutionException {
//		if (args.length < 8 || args.length > 8) {
//		    usage();
//		    System.exit(1);
//		}
//		int replicaId = Integer.parseInt(args[0]);
//		int numQueriesPerTransactions = Integer.parseInt(args[1]);
//		int numRelations = Integer.parseInt(args[2]);
//		int percentOfQueries = Integer.parseInt(args[3]);
//		int percentOfUser = Integer.parseInt(args[4]);
//		int clientCount = Integer.parseInt(args[5]);
//		int requests = Integer.parseInt(args[6]);
//		int numReadThreads = Integer.parseInt(args[7]);
//        boolean tpccProfice = false ;
//
//        System.out.println("#Relations = " + numRelations + " Clients = " + clientCount + 
//        		" Query% = " + percentOfQueries + " Queries/Tx = " + numQueriesPerTransactions + " User% = " + percentOfUser);
//        
//		Configuration process = new Configuration();
//		Manager manager = new Manager(numQueriesPerTransactions, percentOfQueries, percentOfUser);
//		VacationClient client = new VacationClient(clientCount, requests, replicaId, numRelations, percentOfUser, manager);
//		
//		SharedObjectRegistry sharedObjectRegistry = new SharedObjectRegistry(numRelations);
//	    STMImpl stmInstance = new STMImpl(sharedObjectRegistry, manager, numReadThreads, client);
//	    sharedObjectRegistry.init(stmInstance);
//	    manager.init(numRelations, sharedObjectRegistry, stmInstance);
//		//Replica replica = new Replica(process, replicaId, manager, stmInstance, client, tpccProfice);
//
//		//stmInstance.setReplica(replica);
//	    
//	    int sequenceNum = 0;
//	    //LocalRandom random = new LocalRandom();
//	    Random random = new Random();
//        
//	    for (int i = 0; i < requests; i++) {
//        	Deque<Request> dequeue = new ArrayDeque<Request>(128);
//        	for(int count=0; count <clientCount; count++) {
//            	byte[] bytes;
//            	//int percent = random.posrandom_generate();
//            	int percent = random.nextInt(100);
//            	//System.out.println(percent + "- " + percentOfUser);
//            	if(percent < percentOfUser) {
//            		bytes = manager.createRequest(percent, Vacation.ACTION_MAKE_RESERVATION);
//            		
//        			Request request = new Request(new RequestId(count, ++sequenceNum), bytes);
//        			dequeue.add(request);
//        			        			
//            	} else if ((percent & 1) == 1) {
//            		bytes = manager.createRequest(percent, Vacation.ACTION_DELETE_CUSTOMER);
//            		
//        			Request request = new Request(new RequestId(count, ++sequenceNum), bytes);
//        			dequeue.add(request);
//        			
//            	} else {
//            		bytes = manager.createRequest(percent, Vacation.ACTION_UPDATE_TABLES);
//            		
//        			Request request = new Request(new RequestId(count, ++sequenceNum), bytes);
//        			dequeue.add(request);
//        			
//            	}        		
//        	}
//        	stmInstance.enqueueCommittedBatch(i, dequeue);
//        	Thread.sleep(5);
//        }
//		
//		//replica.start();
//		System.in.read();
//		System.exit(-1);
//	}
//	
//	private static void usage() {
//		System.out.println("Invalid arguments. Usage:\n"
//		                   + "   java lsr.paxos.Replica <replicaID> " +
//		                   "<Number Of Relations> <clientCount> <requestsPerClient>" +
//		                   "<read%> <#Read-threads> ");
//	}
//}
