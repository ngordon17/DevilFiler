package dblockcache;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import virtualdisk.VirtualDisk;

import common.Constants;

public abstract class AbstractDBufferCache {
	
	protected int _cacheSize;
	protected LinkedList<DBuffer> _buffers;
	protected VirtualDisk _virtualDisk;
	
	/*
	 * Constructor: allocates a cacheSize number of cache blocks, each
	 * containing BLOCK-size bytes data, in memory
	 */
	public AbstractDBufferCache(String volName, boolean format, int cacheSize) {
		_cacheSize = cacheSize * Constants.BLOCK_SIZE;
		_buffers = new LinkedList<DBuffer>();
		_virtualDisk = null;
		try {
			_virtualDisk = VirtualDisk.getInstance(volName, format);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	/*
	 * Get buffer for block specified by blockID. The buffer is "held" until the
	 * caller releases it. A "held" buffer cannot be evicted: its block ID
	 * cannot change.
	 */
	public abstract DBuffer getBlock(int blockID);

	/* Release the buffer so that others waiting on it can use it */
	public abstract void releaseBlock(DBuffer buf);
	
	/*
	 * sync() writes back all dirty blocks to the volume and wait for completion.
	 * The sync() method should maintain clean block copies in DBufferCache.
	 */
	public abstract void sync();
}