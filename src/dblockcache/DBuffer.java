package dblockcache;

import java.io.IOException;

import virtualdisk.VirtualDisk;

import common.Constants;
import common.Constants.DiskOperationType;

public class DBuffer extends AbstractDBuffer {

	public DBuffer(VirtualDisk virtualDisk, int blockID) {
		super(virtualDisk, blockID);
	}
	
	@Override
	public void startFetch() {
		try {
			_virtualDisk.startRequest(this, DiskOperationType.READ);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startPush() {
		try {
			_virtualDisk.startRequest(this, DiskOperationType.WRITE);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean checkValid() {
		return _isValid;
	}
	
	public void setValid(boolean valid) {
		_isValid = valid;
	}

	@Override
	public synchronized boolean waitValid() {
		while(!_isValid) {
			try {
				this.wait();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				return _isValid;
			}
		}
		return _isValid;
	}

	@Override
	public boolean checkClean() {
		return _isClean;
	}

	@Override
	public synchronized boolean waitClean() {
		while(!_isClean) {
			try {
				this.wait();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				return _isClean;
			}
		}
		return _isClean;
	}

	@Override
	public boolean isBusy() {
		return _isBusy;
	}
	
	public void setBusy(boolean busy) {
		_isBusy = busy;
	}

	@Override
	public synchronized int read(byte[] buffer, int startOffset, int count) {
		if (startOffset < 0 || startOffset + count > buffer.length || count > Constants.BLOCK_SIZE) {
			return -1; // ERROR
		}
		// wait for valid data and fetch data from disk.
		if (!_isValid) {
			startFetch();
			waitValid();
		}
		for (int i = 0; i < count; i++) {
			buffer[startOffset + i] = _buffer[i];
		}
		return count;
	}

	@Override
	public synchronized int write(byte[] buffer, int startOffset, int count) {
		if (startOffset < 0 || startOffset + count > buffer.length || count > Constants.BLOCK_SIZE) {
			return -1;
		}
		for (int i = 0; i < count; i++) {
			_buffer[i] = buffer[startOffset + i];
		}
		for (int i = count; i < _buffer.length; i++) {
			_buffer[i] = 0; //erase rest of buffer
		}
		_isValid = true;
		_isClean = false;
		return count;
	}

	@Override
	public synchronized void ioComplete() {
		_isClean = true;
		_isValid = true;
		notifyAll();
	}

	@Override
	public int getBlockID() {
		return _blockID;
	}
	
	public void setBlockID(int blockID) {
		_blockID = blockID;
	}

	@Override
	public byte[] getBuffer() {
		return _buffer;
	}
	
	public synchronized void evict() {
		if (!_isClean) {
			startPush();
			waitClean();
		}
	}
	
	public synchronized void acquire() {
		while(_isBusy) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		_isBusy = true;
	}
	
	public synchronized void release() {
		_isBusy = false;
		this.notify();
	}
}
