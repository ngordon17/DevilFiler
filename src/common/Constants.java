package common;
/*
 * This class contains the global constants used in DFS
 */

public class Constants {

	public static final int NUM_OF_BLOCKS = 262144 / 8; // 2^14
	public static final int BLOCK_SIZE = 1024; // 1kB
	public static final int INODE_SIZE = 256;
	public static final int NUM_OF_CACHE_BLOCKS = 65536 / 8; //128? - CacheSize
	public static final int BLOCKS_IN_DFILE = 50;
	public static final int MAX_FILE_SIZE = BLOCKS_IN_DFILE*BLOCK_SIZE; //50 blocks
	public static final int MAX_DFILES = 512;
	
	/* DStore Operation types */
	public enum DiskOperationType {
		READ, WRITE
	};

	/* Virtual disk file/store name */
	public static final String vdiskName = "DSTORE.dat";
}