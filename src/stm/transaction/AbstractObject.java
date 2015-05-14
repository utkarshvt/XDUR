package stm.transaction;

public abstract class AbstractObject {
	  public static long MAX_VERSION_NUMBER = 9223372036854775807L;
	  
	  public long tc_version;
	  
	  public abstract String getId();
	  
	  public AbstractObject() {
		  tc_version = -1;
	  }
	  
	  public long getVersion() {
		  return tc_version;
	  }
	  
	  public void setVersion(Long nextversion) {
		  tc_version = nextversion;
	  }

	  public void incrementVersion() {
		  tc_version += 1;
	  }
	  	  
}
