package tests;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import common.Constants;
import common.DFileID;
import dfs.DFS;

public class DFSTest {
	private static DFS dfs;

	@Before
	public void preTestSetUp() throws Exception {
		dfs = DFS.getInstance(true);
		DFS.format();
	}

	@Test
	public void formatDFS() {
		DFS.format();
		assert(dfs.listAllDFiles().isEmpty());
	}	

	@Test
	public void createFile() {
		List<DFileID> dFilesBeforeAddition = dfs.listAllDFiles();
		DFileID dfid = dfs.createDFile();
		List<DFileID> dFilesAfterAddition = dfs.listAllDFiles();
		
		// file only in list of dfiles after creation
		assertFalse(dFilesBeforeAddition.contains(dfid));
		assertTrue(dFilesAfterAddition.contains(dfid));
		
		// check 1) that file is empty
		// 2) that # dfiles is correct, before & after addition
		assertEquals(0, dfs.sizeDFile(dfid));
		assertEquals(dFilesBeforeAddition.size()+1, dfs.listAllDFiles().size());
		assertEquals(dFilesAfterAddition.size(), dfs.listAllDFiles().size());
	}
	
	@Test
	public void deleteFile() {
		DFileID dfid = dfs.createDFile();
		List<DFileID> dFilesBeforeDeletion = dfs.listAllDFiles();
		dfs.destroyDFile(dfid);
		List<DFileID> dFilesAfterDeletion = dfs.listAllDFiles();
		
		// check that list of dfiles only contains file before deletion
		assertTrue(dFilesBeforeDeletion.contains(dfid));
		assertFalse(dFilesAfterDeletion.contains(dfid));
		
		// check that # dfiles is correct, before & after deletion
		assertEquals(dFilesBeforeDeletion.size()-1, dfs.listAllDFiles().size());
		assertEquals(dFilesAfterDeletion.size(), dfs.listAllDFiles().size());
		assertTrue(dFilesBeforeDeletion.containsAll(dFilesAfterDeletion));
	}
	
	@Test 
	public void writeAndReadData() {
		DFileID dfid = dfs.createDFile();	
		assertEquals(0, dfs.sizeDFile(dfid));
		
		int sizeData = 64;
		byte[] dataToWrite = new byte[sizeData];
		
		for (int i = 0; i < sizeData; i++) {
			dataToWrite[i] = 't';
		}
		
		// check that write was successful 
		dfs.write(dfid, dataToWrite, 0, sizeData);
		assertEquals(64, dfs.sizeDFile(dfid));
		
		byte[] dataToRead = new byte[sizeData];
		dfs.read(dfid, dataToRead, 0, sizeData);

		// check that same data was written to & read from file
		assertEquals(dataToRead[63],'t');
		assertEquals(64, dfs.sizeDFile(dfid));
		assertTrue(Arrays.equals(dataToWrite, dataToRead));
	}

	@Test
	public void testWriteOverBlockBoundary() {
		DFileID dfid = dfs.createDFile();
		byte[] input = new byte[2 * Constants.BLOCK_SIZE];
		for (int i = 0; i < 2 * Constants.BLOCK_SIZE; i++)
			input[i] = (byte) i;
		dfs.write(dfid, input, 0, 2 * Constants.BLOCK_SIZE);

		byte[] output = new byte[2 * Constants.BLOCK_SIZE];
		dfs.read(dfid, output, 0, 2 * Constants.BLOCK_SIZE);
		assertArrayEquals(input, output);
	}

    @Test
    public void testWriteTwoFiles() {
		DFileID file1 = dfs.createDFile();
		DFileID file2 = dfs.createDFile();
        assert(file1.getDFileID() > file2.getDFileID());
    }

	@Test
	public void testDestroyFile() {
		DFileID dfid = dfs.createDFile();
		int numfiles = dfs.listAllDFiles().size();
		dfs.destroyDFile(dfid);
		assertEquals(numfiles-1, dfs.listAllDFiles().size());
	}

    @Test
    public void testDestroyFileReleasesInode() {
		DFileID file1 = dfs.createDFile();
		dfs.destroyDFile(file1);
        DFileID file2 = dfs.createDFile();
        assertEquals(file1.getDFileID(), file2.getDFileID());
    }
    
    @Test
	public void createMultipleFiles(){
		DFS.format();
		for(int i = 0; i < Constants.MAX_DFILES; i++){
			assertNotNull(dfs.createDFile());
		}
		assertNull(dfs.createDFile());
		assertEquals(Constants.MAX_DFILES, dfs.listAllDFiles().size());

		Random r = new Random(12);
		int remainingBlocks = Constants.NUM_OF_BLOCKS - (Constants.INODE_SIZE * Constants.MAX_DFILES) / Constants.BLOCK_SIZE;

		HashMap<Integer, byte[]> buffers = new HashMap<Integer,byte[]>();

		for(DFileID file : dfs.listAllDFiles()){
			byte[] writeBuffer = new byte[Math.abs(r.nextInt()) % (Constants.MAX_FILE_SIZE)];
			remainingBlocks -= (int) Math.ceil((double)writeBuffer.length / Constants.BLOCK_SIZE);
			for(int i = 0; i < writeBuffer.length; i++){
				writeBuffer[i] = (byte) (i*i+file.getDFileID());
			}
			int count = dfs.write(file, writeBuffer, 0, writeBuffer.length);
			if(remainingBlocks >= 0){
				assertEquals(writeBuffer.length, count);
				assertEquals(writeBuffer.length, dfs.sizeDFile(file));
				buffers.put(file.getDFileID(), writeBuffer);
				System.out.println("Wrote " + writeBuffer.length + " bytes to file " + file.getDFileID() + " with remaining blocks: " + remainingBlocks);
			}
			else{
				assertEquals(-1, count);
				System.out.println("Failed to write to file " + file.getDFileID() + " remaining blocks: " + remainingBlocks);
				break;
			}
		}

		for(DFileID file : dfs.listAllDFiles()){
			if(!buffers.containsKey(file.getDFileID()))
				continue;
			System.out.println("Starting to read file " + file.getDFileID());
			byte[] writeBuffer = buffers.get(file.getDFileID());
			byte[] readBuffer = new byte[writeBuffer.length];
			assertEquals(writeBuffer.length, dfs.read(file, readBuffer, 0, writeBuffer.length));
			assertTrue(Arrays.equals(writeBuffer, readBuffer));
			System.out.println("Read file " + file.getDFileID() + " successfully");
		}
	}
}
