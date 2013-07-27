package dfs;

import java.util.ArrayList;
import java.util.List;

import common.Constants;
import common.DFileID;
import common.INode;
import dblockcache.DBuffer;

public class DFS extends AbstractDFS {
	private static DFS _instance;

	private DFS(String volName, boolean format) {
		super(volName, format);
	}

	private DFS(boolean format) {
		super(format);
	}

	private DFS() {
		super();
	}

	public static DFS getInstance(String volName, boolean format) {
		if (_instance == null) {
			_instance = new DFS(volName, format);
			return _instance;
		}
		if (format) {
			format();
		}
		return _instance;
	}

	public static DFS getInstance(boolean format) {
		return getInstance(Constants.vdiskName, format);
	}

	public static DFS getInstance() {
		return getInstance(Constants.vdiskName, false);
	}

	@Override
	public DFileID createDFile() {
		for (INode i: _inodes) {
			i.write.lock();
			if (!i.isUsed()) {
				i.clearContent();
				i.setUsed(true);
				writeToINode(i);
				i.write.unlock();
				return new DFileID(i.getIndex());
			}
			i.write.unlock();
		}
		//no inodes available
		//System.out.println("Error: Cannot create file - max number of dfiles already created");
		System.err.println("Error: Cannot create file - max number of dfiles already created");
		return null;
	}

	@Override
	public void destroyDFile(DFileID dFID) {
		INode i = _inodes[dFID.getDFileID()];
		i.write.lock();

		synchronized(this) {
			for (int blockID : i.getBlockList()) {
				_usedBlocks[blockID] = false;
			}
		}
		
		i.clearContent();
		i.setUsed(false);
		writeToINode(i);
		i.write.unlock();

	}

	@Override
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
		INode i = _inodes[dFID.getDFileID()];
		i.read.lock();
		if (!i.isUsed()) {
			i.read.unlock();
			return -1;
		}
		int readSize = Math.min(i.getSize(), count);
		List<Integer> blocks = i.getBlockList();
		for (int k = 0; k < blocks.size(); k++) {
			DBuffer dbuf = _cache.getBlock(blocks.get(k));
			if (dbuf.read(buffer, startOffset + k*Constants.BLOCK_SIZE, Math.min(readSize - k*Constants.BLOCK_SIZE, Constants.BLOCK_SIZE)) == -1) {
				_cache.releaseBlock(dbuf);
				i.read.unlock();
				return -1;
			}
			_cache.releaseBlock(dbuf);

		}
		i.read.unlock();
		return readSize;
	}

	@Override
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
		INode node = _inodes[dFID.getDFileID()];
		node.write.lock();
		if (!node.isUsed()) {
			node.write.unlock();
			return -1;
		}

		// clear used blocks 
		synchronized(this) {
			for (int k : node.getBlockList()) {
				_usedBlocks[k] = false;
			}
		}
		node.clearContent();
		int writeSize = Math.min(buffer.length - startOffset, count);
		int numBlocks = writeSize / Constants.BLOCK_SIZE;
		if (writeSize % Constants.BLOCK_SIZE > 0) {
			numBlocks++;
		}

		// find free blocks
		int head = Constants.MAX_DFILES / (Constants.BLOCK_SIZE / Constants.INODE_SIZE);
		synchronized(this) {
			for (int k = 0; k < numBlocks; k++) {
				while (head < _usedBlocks.length && _usedBlocks[head] == true) {
					head++;
				}
				if (head >= _usedBlocks.length) {
					node.write.unlock();
					return -1;
				}
				_usedBlocks[head] = true;
				node.addBlock(head);
			}
		}

		node.setSize(writeSize);
		writeToINode(node);
		List<Integer> blocks = node.getBlockList();

		for (int k = 0; k<blocks.size(); k++) {

			DBuffer dbuf = _cache.getBlock(blocks.get(k));
			if (dbuf.write(buffer, startOffset + k*Constants.BLOCK_SIZE, Math.min(writeSize - k*Constants.BLOCK_SIZE, Constants.BLOCK_SIZE)) == -1) {
				_cache.releaseBlock(dbuf);
				node.write.unlock();
				return -1;
			}
			_cache.releaseBlock(dbuf);
		}
		node.write.unlock();
		return writeSize;
	}

	@Override
	public int sizeDFile(DFileID dFID) {
		INode i = _inodes[dFID.getDFileID()];
		i.read.lock();
		if (i.isUsed()) {
			try{
				return i.getSize();
			}
			finally {
				i.read.unlock();
			}
		}
		i.read.unlock();
		return -1;
	}

	@Override
	public List<DFileID> listAllDFiles() {
		List<DFileID> list = new ArrayList<DFileID>();
		for (INode i: _inodes) {
			i.read.lock();
			if (i.isUsed()) {
				list.add(new DFileID(i.getIndex()));
			}
			i.read.unlock();
		}
		return list;
	}

	@Override
	public void sync() {
		for (INode i: _inodes) writeToINode(i);
		_cache.sync();

	}

}
