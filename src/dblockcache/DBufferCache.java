package dblockcache;

public class DBufferCache extends AbstractDBufferCache {

	private static DBufferCache _instance;
	
	private DBufferCache(String volName, boolean format, int cacheSize) {
		super(volName, format, cacheSize);
	}
	
	public static DBufferCache getInstance(String volName, boolean format, int cacheSize) {
		if (_instance == null) {
			_instance = new DBufferCache(volName, format, cacheSize);
			return _instance;
		}
		return _instance;
	}

	
	public DBuffer getBlock(int blockID) {
		DBuffer block = findBlock(blockID);
		block.acquire();
		if (!block.checkValid()) {
			block.startFetch();
			block.waitValid();
		}
		return block;
	}
	
	public synchronized DBuffer findBlock(int blockID) {
		for (DBuffer block: _buffers) {
			if (block.getBlockID() == blockID) {
				// maintain LRU order
				_buffers.remove(block);
				_buffers.add(block);
				return block;
			}
		}
		return allocateBlock(blockID);
	}
	
	private DBuffer allocateBlock(int blockID) {
		if (_buffers.size() > _cacheSize) {
			evictBlock();
		}
		DBuffer block = new DBuffer(_virtualDisk, blockID);
		_buffers.add(block);
		return block;
	}
	
	private boolean evictBlock() {
		for (DBuffer block : _buffers) {
			if (!block.isBusy()) {
				_buffers.remove(block);
				if (!block.checkClean()) {
					block.startPush();
				}
				return true;
			}
		}
		// all blocks busy
		return false;
	}
	
	@Override
	public synchronized void releaseBlock(DBuffer buf) {
		buf.release();
		this.notifyAll();
	}

	@Override
	public synchronized void sync() {
		boolean[] heldMap = new boolean[_buffers.size()];
		for (int i = 0; i < _buffers.size(); i++) {
			while (_buffers.get(i).isBusy()) {
				try {
					this.wait();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			_buffers.get(i).setBusy(true);
			heldMap[i] = true;
		}
		for (DBuffer buf: _buffers) {
			if (buf.checkValid() && !buf.checkClean()) {
				buf.evict();
			}
			buf.setBusy(false);
		}
	}
}
