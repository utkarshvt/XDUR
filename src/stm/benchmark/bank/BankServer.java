package stm.benchmark.bank;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import stm.impl.PaxosSTM;
import stm.impl.SharedObjectRegistry;
import lsr.common.Configuration;
import lsr.paxos.ReplicationException;
import lsr.paxos.replica.Replica;

/**
 * Server side application, which starts the Paxos Instance and STM instance. It
 * receives requests from connected clients and sends reply for executed
 * requests.
 * 
 * @author sachin
 * 
 */
public class BankServer {

	public static void main(String[] args) throws IOException,
			InterruptedException, ExecutionException, ReplicationException {
		if (args.length < 7 || args.length > 7) {
			usage();
			System.exit(1);
		}
		int localId = Integer.parseInt(args[0]);
		int objectsCount = Integer.parseInt(args[1]);
		int readThreadCount = Integer.parseInt(args[2]);
		int readPercentage = Integer.parseInt(args[3]);
		int clientCount = Integer.parseInt(args[4]);
		int requests = Integer.parseInt(args[5]);
		int MaxSpec = Integer.parseInt(args[6]);
	
		Configuration process = new Configuration();
		Bank bank = new Bank();
		
		SharedObjectRegistry sharedObjectRegistry = new SharedObjectRegistry(
				objectsCount, MaxSpec);

		PaxosSTM stmInstance = new PaxosSTM(sharedObjectRegistry,
				readThreadCount, MaxSpec);
		bank.init(objectsCount, sharedObjectRegistry, stmInstance, MaxSpec);		

		Replica replica = new Replica(process, localId, bank);
		bank.setReplica(replica);
		
		BankMultiClient client = new BankMultiClient(clientCount, requests,
				objectsCount, readPercentage, bank);
				
		replica.start();
		
		bank.initRequests();
		
		stmInstance.init(bank, clientCount);
		
		client.run();

		System.in.read();
	}

	private static void usage() {
		System.out.println("Invalid arguments. Usage:\n"
				+ "   java lsr.paxos.Replica <replicaID> "
				+ "<Number Of Object> <Number of Read-Threads> <read%> <clients> <requests>"
				+ "<% of Cross Partion access");
	}
}
