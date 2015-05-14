package stm.transaction;

import java.util.Map;


public class TransactionContext {
	private int Tid;						/* Denotes the transaction number, added for parallel implementation */
	WriteSet writeset = new WriteSet();
	private ReadSet readset = new ReadSet();
	private byte[] result;

	
	/* Default constructor for deserialization, Tid = 0 */
	public TransactionContext()
	{
		this.Tid = 0;
	}	
	
	public TransactionContext( int Id)
	{
		this.Tid = Id;
	}	
	
	public int getTransactionId()
	{
		return(this.Tid) ;
	} 
	public AbstractObject getLatestUnCommittedCopy(String objId){
		return writeset.getobject(objId);
	}
	
	public void setResult(byte[] result) {
		this.result = result;
	}
	
	public byte[] getResult() {
		return result;
	}
	
	public void addObjectToWriteSet(String objId, AbstractObject object) {
		writeset.addToWriteSet(objId, object);
	}
	
	public void addObjectToReadSet(String objId, AbstractObject object) {
		readset.addToReadSet(objId, object);
	}
	
	public WriteSet getShadowCopySetElement() {
		return writeset;
	}

	public Map<String, AbstractObject> getReadSet() {
		return readset.getReadSet();
	}
	
	public Map<String, AbstractObject> getWriteSet() {
		return writeset.getWriteSet();
	}

	public void readsetremove(String ObjId)
	{
		readset.remove(ObjId);
	}
	        
	public void writesetremove(String ObjId)
        {
                writeset.remove(ObjId);
        }


}
