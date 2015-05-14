package stm.impl.objectstructure;
import stm.transaction.AbstractObject;

public class SharedObject {
	
	private CommittedObjects committedObjects; 
	private CompletedObject completedObject; 
	
	public SharedObject(AbstractObject object, int MaxSpec) {
		committedObjects = new CommittedObjects(object);
		completedObject = new CompletedObject( MaxSpec);	
	}
	

	public AbstractObject getLatestCommittedObject(int transactionSnapshot) {
		return committedObjects.getLatestObject(transactionSnapshot);
	}
	
	public AbstractObject getLatestCommittedObject() {
		return committedObjects.getLatestObject();
	}
	
	public AbstractObject getLatestCompletedObject() {
		return completedObject.getCurrentObject();
	}
	
	public void updateCompletedObject(AbstractObject object) {
		completedObject.setCurrentObject(object);
	}
	
	public void updateCommittedObject(AbstractObject object, int timeStamp) {
		committedObjects.addCommittedObject(object, timeStamp);
	}

	/* Additional functions to enfrce parallelism */
	 public void lock_object(int caller)
	 {
		completedObject.lock_object(caller);
	 }
	 
	 public void unlock_object(int caller)
	 {
		completedObject.unlock_object(caller);
	 }

	public void setReader(int Tid)
	{
		completedObject.setReader(Tid);
	}
	public void clearReader(int Tid)
	{
		completedObject.clearReader(Tid);
	}
 	public int[] getReaderArray()
        {
                return(completedObject.getReaderArray());
        }
	public int getOwner()
	{
		return(completedObject.getOwner());
	}
 	public void clearOwner()
        {
                completedObject.clearOwner();
        }
	public boolean compareAndSetOwner(int prev, int curr)
        {
               return( completedObject.compareAndSetOwner(prev, curr));
        }

}
