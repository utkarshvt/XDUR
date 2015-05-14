package stm.impl.objectstructure;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import stm.transaction.AbstractObject;

public class CommittedObjects {
	public static long MAX_VERSION_NUMBER = 9223372036854775807L;
	
	public Integer timestamp;
	private ConcurrentSkipListMap<Integer, AbstractObject> committedList;

	public CommittedObjects(AbstractObject obj) {
		timestamp = new Integer(0);
		committedList = new ConcurrentSkipListMap<Integer, AbstractObject>();
		// Enter the first version of object with timestamp
		committedList.put(timestamp, obj);
	}
	
	public void addCommittedObject(AbstractObject obj, int ts) {
		timestamp = (Integer) ts;
		committedList.put(timestamp, obj);
		//System.out.println("List length = " + committedList.size());
	}
	
	public AbstractObject getLatestObject(int transactionSnapshot) {
		// Get the entry belonging to the latest entry lower than or equal to 
		// given timestamp
		Entry<Integer, AbstractObject> entry = committedList.floorEntry((Integer)transactionSnapshot);
		return entry.getValue();
	}
	
	public AbstractObject getLatestObject() {
		// Get the entry belonging to the latest entry lower than or equal to 
		// given timestamp
		Entry<Integer, AbstractObject> entry = committedList.lastEntry();
		//System.out.println("TimeStamp Committed = " + entry.getKey());
		return entry.getValue();
	}
}
