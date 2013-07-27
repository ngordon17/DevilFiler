package common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class INode {
	private int _index;
	private List<Integer> _blockList = new ArrayList<Integer>();
	private int _size;
	private boolean _isUsed;	
	private final ReentrantReadWriteLock _readWriteLock = new ReentrantReadWriteLock();
	public final Lock read = _readWriteLock.readLock();
	public final Lock write = _readWriteLock.writeLock();
	
	public INode(int index) {
		_index = index;
		_blockList = new ArrayList<Integer>();
		_size = 0;
		_isUsed = false;
	}
	
	public int getIndex() {
		return _index;
	}
	
	public boolean addBlock(int block) {
		if (_blockList.size() < Constants.BLOCKS_IN_DFILE) {
			_blockList.add(block);
			return true;
		}
		return false;
	}
	
	public List<Integer> getBlockList() {
		return _blockList;
	}
	 
    public int getSize() {
        return _size;
    }
    
    public void setSize(int size) {
        _size = size;
    }
    
    public boolean isUsed() {
        return _isUsed;
    }
    
    public void setUsed(boolean used) {
        _isUsed = used;
    }
	
	public void clearContent() {
		_blockList.clear();
		_size = 0;
	}
	
	public void initialize(byte[] buf, int offset, int length) {
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(buf, offset, length));
		clearContent();
		try {
			_isUsed = input.readBoolean();
			if (!_isUsed) {
				return;
			}
			int blockListSize = input.readInt();
			_size = input.readInt();
			for (int i = 0; i < blockListSize; i++) {
				_blockList.add(input.readInt());
			}
			input.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] getMetadata() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(bout);
		try {
			output.writeBoolean(_isUsed);
			output.writeInt(_blockList.size());
			output.writeInt(_size);
			for (int blockID: _blockList) {
				output.writeInt(blockID);
			}
			output.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return bout.toByteArray();
	}
	
	
}
