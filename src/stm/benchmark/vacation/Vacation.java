package stm.benchmark.vacation;

public class Vacation {
	public static int ACTION_MAKE_RESERVATION = 0;
	public static int ACTION_DELETE_CUSTOMER = 1;
	public static int ACTION_UPDATE_TABLES = 2;
	public static int NUM_ACTION = 2;
	public static char PARAM_CLIENTS = 'c';
	public static char PARAM_NUMBER = 'n';
	public static char PARAM_QUERIES = 'q';
	public static char PARAM_RELATIONS = 'r';
	public static char PARAM_TRANSACTIONS = 't';
	public static char PARAM_USER = 'u';
	public static int PARAM_DEFAULT_CLIENTS = (1);
	public static int PARAM_DEFAULT_NUMBER = (10);
	public static int PARAM_DEFAULT_QUERIES = (90);
	public static int PARAM_DEFAULT_RELATIONS = (1 << 16);
	public static int PARAM_DEFAULT_TRANSACTIONS = (1 << 26);
	public static int PARAM_DEFAULT_USER = (80);
	public static int RESERVATION_CAR = 0;
	public static int RESERVATION_FLIGHT = 1;
	public static int RESERVATION_ROOM = 2;
	public static int NUM_RESERVATION_TYPE = 3;
	public static long OPERATION_MAKE_RESERVATION = 0L;
	public static long OPERATION_DELETE_CUSTOMER = 1L;
	public static long OPERATION_UPDATE_TABLE = 2L;
	public static long NUM_OPERATION = 3;

	private static String MANAGER = "manager";
	//private static String STATS = "stats";
	public static String CAR_PREFIX = "c_";
	public static String FLIGHT_PREFIX = "f_";
	public static String ROOM_PREFIX = "r_";
	public static String CUSTOMER_PREFIX = "cu_";
	
	int numClients = PARAM_DEFAULT_CLIENTS;
	int numQueriesPerTransactions = PARAM_DEFAULT_NUMBER;
	int percentOfQueries = PARAM_DEFAULT_QUERIES;
	int numRelations = PARAM_DEFAULT_RELATIONS;
	int percentOfUser = PARAM_DEFAULT_USER;
	long mills = 10000;

//	Manager manager;
//	Stats stats;
//
//	protected Vacation(int processId, int numberOfProcesses, String[] args)
//	{
//		super(processId, numberOfProcesses, args);
//		parseArgs(args);
//	}

//	@Override
//	public void setupMaster()
//	{
//		System.out.print("Initializing manager... ");
//		new Transaction()
//		{
//			@Override
//			protected void atomic()
//			{
//				int i;
//				int t;
//
//				Random random = new Random();
//				random.random_alloc();
//				manager = new Manager();
//
//				int numRelation = numRelations;
//				int ids[] = new int[numRelation];
//				for (i = 0; i < numRelation; i++)
//				{
//					ids[i] = i + 1;
//				}
//
//				for (t = 0; t < 4; t++)
//				{
//					/* Shuffle ids */
//					for (i = 0; i < numRelation; i++)
//					{
//						int x = random.posrandom_generate() % numRelation;
//						int y = random.posrandom_generate() % numRelation;
//						int tmp = ids[x];
//						ids[x] = ids[y];
//						ids[y] = tmp;
//					}
//
//					/* Populate table */
//					for (i = 0; i < numRelation; i++)
//					{
//						int id = ids[i];
//						int num = ((random.posrandom_generate() % 5) + 1) * 100;
//						int price = ((random.posrandom_generate() % 5) * 10) + 50;
//						if (t == 0)
//						{
//							manager.addCar(id, num, price);
//						}
//						else if (t == 1)
//						{
//							manager.addFlight(id, num, price);
//						}
//						else if (t == 2)
//						{
//							manager.addRoom(id, num, price);
//						}
//						else if (t == 3)
//						{
//							manager.addCustomer(id);
//						}
//						// assert(status);
//					}
//
//				} /* for t */
//
//				PaxosSTM.getInstance().addToSharedObjectRegistry(MANAGER,
//						manager);
//				stats = new Stats();
//				PaxosSTM.getInstance().addToSharedObjectRegistry(STATS, stats);
//			}
//		};
//		System.out.println("done.");
//	}
//
//	@Override
//	public void setupSlave()
//	{
//		manager = (Manager) PaxosSTM.getInstance().getFromSharedObjectRegistry(
//				MANAGER);
//		stats = (Stats) PaxosSTM.getInstance().getFromSharedObjectRegistry(
//				STATS);
//	}
//
//	@Override
//	public void cleanUpMaster()
//	{
//		PaxosSTM.getInstance().removeFromSharedObjectRegistry(MANAGER);
//		PaxosSTM.getInstance().getFromSharedObjectRegistry(STATS);
//	}
//
//	@Override
//	public void run()
//	{
//		TestCounter.getInstance().reset();
//
//		System.out
//		.println(" th   time   quer/tr   numRel   %quer  %users  #queryRg   AmakeRes    AdelCus   Aupdate   GAmakeRes   GAdelCus   GAupdate    Gtr/sec     Gabort   Gl    gc    lc");
//		System.out.flush();
//		
//		int queryRange = (int) ((double) percentOfQueries / 100.0
//				* (double) numRelations + 0.5);
//
//		Client[] clients = new Client[numClients];
//		VacationInBean[] inBeans = new VacationInBean[numClients];
//		VacationOutBean[] outBeans = new VacationOutBean[numClients];
//		for (int i = 0; i < numClients; i++)
//		{
//			inBeans[i] = new VacationInBean(PaxosSTM.getInstance().getId(), i, manager,
//					numQueriesPerTransactions, queryRange, percentOfUser);
//			outBeans[i] = new VacationOutBean();
//			clients[i] = new Client(inBeans[i], outBeans[i]);
//		}
//
//		PaxosSTM.getInstance().enterBarrier("startBenchmark", numberOfProcesses);
//
//		for (int i = 0; i < numClients; i++)
//			clients[i].start();
//
//		try
//		{
//			Thread.sleep(mills);
//		}
//		catch (InterruptedException e)
//		{
//			e.printStackTrace();
//		}
//
//		Client.stop = true;
//
//		final int processedActions[] = new int[3];
//		long totalTime = 0;
//		try
//		{
//			for (int i = 0; i < numClients; i++)
//			{
//				clients[i].join();
//				processedActions[0] += outBeans[i].performedActions[0];
//				processedActions[1] += outBeans[i].performedActions[1];
//				processedActions[2] += outBeans[i].performedActions[2];
//				totalTime += outBeans[i].duration;
//			}
//		}
//		catch (InterruptedException e)
//		{
//		}
//
//		final long totalDuration = totalTime;
//
//		new Transaction()
//		{
//			@Override
//			protected void atomic()
//			{
//				stats.totalActionMakeResevation += processedActions[0];
//				stats.totalActionDeleteCustomer += processedActions[1];
//				stats.totalActionUpdateTables += processedActions[2];
//				stats.totalDuration += totalDuration;
//				stats.totalNumThreads += numClients;
//				stats.totalLocalConflicts += TestCounter.getInstance()
//						.getLocalConflicts();
//			}
//		};
//		makeSnapshot();
//
//		PaxosSTM.getInstance().enterBarrier("results", numberOfProcesses);
//
//		final long[] results = new long[6];
//
//		new Transaction()
//		{
//			@Override
//			protected void atomic()
//			{
//				results[0] = stats.totalActionMakeResevation;
//				results[1] = stats.totalActionDeleteCustomer;
//				results[2] = stats.totalActionUpdateTables;
//				results[3] = stats.totalDuration;
//				results[4] = stats.totalNumThreads;
//				results[5] = stats.totalLocalConflicts;
//
//			}
//		};
//
//		String format = "%3d %6d %6d      %6d  %6d  %6d    %6d    %7d    %7d   %7d     %7d    %7d    %7d    %7.2f     %5.2f  %4d  %4d  %4d\n";
//
//		float gabort = 1f
//				* (TestCounter.getInstance().getGlobalConflicts() + results[5])
//				/ (TestCounter.getInstance().getGlobalConflicts() + results[5]
//						+ results[0] + results[1] + results[2]);
//
//		// System.err.println(results[0] + " " + results[1] + " " + results[2]
//		// + " " + results[3] + " " + results[4] + " " + results[5]);
//
//		System.out.format(format, numClients, totalDuration / numClients,
//				numQueriesPerTransactions, numRelations, percentOfQueries,
//				percentOfUser, queryRange, processedActions[0],
//				processedActions[1], processedActions[2], results[0],
//				results[1], results[2], 1000f
//						* (results[0] + results[1] + results[2])
//						/ (results[3] / results[4]), gabort, results[5],
//				TestCounter.getInstance().getGlobalConflicts(), TestCounter
//						.getInstance().getLocalConflicts());
//
//		System.out.println("readSet size:\t"
//				+ TestCounter.getInstance().getAvgReadSetSize());
//		System.out.println("writeSet size:\t"
//				+ TestCounter.getInstance().getAvgWriteSetSize());
//		System.out.println("newSet size:\t"
//				+ TestCounter.getInstance().getAvgNewSetSize());
//		System.out.println("typeSet size:\t"
//				+ TestCounter.getInstance().getAvgTypeSetSize());
//		System.out.println("package size:\t"
//				+ TestCounter.getInstance().getAvgPackageSize());
//	}
//
//	public void parseArgs(String args[])
//	{
//		if (args != null && args.length > 0)
//		{
//			numClients = Integer.parseInt(args[0]);
//			numQueriesPerTransactions = Integer.parseInt(args[1]);
//			numRelations = Integer.parseInt(args[2]);
//			percentOfQueries = Integer.parseInt(args[3]);
//			percentOfUser = Integer.parseInt(args[4]);
//			mills = Long.parseLong(args[5]);
//		}
//	}
//
//	public static boolean addCustomer(Manager managerPtr, int id, int num,
//			int price)
//	{
//		return managerPtr.addCustomer(id);
//	}
}
