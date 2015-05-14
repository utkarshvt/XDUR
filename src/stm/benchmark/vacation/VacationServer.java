package stm.benchmark.vacation;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import stm.impl.PaxosSTM;
import stm.impl.SharedObjectRegistry;
import lsr.common.Configuration;
import lsr.paxos.ReplicationException;
import lsr.paxos.replica.Replica;

public class VacationServer {
	
	public static void main(String[] args) throws IOException, InterruptedException,
    ExecutionException, ReplicationException {
		if (args.length < 9 || args.length > 9) {
		    usage();
		    System.exit(1);
		}
		int replicaId = Integer.parseInt(args[0]);
		int numQueriesPerTransactions = Integer.parseInt(args[1]);
		int numRelations = Integer.parseInt(args[2]);
		int percentOfQueries = Integer.parseInt(args[3]);
		int percentOfUser = Integer.parseInt(args[4]);
		int numReadThreads = Integer.parseInt(args[5]);
		int clientCount = Integer.parseInt(args[6]);
		int requests = Integer.parseInt(args[7]);
		int MaxSpec = Integer.parseInt(args[8]);
        
		Configuration process = new Configuration();
		
		Manager manager = new Manager(numQueriesPerTransactions, percentOfQueries, percentOfUser, MaxSpec);

		SharedObjectRegistry sharedObjectRegistry = new SharedObjectRegistry(numRelations, MaxSpec);
				
		PaxosSTM stmInstance = new PaxosSTM(sharedObjectRegistry, numReadThreads, MaxSpec); //manager, client);
	    //sharedObjectRegistry.init(stmInstance);
	    manager.init(sharedObjectRegistry, stmInstance, numRelations);
		Replica replica = new Replica(process, replicaId, manager); //, stmInstance, client, tpccProfice);

		manager.setReplica(replica);
		
		VacationMultiClient client = new VacationMultiClient(clientCount, requests, numRelations, percentOfUser, manager);
		
		replica.start();
		
		manager.initRequests();
		
		stmInstance.init(manager, clientCount);
		
		client.run();
		
		System.in.read();
		System.exit(-1);
	}
	
	private static void usage() {
		System.out.println("Invalid arguments. Usage:\n"
		                   + "   java lsr.paxos.Replica <replicaID> <numQueries/Tx 10>" +
		                   "<Number Of Relations> <%Queries 90> <%Reserve 80> <#Read-threads> <clientCount> <#reqs>");
	}
}
