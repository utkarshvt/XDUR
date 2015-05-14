package stm.impl.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class WriteTransactionExecutor {
	ExecutorService executor; 
	long failedCount = 0;	
	/* Default constructor with 1 thread pool */
	public WriteTransactionExecutor() {
		executor = Executors.newFixedThreadPool(1); //newSingleThreadExecutor();
	}
	

	public WriteTransactionExecutor( int MaxSpec) {
		executor = Executors.newFixedThreadPool(MaxSpec); //newSingleThreadExecutor();
	}
	
	public void execute(Runnable task) {
		try {
			executor.execute(task);
		} catch (RejectedExecutionException ex) {
			failedCount++;
			//System.out.print("!");
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
		return this.failedCount;
	}
}
