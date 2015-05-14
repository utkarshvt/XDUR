package lsr.paxos.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import lsr.common.Request;
import lsr.common.RequestId;
import lsr.paxos.NotLeaderException;
import lsr.paxos.Paxos;
import lsr.paxos.PaxosImpl;
import lsr.paxos.client.messages.RequestMessage;
import lsr.paxos.client.network.ClientNetwork;

public class NewClient {

	private ClientThread client;
	private Paxos paxos;
	private ClientNetwork clientNetwork;

	ConcurrentHashMap<RequestId, RequestId> pendingClientRequestMap = new ConcurrentHashMap<RequestId, RequestId>();

	BlockingQueue<Request> queue;

	SenderThread senderThread;

	class SenderThread extends Thread {

		@Override
		public void run() {
			while (true) {
				try {
					Request request = queue.take();
					RequestId requestId = request.getRequestId();

					RequestMessage msg = new RequestMessage(
							requestId.getClientId(), requestId.getSeqNumber(),
							request.getValue());

					clientNetwork.sendMessage(msg, paxos.getLeaderId());

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}

	class ClientThread extends Thread {

		private BlockingQueue<Request> sends;

		public ClientThread() {
			sends = new LinkedBlockingQueue<Request>();
		}

		@Override
		public void run() {

			while (true) {
				try {
					Request request = sends.take();

					if (paxos.isLeader()) {
						paxos.propose(request);
					} else {
						queue.add(request);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (NotLeaderException e) {
					e.printStackTrace();
				}
			}
		}

		public void execute(Request proposal) {
			this.sends.add(proposal);
		}

	}

	public NewClient(Paxos paxos) {
		this.paxos = paxos;
		this.clientNetwork = ((PaxosImpl) paxos).getClientNetwork();
		
		this.queue = new LinkedBlockingQueue<Request>();
	}

	public void queue(Request proposal) {
		client.execute(proposal);
	}

	public void init() {
		client = new ClientThread();
		client.start();


		senderThread = new SenderThread();

		// Start the sender thread before we start client threads
		senderThread.start();
	}

	public void setClientNetwork(ClientNetwork clientNetwork) {
		this.clientNetwork = clientNetwork;
	}

}
