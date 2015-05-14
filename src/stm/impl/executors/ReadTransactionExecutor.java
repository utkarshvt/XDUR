package stm.impl.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class ReadTransactionExecutor {
	ExecutorService executor; 

	// 4 ~ Threads for (ReadExecutor + SPaxos + ClientManager + Replica Communication etc.) 
	final int NUMBER_THREADS_FOR_OTHER_PARTS_OF_SPAXOS = 4;
	long failCount = 0;
	
	public ReadTransactionExecutor(int readThreadCount) {
//		int processors = Runtime.getRuntime().availableProcessors();
//		int NTHREADS = processors - NUMBER_THREADS_FOR_OTHER_PARTS_OF_SPAXOS; 
//		if(NTHREADS<= 5)
//			NTHREADS = 2;
//		else if (NTHREADS<= 10)
//			NTHREADS = 10;
//		else 
//			NTHREADS = 20;
//		executor = Executors.newFixedThreadPool(NTHREADS);
		executor = Executors.newFixedThreadPool(readThreadCount);
//		System.out.print("Read Threads= " + NTHREADS + " ");
	}
	
	public void execute(Runnable task) {
		try {
			executor.execute(task);
		} catch (RejectedExecutionException ex) {
			failCount++;
		}
	}
	
	public long shutDownWriteExecutor() {
		executor.shutdown();
		try {
			executor.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return failCount;
	}
}
