package virtualdisk;

import java.io.FileNotFoundException;
import java.io.IOException;

import common.Constants;
import common.Constants.DiskOperationType;

import dblockcache.DBuffer;

public class VirtualDisk extends AbstractVirtualDisk {
	private static VirtualDisk _instance;

	private VirtualDisk(String volName, boolean format) throws FileNotFoundException,IOException {
		super(volName, format);
	}

	private VirtualDisk(boolean format) throws FileNotFoundException,IOException {
		super(format);
	}

	private VirtualDisk() throws FileNotFoundException, IOException {
		super();
	}
	
	public static VirtualDisk getInstance(String volName, boolean format) throws FileNotFoundException, IOException {
		if (_instance == null) {
			_instance = new VirtualDisk(volName, format);	
			return _instance;
		}
		if (format) {
			formatStore();
		}
		return _instance;
	}
	
	public static VirtualDisk getInstance(boolean format) throws FileNotFoundException, IOException {
		return getInstance(Constants.vdiskName, format);
	}
	
	public static VirtualDisk getInstance() throws FileNotFoundException, IOException {
		return getInstance(Constants.vdiskName, false);
	}
	

	@Override
	public void startRequest(DBuffer buf, DiskOperationType operation) throws IllegalArgumentException, IOException {
		synchronized(this) {
			_requestQueue.add(new VirtualDiskRequest(buf, operation));
			this.notify();
		}
	}

	@Override
	public void run() {
		while(true) {
			VirtualDiskRequest request = null;
			synchronized(this) {
				while (_requestQueue.isEmpty()) {
					try {
						this.wait();
					}
					catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				request = _requestQueue.poll();
			}
			if (request != null) {
				commitRequest(request);
			}	
		}	
	}
	
	private void commitRequest(VirtualDiskRequest request) {
		try {
			switch(request.operation) {
			case READ:
				readBlock(request.buf);
				break;
			case WRITE:
				writeBlock(request.buf);
				break;
			}
			request.buf.ioComplete();	
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}


}
