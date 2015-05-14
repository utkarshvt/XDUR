package stm.benchmark.vacation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import lsr.common.ClientRequest;
import lsr.common.ProcessDescriptor;
import lsr.common.Request;
import lsr.common.RequestId;
import lsr.common.SingleThreadDispatcher;
import lsr.paxos.replica.Replica;
import lsr.paxos.replica.SnapshotListener;
import lsr.service.STMService;
import stm.benchmark.bank.BankMultiClient;
import stm.benchmark.tpcc.TpccItem;
import stm.impl.PaxosSTM;
import stm.impl.SharedObjectRegistry;
import stm.transaction.AbstractObject;
import stm.transaction.TransactionContext;

public class Manager extends STMService {

	SharedObjectRegistry sharedObjectRegistry;
	PaxosSTM stmInstance;
	Replica replica;
	private SingleThreadDispatcher vacationSTMDispatcher;
	VacationMultiClient client;

	int numRelations;
	int numQueriesPerTransactions;
	int percentOfQueries;
	int percentOfUser;

	public int MaxSpec;
	public final int DEFAULT_LENGTH = 5;

	AtomicInteger lockThread = new AtomicInteger(0);
	MonitorThread monitorTh = new MonitorThread();

	volatile long start = 0;
	volatile long end = 0;

	private long lastWriteCount = 0;
	private long lastAbortCount = 0;

	volatile long writeCount = 0;
	volatile long makeResCount = 0;
	volatile long delCusCount = 0;
	volatile long updateTableCount = 0;

	private final Map<RequestId, byte[]> requestIdValueMap = new HashMap<RequestId, byte[]>();

	private volatile long committedCount = 0;
	private volatile long abortedCount = 0;

	private int localId;
	private int min;
	private int max;
	private int numReplicas;
	private int accessibleObjects;

	/*************************************************************************
	 * This class is only for taking the readings from the experiment. The
	 * sampling thread is triggered when read/write count reaches a particular
	 * limit, it goes to sleep for 20 seconds and then it samples the reading.
	 * 
	 * @author sachin
	 * 
	 ************************************************************************/
	class MonitorThread extends Thread {

		public void run() {
			int count = 0;
			long localWriteCount = 0;
			long localAbortCount = 0;

			// System.out.println("MakeRes-thr" + "\t" + "DelCus-thr" +"\t" +
			// "UpdateTab-thr" + "\t" + "Time-taken" +
			System.out.println("Write-thr" + "\t" + "Write Latency" + "\t"
					+ "Abort" + "\t" + "Time");

			while (count < 7) {

				start = System.currentTimeMillis();
				// Sample time is 30 seconds
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				localWriteCount = committedCount;
				localAbortCount = abortedCount;

				end = System.currentTimeMillis();
				client.collectLatencyData();

				System.out.format("%5d  %4.2f %5d %6d\n",
						((localWriteCount - lastWriteCount) * 1000)
								/ (end - start), client.getWriteLatency(),
						(localAbortCount - lastAbortCount), (end - start));

				lastWriteCount = localWriteCount;
				lastAbortCount = localAbortCount;

				count++;

			}
			System.exit(1);
		}
	}

	public void incrementWriteCount(long timeTaken) {
		writeCount++;
		// System.out.print(".");
		if (start == 0) {
			if (lockThread.compareAndSet(0, 1)) {
				start = System.currentTimeMillis();
				monitorTh.start();
			}
		}
	}

	public Manager(int numQueriesPerTransactions, int percentOfQueries,
			int percentOfUser, int MaxSpec) {
		this.MaxSpec = MaxSpec;
		this.numQueriesPerTransactions = numQueriesPerTransactions;
		this.percentOfQueries = percentOfQueries;
		this.percentOfUser = percentOfUser;
		vacationSTMDispatcher = new SingleThreadDispatcher("VacationSTM");
		// this.vacationSTMDispatcher.start();
	}

	public void init(SharedObjectRegistry sharedObjectRegistry,
			PaxosSTM stminstance, int numRelations) {
		this.sharedObjectRegistry = sharedObjectRegistry;
		this.stmInstance = stminstance;
		this.numRelations = numRelations;

		int i;
		int t;
		// LocalRandom random = new LocalRandom();
		// random.random_alloc();
		Random random = new Random();

		int numRelation = numRelations;
		int ids[] = new int[numRelation];
		for (i = 0; i < numRelation; i++) {
			ids[i] = i + 1;
		}

		for (t = 0; t < 4; t++) {
			/* Shuffle ids */
//			for (i = 0; i < numRelation; i++) {
//				int x = random.nextInt(numRelation); // .posrandom_generate() %
//														// numRelation;
//				int y = random.nextInt(numRelation);
//				; // random.posrandom_generate() % numRelation;
//				int tmp = ids[x];
//				ids[x] = ids[y];
//				ids[y] = tmp;
//			}

			/* Populate table */
			for (i = 0; i < numRelation; i++) {
				int id = ids[i];
				// int num = ((random.posrandom_generate() % 5) + 1) * 100;
				// int price = ((random.posrandom_generate() % 5) * 10) + 50;
				int num = ((random.nextInt(5)) + 1) * 100;
				int price = ((random.nextInt(5)) * 10) + 50;
				if (t == 0) {
					final String reservationId = Vacation.CAR_PREFIX + id;
					Reservation reservation = new Reservation(reservationId,
							id, num, price);
					this.sharedObjectRegistry.registerObjects(reservationId,
							reservation, MaxSpec);
					// addCar(id, num, price);
				} else if (t == 1) {
					final String reservationId = Vacation.FLIGHT_PREFIX + id;
					Reservation reservation = new Reservation(reservationId,
							id, num, price);
					this.sharedObjectRegistry.registerObjects(reservationId,
							reservation, MaxSpec);
					// addFlight(id, num, price);
				} else if (t == 2) {
					final String reservationId = Vacation.ROOM_PREFIX + id;
					Reservation reservation = new Reservation(reservationId,
							id, num, price);
					this.sharedObjectRegistry.registerObjects(reservationId,
							reservation, MaxSpec);
					// addRoom(id, num, price);
				} else if (t == 3) {
					final String reservationId = Vacation.CUSTOMER_PREFIX + id;
					Customer customer = new Customer(reservationId, id);
					this.sharedObjectRegistry.registerObjects(reservationId,
							customer, MaxSpec);
					// addCustomer(id);
				}
				// assert(status);
			}

		}

		// start the monitor thread
		monitorTh.start();
	}

	public void initRequests() {
		this.localId = ProcessDescriptor.getInstance().localId;
		this.numReplicas = ProcessDescriptor.getInstance().numReplicas;
	}

	public void initClient(VacationMultiClient client) {
		this.client = client;
	}

	/**
	 * Pass reference to replice for sending a reply to client after read
	 * request is executed or write request is committed.
	 */
	public void setReplica(Replica replica) {
		this.replica = replica;
	}

	boolean addReservation(String reservationId, // TransactionalRBTree<Integer,
													// Reservation> table,
			int id, int num, int price, RequestId requestId, boolean retry) {
		Reservation reservation;

		reservation = (Reservation) stmInstance.open(reservationId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());// table.find(id);
		if (reservation == null) {
			/* Create new reservation */
			if (num < 1 || price < 0) {
				return false;
			}
			reservation = new Reservation(reservationId, id, num, price);
			// assert(reservationPtr != NULL);
			// stmInstance.write(reservation, reservationId, requestId); // do
			// the registration at commit time
			// this.sharedObjectRegistry.registerObjects(reservationId,
			// reservation); // table.insert(id, reservation);
		} else {
			if (reservation.isNull == true)
				reservation.isNull = false;
			/* Update existing reservation */
			if (!reservation.reservation_addToTotal(num)) {
				return false;
			}
			if (reservation.numTotal == 0) {
				reservation.isNull = true;
				// stmInstance.write(reservation, reservationId, requestId);
				// table.remove(id);
			} else {
				reservation.reservation_updatePrice(price);
				// stmInstance.write(reservation, reservationId, requestId);
			}
		}

		return true;
	}

	/*
	 * ==========================================================================
	 * === manager_addCar -- Add cars to a city -- Adding to an existing car
	 * overwrite the price if 'price' >= 0 -- Returns TRUE on success, else
	 * FALSE
	 * ====================================================================
	 * =========
	 */
	final boolean addCar(int carId, int numCars, int price,
			RequestId requestId, boolean retry) {
		final String reservationStringId = Vacation.CAR_PREFIX + carId;
		return addReservation(reservationStringId, carId, numCars, price,
				requestId, retry);
	}

	/*
	 * ==========================================================================
	 * === manager_deleteCar -- Delete cars from a city -- Decreases available
	 * car count (those not allocated to a customer) -- Fails if would make
	 * available car count negative -- If decresed to 0, deletes entire entry --
	 * Returns TRUE on success, else FALSE
	 * ======================================
	 * =======================================
	 */
	final boolean deleteCar(int carId, int numCar, RequestId requestId,
			boolean retry) {
		final String reservationStringId = Vacation.CAR_PREFIX + carId;
		/* -1 keeps old price */
		return addReservation(reservationStringId, carId, -numCar, -1,
				requestId, retry);
	}

	/*
	 * ==========================================================================
	 * === manager_addRoom -- Add rooms to a city -- Adding to an existing room
	 * overwrite the price if 'price' >= 0 -- Returns TRUE on success, else
	 * FALSE
	 * ====================================================================
	 * =========
	 */
	final boolean addRoom(int roomId, int numRoom, int price,
			RequestId requestId, boolean retry) {
		final String reservationStringId = Vacation.ROOM_PREFIX + roomId;
		return addReservation(reservationStringId, roomId, numRoom, price,
				requestId, retry);
	}

	/*
	 * ==========================================================================
	 * === manager_deleteRoom -- Delete rooms from a city -- Decreases available
	 * room count (those not allocated to a customer) -- Fails if would make
	 * available room count negative -- If decresed to 0, deletes entire entry
	 * -- Returns TRUE on success, else FALSE
	 * ====================================
	 * =========================================
	 */
	final boolean deleteRoom(int roomId, int numRoom, RequestId requestId,
			boolean retry) {
		final String reservationStringId = Vacation.ROOM_PREFIX + roomId;
		/* -1 keeps old price */
		return addReservation(reservationStringId, roomId, -numRoom, -1,
				requestId, retry);
	}

	/*
	 * ==========================================================================
	 * === manager_addFlight -- Add seats to a flight -- Adding to an existing
	 * flight overwrite the price if 'price' >= 0 -- Returns TRUE on success,
	 * FALSE on failure
	 * ==========================================================
	 * ===================
	 */
	final boolean addFlight(int flightId, int numSeat, int price,
			RequestId requestId, boolean retry) {
		final String reservationStringId = Vacation.FLIGHT_PREFIX + flightId;
		return addReservation(reservationStringId, flightId, numSeat, price,
				requestId, retry);
	}

	/*
	 * ==========================================================================
	 * === manager_deleteFlight -- Delete an entire flight -- Fails if customer
	 * has reservation on this flight -- Returns TRUE on success, else FALSE
	 * ====
	 * =========================================================================
	 */
	boolean deleteFlight(int flightId, RequestId requestId, boolean retry) {
		final String reservationStringId = Vacation.FLIGHT_PREFIX + flightId;

		// Reservation reservation = (Reservation) flightTable.find(flightId);
		Reservation reservation = (Reservation) stmInstance.open(
				reservationStringId, stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
		if (reservation == null) {
			return false;
		}

		if (reservation.numUsed > 0) {
			return false; /* somebody has a reservation */
		}

		return addReservation(reservationStringId, flightId,
				-reservation.numTotal, -1 /* -1 keeps old price */, requestId,
				retry);
	}

	/*
	 * ==========================================================================
	 * === manager_addCustomer -- If customer already exists, returns failure --
	 * Returns TRUE on success, else FALSE
	 * ======================================
	 * =======================================
	 */
	boolean addCustomer(int customerId, RequestId requestId, boolean retry) {
		final String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		Customer customer = (Customer) stmInstance.open(customerStringId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
		;

		// if (customerTable.contains(customerId))
		if (customer != null) {
			return false;
		}

		if (customer.isNull == true) {
			customer.isNull = false;
			// stmInstance.write(customer, customerStringId, requestId);
		} else {
			customer = new Customer(customerStringId, customerId);
			this.sharedObjectRegistry.registerObjects(customerStringId,
					customer,MaxSpec);
		}
		// assert(customerPtr != null);
		// customerTable.insert(customerId, customer);

		return true;
	}

	/*
	 * ==========================================================================
	 * === manager_deleteCustomer -- Delete this customer and associated
	 * reservations -- If customer does not exist, returns success -- Returns
	 * TRUE on success, else FALSE
	 * ==============================================
	 * ===============================
	 */
	@SuppressWarnings("unchecked")
	boolean deleteCustomer(int customerId, RequestId requestId, boolean retry) {
		final String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		Customer customer = (Customer) stmInstance.open(customerStringId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
		;
		if (customer == null || customer.isNull == true) {
			return false;
		}

		// stmInstance.write(customer, customerStringId, requestId);

		LinkedList<ReservationInfo> reservationInfoList = customer.reservationInfoList;
		Iterator<ReservationInfo> iter = reservationInfoList.iterator();

		String reservationStringId;
		while (iter.hasNext()) {
			ReservationInfo reservationInfo = iter.next();
			if (reservationInfo.type == Vacation.RESERVATION_CAR) {
				reservationStringId = Vacation.CAR_PREFIX + reservationInfo.id;
			} else if (reservationInfo.type == Vacation.RESERVATION_ROOM) {
				reservationStringId = Vacation.ROOM_PREFIX + reservationInfo.id;
			} else {
				reservationStringId = Vacation.FLIGHT_PREFIX
						+ reservationInfo.id;
			}
			Reservation reservation = (Reservation) stmInstance.open(
					reservationStringId, stmInstance.TX_READ_WRITE_MODE,
					requestId, stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
			if (reservation != null) {
				reservation.reservation_cancel();
				// stmInstance.write(reservation, reservationStringId,
				// requestId);
			}
		}

		// equivalent to removing the customer entry
		customer.isNull = true;
		// stmInstance.write(customer, customerStringId, requestId);
		// customerTable.remove(customerId);
		return true;
	}

	int queryNumFree(RequestId requestId, String reservationId, int id,
			boolean retry) {
		int numFree = -1;
		Reservation reservation = (Reservation) stmInstance.open(reservationId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
		// Reservation reservation = table.find(id);
		if (reservation != null) {
			numFree = reservation.numFree;
		}

		return numFree;
	}

	int queryPrice(RequestId requestId, String reservationId, int id,
			boolean retry) {
		int price = -1;
		Reservation reservationPtr = (Reservation) stmInstance.open(
				reservationId, stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
		// Reservation reservationPtr = table.find(id);
		if (reservationPtr != null) {
			price = reservationPtr.price;
		}

		return price;
	}

	final int queryCar(int carId, RequestId requestId, boolean retry) {
		final String reservationId = Vacation.CAR_PREFIX + carId;
		return queryNumFree(requestId, reservationId, carId, retry);
	}

	final int queryCarPrice(int carId, RequestId requestId, boolean retry) {
		final String reservationId = Vacation.CAR_PREFIX + carId;
		return queryPrice(requestId, reservationId, carId, retry);
	}

	final int queryRoom(int roomId, RequestId requestId, boolean retry) {
		final String reservationId = Vacation.ROOM_PREFIX + roomId;
		return queryNumFree(requestId, reservationId, roomId, retry);
	}

	final int queryRoomPrice(int roomId, RequestId requestId, boolean retry) {
		final String reservationId = Vacation.ROOM_PREFIX + roomId;
		return queryPrice(requestId, reservationId, roomId, retry);
	}

	final int queryFlight(int flightId, RequestId requestId, boolean retry) {
		final String reservationId = Vacation.FLIGHT_PREFIX + flightId;
		return queryNumFree(requestId, reservationId, flightId, retry);
	}

	final int queryFlightPrice(int flightId, RequestId requestId, boolean retry) {
		final String reservationId = Vacation.FLIGHT_PREFIX + flightId;
		return queryPrice(requestId, reservationId, flightId, retry);
	}

	int queryCustomerBill(int customerId, RequestId requestId, boolean retry) {
		int bill = -1;
		final String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		Customer customer;

		customer = (Customer) stmInstance.open(customerStringId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());

		if (customer != null) {
			bill = customer.customer_getBill();
		}

		return bill;
	}

	boolean reserve(String reservationStringId, String customerStringId,
			int customerId, int id, int type, RequestId requestId, boolean retry) {
		Customer customer;
		Reservation reservation;

		customer = (Customer) stmInstance.open(customerStringId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());

		if (customer == null) {
			return false;
		}

		reservation = (Reservation) stmInstance.open(reservationStringId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
		if (reservation == null) {
			return false;
		}

		if (!reservation.reservation_make()) {
			return false;
		}

		if (!customer.customer_addReservationInfo(type, id, reservation.price)) {
			/* Undo previous successful reservation */
			// boolean status =
			reservation.reservation_cancel();
			return false;
		}
		// stmInstance.write(customer, customerStringId, requestId);
		// stmInstance.write(reservation, reservationStringId, requestId);
		return true;
	}

	final boolean reserveCar(int customerId, int carId, RequestId requestId,
			boolean retry) {
		final String reservationStringId = Vacation.CAR_PREFIX + carId;
		String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		return reserve(reservationStringId, customerStringId, customerId,
				carId, Vacation.RESERVATION_CAR, requestId, retry);
	}

	final boolean reserveRoom(int customerId, int roomId, RequestId requestId,
			boolean retry) {
		final String reservationStringId = Vacation.ROOM_PREFIX + roomId;
		String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		return reserve(reservationStringId, customerStringId, customerId,
				roomId, Vacation.RESERVATION_ROOM, requestId, retry);
	}

	final boolean reserveFlight(int customerId, int flightId,
			RequestId requestId, boolean retry) {
		final String reservationStringId = Vacation.FLIGHT_PREFIX + flightId;
		String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		return reserve(reservationStringId, customerStringId, customerId,
				flightId, Vacation.RESERVATION_FLIGHT, requestId, retry);
	}

	/*
	 * ==========================================================================
	 * === cancel -- Customer is not allowed to cancel multiple times -- Returns
	 * TRUE on success, else FALSE
	 * ==============================================
	 * ===============================
	 */
	boolean cancel(String reservationStringId, String customerStringId,
			int customerId, int id, int type, RequestId requestId, boolean retry) {
		Customer customerPtr;
		Reservation reservationPtr;

		customerPtr = (Customer) stmInstance.open(customerStringId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
		if (customerPtr == null) {
			return false;
		}

		reservationPtr = (Reservation) stmInstance.open(reservationStringId,
				stmInstance.TX_READ_WRITE_MODE, requestId,
				stmInstance.OBJECT_WRITE_MODE, retry, stmInstance.getTransactionId());
		if (reservationPtr == null) {
			return false;
		}

		if (!reservationPtr.reservation_cancel()) {
			return false;
		}

		if (!customerPtr.customer_removeReservationInfo(type, id)) {
			/* Undo previous successful cancellation */
			// boolean status =
			reservationPtr.reservation_make();
			return false;
		}
		// stmInstance.write(customerPtr, customerStringId, requestId);
		// stmInstance.write(reservationPtr, reservationStringId, requestId);

		return true;
	}

	final boolean cancelCar(int customerId, int carId, RequestId requestId,
			boolean retry) {
		final String reservationStringId = Vacation.CAR_PREFIX + carId;
		String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		return cancel(reservationStringId, customerStringId, customerId, carId,
				Vacation.RESERVATION_CAR, requestId, retry);
	}

	final boolean cancelRoom(int customerId, int roomId, RequestId requestId,
			boolean retry) {
		final String reservationStringId = Vacation.ROOM_PREFIX + roomId;
		String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		return cancel(reservationStringId, customerStringId, customerId,
				roomId, Vacation.RESERVATION_ROOM, requestId, retry);
	}

	final boolean cancelFlight(int customerId, int flightId,
			RequestId requestId, boolean retry) {
		final String reservationStringId = Vacation.FLIGHT_PREFIX + flightId;
		String customerStringId = Vacation.CUSTOMER_PREFIX + customerId;
		return cancel(reservationStringId, customerStringId, customerId,
				flightId, Vacation.RESERVATION_FLIGHT, requestId, retry);
	}

	public byte[] makeReservation(ClientRequest request, boolean retry) {

		RequestId requestId = request.getRequestId();
		Random random = new Random();

		final int types[] = new int[numQueriesPerTransactions];
		final int ids[] = new int[numQueriesPerTransactions];

		final int maxPrices[] = new int[Vacation.NUM_RESERVATION_TYPE];
		final int maxIds[] = new int[Vacation.NUM_RESERVATION_TYPE];
		maxPrices[0] = -1;
		maxPrices[1] = -1;
		maxPrices[2] = -1;
		maxIds[0] = -1;
		maxIds[1] = -1;
		maxIds[2] = -1;

		int queryRange = (int) ((double) percentOfQueries / 100.0
				* (double) numRelations + 0.5);

		int accessibleRange = queryRange / this.numReplicas;
		int min = accessibleRange * this.localId;

		int numQuery = random.nextInt(numQueriesPerTransactions) + 1;
		int customerId = random.nextInt(accessibleRange) + min + 1;
		
		if(customerId < min + 1 && customerId > min+accessibleRange+1) {
			System.out.println("mR: " + customerId);
		}

		for (int n = 0; n < numQuery; n++) {
			types[n] = random.nextInt(Vacation.NUM_RESERVATION_TYPE);
			ids[n] = random.nextInt(accessibleRange) + min + 1;
			
			if(ids[n] < min + 1 && ids[n] > min+accessibleRange+1) {
				System.out.println("mR ids: " + ids[n]);
			}
		}

		boolean isFound = false;

		for (int n = 0; n < numQuery; n++) {
			int t = types[n];
			int id = ids[n];
			int price = -1;
			if (t == Vacation.RESERVATION_CAR) {
				if (queryCar(id, requestId, retry) >= 0) {
					price = queryCarPrice(id, requestId, retry);
				}
			} else if (t == Vacation.RESERVATION_FLIGHT) {
				if (queryFlight(id, requestId, retry) >= 0) {
					price = queryFlightPrice(id, requestId, retry);
				}
			} else if (t == Vacation.RESERVATION_ROOM) {
				if (queryRoom(id, requestId, retry) >= 0) {
					price = queryRoomPrice(id, requestId, retry);
				}
			}
			if (price > maxPrices[t]) {
				maxPrices[t] = price;
				maxIds[t] = id;
				isFound = true;
			}
		} /* for n */
		if (isFound) {
			addCustomer(customerId, requestId, retry);
		}
		if (maxIds[Vacation.RESERVATION_CAR] > 0) {
			reserveCar(customerId, maxIds[Vacation.RESERVATION_CAR], requestId,
					retry);
		}
		if (maxIds[Vacation.RESERVATION_FLIGHT] > 0) {
			reserveFlight(customerId, maxIds[Vacation.RESERVATION_FLIGHT],
					requestId, retry);
		}
		if (maxIds[Vacation.RESERVATION_ROOM] > 0) {
			reserveRoom(customerId, maxIds[Vacation.RESERVATION_ROOM],
					requestId, retry);
		}

		byte[] result = new byte[4];

		stmInstance.updateUnCommittedSharedCopy(requestId);
		// stmInstance.onCommit(request);
		return result;
	}

	public byte[] deleteCustomer(ClientRequest request, boolean retry) {
		RequestId requestId = request.getRequestId();
		Random randomPtr = new Random();

		int queryRange = (int) ((double) percentOfQueries / 100.0
				* (double) numRelations + 0.5);

		int accessibleRange = queryRange / this.numReplicas;
		int min = accessibleRange * this.localId;
//		int max = accessibleRange * (this.localId+1);
		
//		System.out.println("Local Id: " + this.localId + " Num Rep: " + this.numReplicas);
//		System.out.println("qRange: " + queryRange);
//		System.out.println("min: " + min + "max: " + max);
//		System.exit(-1);

		final int customerId = randomPtr.nextInt(accessibleRange) + min + 1;
		int bill = queryCustomerBill(customerId, requestId, retry);
		if (bill >= 0) {
			deleteCustomer(customerId, requestId, retry);
		}

		byte[] result = new byte[4];

		stmInstance.updateUnCommittedSharedCopy(requestId);
		// stmInstance.onCommit(request);
		return result;
	}

	public byte[] updateTable(ClientRequest request, boolean retry) {
		RequestId requestId = request.getRequestId();
		Random randomPtr = new Random();

		int queryRange = (int) ((double) percentOfQueries / 100.0
				* (double) numRelations + 0.5);

		int accessibleRange = queryRange / this.numReplicas;
		int min = accessibleRange * this.localId;

		final int types[] = new int[numQueriesPerTransactions];
		final int ids[] = new int[numQueriesPerTransactions];
		final int ops[] = new int[numQueriesPerTransactions];
		final int prices[] = new int[numQueriesPerTransactions];

		final int numUpdate = randomPtr.nextInt(numQueriesPerTransactions) + 1;
		for (int n = 0; n < numUpdate; n++) {
			types[n] = randomPtr.nextInt(Vacation.NUM_RESERVATION_TYPE);
			ids[n] = (randomPtr.nextInt(accessibleRange) + min + 1);
			ops[n] = randomPtr.nextInt(2);
			if (ops[n] == 1) {
				prices[n] = ((randomPtr.nextInt(5)) * 10) + 50;
			}
		}

		for (int n = 0; n < numUpdate; n++) {
			int t = types[n];
			int id = ids[n];
			int doAdd = ops[n];
			if (doAdd == 1) {
				int newPrice = prices[n];
				if (t == Vacation.RESERVATION_CAR) {
					addCar(id, 100, newPrice, requestId, retry);
				} else if (t == Vacation.RESERVATION_FLIGHT) {
					addFlight(id, 100, newPrice, requestId, retry);
				} else if (t == Vacation.RESERVATION_ROOM) {
					addRoom(id, 100, newPrice, requestId, retry);
				}
			} else { /* do delete */
				if (t == Vacation.RESERVATION_CAR) {
					deleteCar(id, 100, requestId, retry);
				} else if (t == Vacation.RESERVATION_FLIGHT) {
					deleteFlight(id, requestId, retry);
				} else if (t == Vacation.RESERVATION_ROOM) {
					deleteRoom(id, 100, requestId, retry);
				}
			}
		}

		stmInstance.updateUnCommittedSharedCopy(requestId);

		byte[] result = new byte[4];
		// stmInstance.onCommit(request);
		return result;
	}

	/*************************************************************************
	 * * This method is used for two purposes. 1. For write transaction which is
	 * executed speculatively 2. For write transaction which is retied after
	 * commit failed
	 * 
	 * @param request
	 * @param retry
	 ************************************************************************/
	public void executeWriteRequest(final ClientRequest request,
			final boolean retry) {
		// TODO Auto-generated method stub
		byte[] value = request.getValue();
		ByteBuffer buffer = ByteBuffer.wrap(value);
		final RequestId requestId = request.getRequestId();

		byte transactionType = buffer.get();
		int commandType = buffer.getInt();

		// System.out.println(commandType);
		if (commandType == Vacation.ACTION_MAKE_RESERVATION) {

			// System.out.print(".");
			stmInstance.executeWriteRequest(new Runnable() {
				public void run() {
					makeReservation(request, retry);
					stmInstance.onExecuteComplete(request);
				}
			});
			makeResCount++;
		} else if (commandType == Vacation.ACTION_DELETE_CUSTOMER) {
			// System.out.print("-");
			stmInstance.executeWriteRequest(new Runnable() {
				public void run() {
					deleteCustomer(request, retry);
					stmInstance.onExecuteComplete(request);

				}
			});
			delCusCount++;
		} else if (commandType == Vacation.ACTION_UPDATE_TABLES) {

			// System.out.print(":");
			stmInstance.executeWriteRequest(new Runnable() {
				public void run() {
					updateTable(request, retry);
					stmInstance.onExecuteComplete(request);

				}
			});
			updateTableCount++;
		} else {
			System.out.println("Wrong WR command " + commandType
					+ " transaction type " + transactionType);
		}

	}

	/**
	 * This method fills the parameters in the request byte array for client for
	 * the bank benchmark.
	 * 
	 * @param request
	 *            : byte array
	 * @param readOnly
	 *            : boolean specifying what should be the transaction type
	 */
	public byte[] createRequest(int percent, int requestType) {
		byte[] request = new byte[DEFAULT_LENGTH];
		// Random random = new Random();

		ByteBuffer buffer = ByteBuffer.wrap(request);

		if (requestType == Vacation.ACTION_MAKE_RESERVATION) {
			buffer.put(READ_WRITE_TX);
			; // buffer.putInt(TransactionType.ReadWriteTransaction.ordinal());
			buffer.putInt(Vacation.ACTION_MAKE_RESERVATION);
		} else if ((percent & 1) == 1) {
			buffer.put(READ_WRITE_TX); // buffer.putInt(TransactionType.ReadWriteTransaction.ordinal());
			buffer.putInt(Vacation.ACTION_DELETE_CUSTOMER);
		} else {
			buffer.put(READ_WRITE_TX);
			; // buffer.putInt(TransactionType.ReadWriteTransaction.ordinal());
			buffer.putInt(Vacation.ACTION_UPDATE_TABLES);
		}

		buffer.flip();
		return request;
	}

	// /**
	// * Executes batch of requests speculatively, shared data gets committed
	// later on consensus.
	// */
	// @Override
	// public void executeSpeculatively(final int instance, final ClientRequest
	// bInfo) {
	// // TODO Auto-generated method stub
	// vacationSTMDispatcher.submit(new Runnable() {
	// public void run() {
	// ClientRequest[] requestBatch = bInfo.batch;
	// for(ClientRequest request : requestBatch) {
	// byte[] value = request.getValue();
	// if(value != null) {
	// //System.out.println("Execute: " + request.getRequestId().toString());
	// executeWriteRequest(request, false);
	// } else {
	// System.out.println("null byte array : Bank execute batch");
	// }
	// }
	// }
	// });
	// }

	// Dummy method
	public void executeRequest(ClientRequest request, boolean retry) {

	}

	// /**
	// * Commit ordered batch by committing the shared data modified by previous
	// speculative
	// * execution which followed propose message from leader.
	// */
	// @Override
	// public void commitExecutedRequests(final int instance, final
	// ClientRequest bInfo) {
	// vacationSTMDispatcher.submit(new Runnable() {
	// public void run() {
	// ClientRequest[] requestBatch = bInfo.batch;
	// for(ClientRequest request : requestBatch) {
	// //System.out.println("Commit: " + request.getRequestId().toString());
	// commitBatchOnDecision(request);
	// }
	// }
	// });
	// }

	/**
	 * A common interface to send the reply to client request back to client
	 * through replica
	 * 
	 * @param result
	 * @param cRequest
	 */
	public void sendReply(byte[] result, ClientRequest cRequest) {
		// System.out.println("Sending reply to " +
		// cRequest.getRequestId().toString());
		// replica.replyToClient(result, cRequest);
	}

	/**
	 * Called by network layer to commit a previous speculatively executed
	 * batch.
	 */
	@Override
	public void commitBatchOnDecision(final RequestId rId,
			final TransactionContext txContext) {
		// TODO Auto-generated method stub
		// Validate sequence
		// If validated - commit -- Delete RequestId from
		// LocalTransactionManager.requestDirtycopyMap
		// else abort and retry

		stmInstance.executeCommitRequest(new Runnable() {
			public void run() {
				// System.out.println("Comm");
				onCommit(rId, txContext);
			}
		});
	}

	/*************************************************************************
	 * 
	 * @param requestId
	 * @param commandType
	 * @return
	 * 
	 *         This method commits the given transaction. It first validates the
	 *         readset and then after validating shadowcopy too, updates the
	 *         objects in SharedObjectRegistry with the shadowcopy. Finally it
	 *         removes all the data for optimistically executed transaction
	 *         (cleanup).
	 ************************************************************************/
	public void onCommit(RequestId rId, TransactionContext txContext) {

		// Validate the transaction object versions or Decided InstanceIds and
		// sequence numbers
		// Check the version of object in stable copy and see if the current
		// shadowcopy version is +1
		// and InstanceId of shadowcopy matches with the stable copy.
		// Object object = null;
		// boolean retry = true;
		// RequestId requestId = cRequest.getRequestId();

		// Validate read set first
//		System.out.println("*onCommit");
		if (stmInstance.validateReadset(txContext)) {
			stmInstance.updateSharedObject(txContext);
			committedCount++;
		} else {
			abortedCount++;
			// Isn;t it needed to remove the previous content
			// stmInstance.removeTransactionContext(txContext);
			// executeRequest(cRequest, retry);
			// stmInstance.updateSharedObject(requestId);
		}
		// writeCount++;
		// remove the entries for this transaction LTM (TransactionContext,
		// lastModifier)
		// object = stmInstance.getResultFromContext(requestId);
		// sendReply(stmInstance.getResultFromContext(requestId), cRequest);
		client.replyToClient(rId);

		stmInstance.removeTransactionContext(rId);
		requestIdValueMap.remove(rId);

	}

	/**
	 * Used to execute read requests from clients locally.
	 */
	@Override
	public void executeReadRequest(final ClientRequest cRequest) {
		// TODO Auto-generated method stub
		vacationSTMDispatcher.submit(new Runnable() {
			public void run() {
				executeRequest(cRequest, false);
			}
		});
	}

	@Override
	public Replica getReplica() {
		// TODO Auto-generated method stub
		return this.replica;
	}

	@Override
	public void notifyCommitManager(Request request) {
		// TODO Auto-generated method stub
		stmInstance.notifyCommitManager(request);
	}

	/**
	 * Read the command name from the client request byte array.
	 * 
	 * @param value
	 * @return
	 */
	public int getCommandName(byte[] value) {
		ByteBuffer buffer = ByteBuffer.wrap(value);
		byte transactionType = buffer.get();
		int command = buffer.getInt();
		buffer.flip();
		return command;

	}
	
	@Override
	public byte[] serializeTransactionContext(TransactionContext ctx) throws IOException {
		Map<String, AbstractObject> readset = ctx.getReadSet();
		Map<String, AbstractObject> writeset = ctx.getWriteSet();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteBuffer bb;

		bb = ByteBuffer.allocate(4);
		bb.putInt(readset.size());
		bb.flip();

		out.write(bb.array());
		for (Map.Entry<String, AbstractObject> entry : readset.entrySet()) {

			String id = entry.getKey();
			byte[] idBytes = id.getBytes(Charset.forName("UTF-8"));

			bb = ByteBuffer.allocate(idBytes.length + 4 + 8);

			bb.putInt(idBytes.length);
			bb.put(idBytes);
			bb.putLong(entry.getValue().getVersion());

			bb.flip();

			out.write(bb.array());

		}

		bb = ByteBuffer.allocate(4);
		bb.putInt(writeset.size());
		bb.flip();

		out.write(bb.array());

		for (Map.Entry<String, AbstractObject> entry : writeset.entrySet()) {

			String id = entry.getKey();
			byte[] idBytes = id.getBytes(Charset.forName("UTF-8"));

			ByteBuffer bb1 = ByteBuffer.allocate(idBytes.length + 4);

			bb1.putInt(idBytes.length);
			bb1.put(idBytes);

			bb1.flip();
			out.write(bb1.array());

			Object object = entry.getValue();
			if (object instanceof Reservation) {
				bb = ByteBuffer.allocate(30);

				bb.putShort((short) 0);

				Reservation reservation = (Reservation) object;

				bb.putInt(reservation.id);
				bb.putInt(reservation.numFree);
				bb.putInt(reservation.numTotal);
				bb.putInt(reservation.numUsed);
				bb.putInt(reservation.price);

				bb.putLong(reservation.getVersion());

				bb.flip();
				out.write(bb.array());

			} else if (object instanceof Customer) {

				Customer customer = (Customer) object;

				LinkedList<ReservationInfo> list = customer.reservationInfoList;

				bb = ByteBuffer.allocate(18 + (list.size() * 12));

				bb.putShort((short) 1);

				bb.putInt(customer.id);
				bb.putInt(list.size());

				for (int i = 0; i < list.size(); i++) {
					ReservationInfo reservationInfo = list.get(i);
					if (reservationInfo != null) {
						bb.putInt(reservationInfo.id);
						bb.putInt(reservationInfo.type);
						bb.putInt(reservationInfo.price);
					}
				}

				bb.putLong(customer.getVersion());

				bb.flip();
				out.write(bb.array());

			} else {

				System.out
						.println("Tpcc Object serialization: object not defined");

			}
		}

		return out.toByteArray();
	}

	@Override
	public TransactionContext deserializeTransactionContext(byte[] bytes) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(bytes);

		TransactionContext ctx = new TransactionContext();

		int readsetSize = bb.getInt();
		for (int i = 0; i < readsetSize; i++) {
			byte[] value = new byte[bb.getInt()];
			bb.get(value);

			String id = new String(value, Charset.forName("UTF-8"));

			long version = bb.getLong();

			TpccItem object = new TpccItem(id);
			object.setVersion(version);

			ctx.addObjectToReadSet(id, object);
		}

		int writesetSize = bb.getInt();
		for (int i = 0; i < writesetSize; i++) {
			byte[] value = new byte[bb.getInt()];
			bb.get(value);

			String id = new String(value, Charset.forName("UTF-8"));

			short type = bb.getShort();

			switch (type) {
			case 0: {
				Reservation object = new Reservation();

				object.id = bb.getInt();
				object.numFree = bb.getInt();
				object.numTotal = bb.getInt();
				object.numUsed = bb.getInt();
				object.price = bb.getInt();

				object.setVersion(bb.getLong());

				ctx.addObjectToWriteSet(id, object);
				break;
			}
			case 1: {
				Customer object = new Customer();

				object.id = bb.getInt();

				int size = bb.getInt();

				LinkedList<ReservationInfo> list = new LinkedList<ReservationInfo>();

				ReservationInfo info;
				for (int j = 0; j < size; j++) {
					info = new ReservationInfo();

					info.id = bb.getInt();
					info.type = bb.getInt();
					info.price = bb.getInt();
				}

				object.reservationInfoList = list;

				object.setVersion(bb.getLong());

				ctx.addObjectToWriteSet(id, object);
				break;
			}
			default:
				System.out.println("Invalid Object Type");
			}

		}

		return ctx;
	}
	
	@Override
	public byte[] execute(byte[] value, int executeSeqNo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void askForSnapshot(int lastSnapshotNextRequestSeqNo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void forceSnapshot(int lastSnapshotNextRequestSeqNo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addSnapshotListener(SnapshotListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSnapshotListener(SnapshotListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateToSnapshot(int nextRequestSeqNo, byte[] snapshot) {
		// TODO Auto-generated method stub

	}

}
