package stm.transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReadSet {
    public ConcurrentHashMap<String, AbstractObject> readset;
    
    public ReadSet() {
    	readset = new ConcurrentHashMap<String, AbstractObject>();
    }
    
    public void addToReadSet(String objId, AbstractObject object) {
    	readset.put(objId, object);
    }
    
    public Map<String, AbstractObject> getReadSet() {
    	return readset;
    }


    public void remove(String objId)
    {
       readset.remove(objId);
    }



}
