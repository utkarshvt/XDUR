package stm.transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WriteSet {
	public ConcurrentHashMap<String, AbstractObject> writeset;

	public WriteSet() {
		writeset = new ConcurrentHashMap<String, AbstractObject>();
	}
	
	public AbstractObject getobject(String objId) {
		return writeset.get(objId);
	}
	
	public void addToWriteSet(String objId, AbstractObject object) {
		writeset.put(objId, object);
	}
	
	public long getVersion(int objIndex) {
		return writeset.get(objIndex).getVersion();
	}
	
	public Map<String, AbstractObject> getWriteSet() {
		return writeset;
	}

	public void remove(String objId)
	{
		writeset.remove(objId);
	}
	

}
