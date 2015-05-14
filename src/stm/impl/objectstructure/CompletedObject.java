package stm.impl.objectstructure;
import java.util.concurrent.atomic.AtomicInteger;
import stm.transaction.AbstractObject;

public class CompletedObject { 

	AbstractObject currentObject; // version is part of object
	
	/* Fields added fr parallelization */
	AtomicInteger  wait;
	AtomicInteger owner;
	private int readers_array[];
	private int vcts;
	private int MaxSpec;

	public CompletedObject(int MaxSpec)
	{
		wait = new AtomicInteger(0);
		owner = new AtomicInteger(0);
		readers_array = new int[MaxSpec];
		vcts = 0;
		this.MaxSpec = MaxSpec;
	}
	
	public void lock_object(int caller)
	{
		int curr;
		int it = 0;	
		
		while(true)
		{
			it++;
			curr = this.owner.get();
			if((curr == 0) || (caller <= curr))
			{	
				if( this.owner.compareAndSet(curr, caller) == true)
				{
					//System.out.println("Iterations in while = " + it + " Owner = " + caller + " Prev owner = " + curr);
					setReader(caller);
					return;
				}
			
			}
		}
	}
	
	public void unlock_object(int caller)
	{
		if(this.owner.compareAndSet(caller,0))
			return;
		else
			return;				/* Someone else cquired the lock, no change is required */
	}

	public void setReader(int Tid)
	{
		int index = (Tid - 1) % MaxSpec;	/* Tx Ids start from 1 at present */
		readers_array[index] = Tid;	
	}	
	
	public void clearReader(int Tid)
	{
		int index = (Tid - 1) % MaxSpec;	/* Tx Ids start from 1 at present */
		readers_array[index] = 0;	
	}	
	
	public int[] getReaderArray()
	{
		return (this.readers_array);
	}
	
	public int getOwner()
	{
		return(this.owner.get());
	}
	public void setCurrentObject(AbstractObject object) {
		currentObject = object;
	}
	
	public AbstractObject getCurrentObject() {
		return currentObject;
	}

	public void clearOwner()
	{
		this.owner.set(0);
	}
	
	public boolean compareAndSetOwner(int prev,int  curr)
        {
                return(owner.compareAndSet(prev,curr));
        }

}
