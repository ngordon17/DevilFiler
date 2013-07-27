package dfs;

import java.util.Arrays;
import java.util.List;

import common.Constants;
import common.DFileID;
import common.INode;
import dblockcache.DBuffer;
import dblockcache.DBufferCache;

public abstract class AbstractDFS {

	protected boolean _format;
	protected String _volName;
	protected static DBufferCache _cache;
	protected static INode[] _inodes;
	protected static boolean[] _usedBlocks;

	/* 
	 * @volName: Explicitly overwrite volume name
	 * @format: If format is true, the system should scan the underlying disk contents and reinitialize the volume.
	 */

	public AbstractDFS(String volName, boolean format) {
		_volName = volName;
		_format = format;
		_cache = DBufferCache.getInstance(_volName, format, Constants.NUM_OF_CACHE_BLOCKS);
		_inodes = new INode[Constants.MAX_DFILES];
		_usedBlocks = new boolean[Constants.NUM_OF_BLOCKS];

		for (int i = 0; i < _inodes.length; i++) {
			_inodes[i] = new INode(i);
		}

		if (format) format();
		else init();
	}

	public AbstractDFS(boolean format) {
		this(Constants.vdiskName,format);
	}

	public AbstractDFS() {
		this(Constants.vdiskName,false);
	}

	private void init() {
		for (int i = 0; i < _inodes.length; i++) {
			int blockID = i / (Constants.BLOCK_SIZE / Constants.INODE_SIZE);
			int offset = i % (Constants.BLOCK_SIZE / Constants.INODE_SIZE);
			byte[] buffer = new byte[Constants.BLOCK_SIZE];

			DBuffer dbuf = _cache.getBlock(blockID);
			dbuf.read(buffer, 0, Constants.BLOCK_SIZE);
			_cache.releaseBlock(dbuf);
			_inodes[i].initialize(buffer, offset*Constants.INODE_SIZE, Constants.INODE_SIZE);

			if (_inodes[i].isUsed()) {
				int actualNumBlocks = _inodes[i].getBlockList().size();
				int predictedNumBlocks = (int) Math.ceil((double) _inodes[i].getSize() / Constants.BLOCK_SIZE);

				// ensure that the # blocks in your list is correct
				if (actualNumBlocks == predictedNumBlocks) {
					for (int dfid: _inodes[i].getBlockList()) {
						if(_usedBlocks[dfid]) System.err.println("Block " + dfid + " is in use by multiple INodes");

						// block # is either - or greater than allowed # blocks
						// signifies corruption of block list
						if (dfid < 0 || dfid >= Constants.NUM_OF_BLOCKS) {
							try {
								_inodes[i].clearContent(); // delete the block data
								System.err.println("INode " + i + " has a # blocks outside of the acceptable range");
							} catch(Exception e) {
								System.err.println("Error in clearing dfiles");
							}
							break;
						}
						// we can initialize & use block
						else _usedBlocks[dfid] = true;		
					}
				}
				else {
					// File size doesn't compute, given operations
					try {
						_inodes[i].clearContent(); // delete the block data
						System.err.println("File " + i + " doesn't have the correct size");
					} catch(Exception e) {
						System.err.println("Error in clearing dfiles");
					}	
				}
			}
		}
	}

	protected static void writeToINode(INode node) {
		int blockID = node.getIndex() / (Constants.BLOCK_SIZE/Constants.INODE_SIZE);
		int inodeOffset = node.getIndex() % (Constants.BLOCK_SIZE/Constants.INODE_SIZE);
		byte[] dataToWrite = new byte[Constants.BLOCK_SIZE];
		byte[] fileMetadata = node.getMetadata();

		DBuffer buffer = _cache.getBlock(blockID);
		buffer.read(dataToWrite, 0, Constants.BLOCK_SIZE);
		for (int k = 0 ; k < fileMetadata.length; k++) dataToWrite[inodeOffset*Constants.INODE_SIZE + k] = fileMetadata[k];

		buffer.write(dataToWrite, 0, Constants.BLOCK_SIZE);
		_cache.releaseBlock(buffer);
	}

	private static void inodeIsUsed() {
		for (int i = 0; i < (Constants.MAX_DFILES * Constants.INODE_SIZE) / Constants.BLOCK_SIZE; i++){
			_usedBlocks[i] = true;
		}
	}

	public synchronized static boolean format() {
		for (INode i: _inodes) {
			i.write.lock();
		}
		Arrays.fill(_usedBlocks, false);
		inodeIsUsed();

		for (INode i: _inodes) {
			i.clearContent();
			i.setUsed(false);
			writeToINode(i);              
			i.write.unlock();
		}
		return true;
	}

	/* creates a new DFile and returns the DFileID, which is useful to uniquely identify the DFile*/
	public abstract DFileID createDFile();

	/* destroys the file specified by the DFileID */
	public abstract void destroyDFile(DFileID dFID);

	/*
	 * reads the file dfile named by DFileID into the buffer starting from the
	 * buffer offset startOffset; at most count bytes are transferred
	 */
	public abstract int read(DFileID dFID, byte[] buffer, int startOffset, int count);

	/*
	 * writes to the file specified by DFileID from the buffer starting from the
	 * buffer offset startOffset; at most count bytes are transferred
	 */
	public abstract int write(DFileID dFID, byte[] buffer, int startOffset, int count);

	/* returns the size in bytes of the file indicated by DFileID. */
	public abstract int sizeDFile(DFileID dFID);

	/* 
	 * List all the existing DFileIDs in the volume
	 */
	public abstract List<DFileID> listAllDFiles();

	/* Write back all dirty blocks to the volume, and wait for completion. */
	public abstract void sync();
}