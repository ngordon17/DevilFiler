package dfs;

public class LockState {
	public boolean isShared;
	public int numReaders;

	// provide safe multithreading access to resource
	public LockState(boolean shared, int reader) {
		isShared = shared;
		numReaders = reader;
	}
}