package stm.benchmark.tpcc;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import stm.benchmark.bank.BankMultiClient;
import stm.impl.PaxosSTM;
import stm.impl.SharedObjectRegistry;
import lsr.common.Configuration;
import lsr.paxos.ReplicationException;
import lsr.paxos.replica.Replica;

public class TpccServer {

	public static void main(String[] args) throws IOException,
			InterruptedException, ExecutionException, ReplicationException {
		if (args.length < 9 || args.length > 9) {
			usage();
			System.exit(1);
		}
		int localId = Integer.parseInt(args[0]);
		int warehouseCount = Integer.parseInt(args[1]); // Default is 1000
		int itemCount = Integer.parseInt(args[2]);
		int readThreadCount = Integer.parseInt(args[3]);
		int readPercentage = Integer.parseInt(args[4]);
		boolean tpccProfile = Integer.parseInt(args[5]) == 0 ? false : true;
		int clientCount = Integer.parseInt(args[6]);
		int requests = Integer.parseInt(args[7]);
		int MaxSpec = Integer.parseInt(args[8]);
		Configuration process = new Configuration();
		Tpcc tpcc = new Tpcc();
		
		SharedObjectRegistry sharedObjectRegistry = new SharedObjectRegistry(
				itemCount, MaxSpec);
		
		PaxosSTM stmInstance = new PaxosSTM(sharedObjectRegistry,
				readThreadCount, MaxSpec);
		tpcc.TpccInit(sharedObjectRegistry, stmInstance, warehouseCount,
				itemCount, MaxSpec);

		Replica replica = new Replica(process, localId, tpcc);
		tpcc.setReplica(replica);

		TpccMultiClient client = new TpccMultiClient(clientCount, requests,
				readPercentage, tpccProfile, tpcc);

		replica.start();

		tpcc.initRequests();

		stmInstance.init(tpcc, clientCount);

		client.run();
		System.out.println("Tpcc before in.read");
		System.in.read();
		System.out.println("Tpcc after in.read");

		System.in.read();
		System.exit(-1);
	}

	private static void usage() {
		System.out
				.println("Invalid arguments. Usage:\n"
						+ "   java lsr.paxos.Replica <replicaID> <Number Of Warehouses - 20> <Number Of Items - 5000> <Number of Read-Threads> "
						+ "<readPercentage> <tpccProfile> <clientCount> <#reqs>");
	}
}
