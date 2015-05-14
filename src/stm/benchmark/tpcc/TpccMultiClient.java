package stm.benchmark.tpcc;

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

public class TpccMultiClient {
	private Vector<ClientThread> clients = new Vector<ClientThread>();
	private final int clientCount;
	private final int requests;
	private final Tpcc tpcc;
	private final int readPercentage;
	private final boolean tpccProfile;

	ConcurrentHashMap<Long, RequestId> pendingClientRequestMap = new ConcurrentHashMap<Long, RequestId>();

	private int readCountFinal = 0;
	private int writeCountFinal = 0;
	public double readLatencyFinal = 0;
	public double writeLatencyFinal = 0;

	private int replicaId;

	class ClientThread extends Thread {
		private final long clientId;
		private int sequenceId = 0;

		private ArrayBlockingQueue<Integer> sends;
		private final Random random;

		public int readCount = 0;
		public int writeCount = 0;
		public long readLatency = 0;
		public long writeLatency = 0;

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

		@Override
		public void run() {
			boolean readOnly = true;
			boolean readWrite = !readOnly;
			try {
				Integer count;
				
                Thread.sleep(20000);
				
				count = sends.take();

				for (int i = 0; i < count; i++) {
					byte[] reqBytes;
					ClientRequest request;
					RequestId requestId;
					long start = System.currentTimeMillis();
					int percent = random.nextInt(100);
					if (tpccProfile) {
						if (percent < 8) {
							reqBytes = tpcc.createRequest(readOnly,
									tpccProfile, percent);
							request = new ClientRequest(nextRequestId(),
									reqBytes);
							requestId = request.getRequestId();
							pendingClientRequestMap.put(
									requestId.getClientId(), requestId);

							readCount++;

							synchronized (requestId) {
								tpcc.executeReadRequest(request);
								requestId.wait();
							}

							this.readLatency += System.currentTimeMillis()
									- start;
						} else {
							reqBytes = tpcc.createRequest(readWrite,
									tpccProfile, percent);
							request = new ClientRequest(nextRequestId(),
									reqBytes);
							requestId = request.getRequestId();
							pendingClientRequestMap.put(
									requestId.getClientId(), requestId);

							writeCount++;

							synchronized (requestId) {
								tpcc.executeRequest(request, false);
								requestId.wait();
							}

							this.writeLatency += System.currentTimeMillis()
									- start;
						}
					} else {
						if (percent < readPercentage) {
							reqBytes = tpcc.createRequest(readOnly,
									tpccProfile, percent);
							request = new ClientRequest(nextRequestId(),
									reqBytes);
							requestId = request.getRequestId();
							pendingClientRequestMap.put(
									requestId.getClientId(), requestId);

							readCount++;

							synchronized (requestId) {
								tpcc.executeReadRequest(request);
								requestId.wait();
							}
							this.readLatency += System.currentTimeMillis()
									- start;
						} else {
							reqBytes = tpcc.createRequest(readWrite,
									tpccProfile, percent);
							request = new ClientRequest(nextRequestId(),
									reqBytes);
							requestId = request.getRequestId();
							pendingClientRequestMap.put(
									requestId.getClientId(), requestId);

							writeCount++;

							synchronized (requestId) {
								tpcc.executeRequest(request, false);
								requestId.wait();
							}
							this.writeLatency += System.currentTimeMillis()
									- start;
						}
					}
					// if(i%100 == 0)
					// System.out.println("read-Count = " + readCount + " " +
					// writeCount + " " + readLatency + " " + writeLatency);

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

	public TpccMultiClient(int clientCount, int requests, int readPercentage,
			boolean tpccProfile, Tpcc tpcc) {
		this.tpcc = tpcc;
		this.clientCount = clientCount;
		this.requests = requests;
		this.readPercentage = readPercentage;
		this.tpccProfile = tpccProfile;
		tpcc.initClient(this);
	}

	public void run() throws IOException, ReplicationException,
			InterruptedException {

		execute(this.clientCount, this.requests);
	}

	private void execute(int clientCount, int requests)
			throws ReplicationException, IOException, InterruptedException {

		int dummyCount = 0;

		IdGenerator idGenerator = new SimpleIdGenerator(
				ProcessDescriptor.getInstance().localId,
				ProcessDescriptor.getInstance().numReplicas);
		for (int i = 0; i < clientCount; i++) {
			ClientThread client = new ClientThread(idGenerator.next());
			clients.add(client);
			client.start();
		}

		// startTime = System.currentTimeMillis();

		// prime the STM and OS-Paxos
		for (int i = 0; i < clientCount; i++) {
			clients.get(i).execute(requests);
		}

		// System.out.println("Read-Latency Write-Latency Latency ");
		//
		// while(dummyCount < 10) {
		// Thread.sleep(10000);
		// readCountFinal = 0;
		// writeCountFinal = 0;
		// readLatencyFinal = 0;
		// writeLatencyFinal = 0;
		// for (int i = 0; i < clientCount; i++) {
		// readCountFinal += clients.get(i).readCount;
		// writeCountFinal += clients.get(i).writeCount;
		// readLatencyFinal += clients.get(i).readLatency;
		// writeLatencyFinal += clients.get(i).writeLatency;
		// clients.get(i).resetCounts();
		// }
		// //System.out.println("read-Count = " + clients.get(0).readCount + " "
		// + clients.get(0).writeCount + " " + clients.get(0).readLatency + " "
		// + clients.get(0).writeLatency);
		//
		// System.out.format("%4.2f %4.2f %4.2f\n", 1f*readLatencyFinal/
		// readCountFinal, 1f*writeLatencyFinal/writeCountFinal,
		// 1f*(readLatencyFinal+writeLatencyFinal)/
		// (readCountFinal+writeCountFinal));
		// dummyCount++;
		// }

		// for (int i = 0; i < clientCount; i++) {
		// clients.get(i).join();
		// readCountFinal += clients.get(i).getReadCount();
		// writeCountFinal += clients.get(i).getWriteCount();
		// latencyFinal += clients.get(i).latency;
		// }
		// long duration = System.currentTimeMillis() - startTime;
		//
		// System.err.println(String.format("Finished-Duration= %d Read-Throughput= %4.2f "
		// +
		// "Write-Throughput= %4.2f Latency= %4.2f\n", duration,
		// 1f * readCountFinal*1000 / duration, 1f * writeCountFinal*1000 /
		// duration,
		// latencyFinal/clientCount));
		// //(1f * duration * clientCount)/(readCountFinal + writeCountFinal)));
		//
		// System.exit(0);
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
//		this.latencyFinal = (double) (this.readLatencyFinal + this.writeLatencyFinal) / (this.readCountFinal + this.writeCountFinal);
		this.readLatencyFinal = this.readLatencyFinal / this.readCountFinal;
		this.writeLatencyFinal = this.writeLatencyFinal / this.writeCountFinal; 
	}
	
	public double getReadLatency() {
		double latency = this.readLatencyFinal;
		this.readLatencyFinal = 0;
		this.readCountFinal = 0;
		return latency;
	}
	
//	public double getFinalLatency() {
//		double latency = this.latencyFinal;
//		this.latencyFinal = 0;
//		return latency;		
//	}
	
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
			// System.out.print(".");
		}
	}

}
