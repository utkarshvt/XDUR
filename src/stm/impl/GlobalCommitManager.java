package stm.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.esotericsoftware.kryo.Kryo;
import stm.benchmark.bank.Account;
import stm.benchmark.tpcc.TpccCustomer;
import stm.benchmark.tpcc.TpccDistrict;
import stm.benchmark.tpcc.TpccItem;
import stm.benchmark.tpcc.TpccOrder;
import stm.benchmark.tpcc.TpccOrderline;
import stm.benchmark.tpcc.TpccStock;
import stm.benchmark.tpcc.TpccWarehouse;
import stm.benchmark.vacation.Customer;
import stm.benchmark.vacation.Reservation;
import stm.benchmark.vacation.ReservationInfo;
import stm.impl.PaxosSTM;
import stm.transaction.AbstractObject;
import stm.transaction.TransactionContext;
import lsr.common.ClientRequest;
import lsr.common.Request;
import lsr.common.RequestId;
import lsr.paxos.Paxos;
import lsr.paxos.client.NewClient;

public class GlobalCommitManager {

	BlockingQueue<ClientRequest> rQueue = new LinkedBlockingQueue<ClientRequest>();
	BlockingQueue<Request> cQueue = new LinkedBlockingQueue<Request>();

	private Thread batcherThread;
	private Thread commitThread;

	private final PaxosSTM stmInstance;

	long failedCount = 0;

	private NewClient client;

	public GlobalCommitManager(PaxosSTM stmInstance, Paxos paxos, int clientCount) {
		this.stmInstance = stmInstance;
		this.client = new NewClient(paxos);
	}

	public void start() {
		this.batcherThread = new Thread(new Batcher(), "Batcher");
		batcherThread.start();

		this.commitThread = new Thread(new Committer(), "Committer");
		commitThread.start();

		this.client.init();
	}

	public void execute(ClientRequest task) {
		rQueue.offer(task);
	}

	public void notify(Request request) {
		cQueue.offer(request);
	}

	private class Batcher implements Runnable {

		private final Kryo kryo;

		public Batcher() {
			kryo = new Kryo();
			kryo.register(TransactionContext.class);
		}

		@Override
		public void run() {
			while (true) {
				ClientRequest request = null;
				try {
					request = rQueue.take();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				RequestId rId = request.getRequestId();
				TransactionContext ctx = stmInstance.getTransactionContext(rId);

				byte[] value = null;
				try {
					value = stmInstance.getSTMService().serializeTransactionContext(ctx);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Request proposal = new Request(rId, value);
				
				client.queue(proposal);

			}
		}
	}

	private class Committer implements Runnable {

		private final Kryo kryo;

		public Committer() {
			kryo = new Kryo();
			kryo.register(TransactionContext.class);
		}

		@Override
		public void run() {
			while (true) {
				Request request = null;
				try {
					request = cQueue.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


				try {
					stmInstance.onCommit(request.getRequestId(),
							stmInstance.getSTMService().deserializeTransactionContext(request.getValue()));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}
	}
}
