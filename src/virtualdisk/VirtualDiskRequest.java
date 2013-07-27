package virtualdisk;
import common.Constants.DiskOperationType;
import dblockcache.DBuffer;

public class VirtualDiskRequest{

    DBuffer buf;
    DiskOperationType operation;

    public VirtualDiskRequest(DBuffer buf, DiskOperationType operation){
        this.buf = buf;
        this.operation = operation;
    }
}