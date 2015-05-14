package stm.benchmark.vacation;

import stm.transaction.AbstractObject;

public class Reservation extends AbstractObject implements Comparable<Reservation>
{
	public String objId;
	public int id;
	public int numUsed;
	public int numFree;
	public int numTotal;
	public int price;
	boolean isNull = false;

	public Reservation()
	{
	}
	
	public Reservation(String objId, int id, int numTotal, int price)
	{
		this.objId = objId;
		this.id = id;
		this.numUsed = 0;
		this.numFree = numTotal;
		this.numTotal = numTotal;
		this.price = price;
		checkReservation();
	}

	public void checkReservation()
	{
		if (numUsed < 0 || numFree < 0 || numTotal < 0
				|| ((numUsed + numFree) != numTotal) || price < 0)
		{
			// rollback();
		}

	}

	boolean reservation_addToTotal(int num)
	{
		if (numFree + num < 0)
		{
			return false;
		}

		numFree += num;
		numTotal += num;
		checkReservation();
		return true;
	}

	public boolean reservation_make()
	{
		if (numFree < 1)
		{
			return false;
		}
		numUsed++;
		numFree--;
		checkReservation();
		return true;
	}

	boolean reservation_cancel()
	{
		if (numUsed < 1)
		{
			return false;
		}
		numUsed--;
		numFree++;
		checkReservation();
		return true;
	}

	boolean reservation_updatePrice(int newPrice)
	{
		if (newPrice < 0)
		{
			return false;
		}

		this.price = newPrice;
		checkReservation();
		return true;
	}

	int reservation_compare(Reservation aPtr, Reservation bPtr)
	{
		return aPtr.id - bPtr.id;
	}

	int reservation_hash()
	{
		return id;
	}

	@Override
	public final int compareTo(Reservation o)
	{
		return id - o.id;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return objId;
	}
	
}