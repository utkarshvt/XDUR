package stm.benchmark.vacation;

import stm.transaction.AbstractObject;

public class ReservationInfo extends AbstractObject implements Comparable<ReservationInfo>
{
	public int id;
	public int type;
	public int price;
	boolean isNull = false;

	public ReservationInfo()
	{
	}
	
	public ReservationInfo(int type, int id, int price)
	{
		this.type = type;
		this.id = id;
		this.price = price;
	}

	public static int reservation_info_compare(ReservationInfo aPtr,
			ReservationInfo bPtr)
	{
		int typeDiff;

		typeDiff = aPtr.type - bPtr.type;

		return ((typeDiff != 0) ? (typeDiff) : (aPtr.id - bPtr.id));
	}

	@Override
	public final int compareTo(ReservationInfo o)
	{
		int typeDiff = type - o.type;

		return ((typeDiff != 0) ? (typeDiff) : (id - o.id));
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}
}
