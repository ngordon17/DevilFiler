/**********************************************
 * Please DO NOT MODIFY the format of this file
 **********************************************/

/*************************
 * Team Info & Time spent
 *************************/

	Name1: Nick Gordon 	// Edit this accordingly
	NetId1: njg10	 	// Edit
	Time spent: 15 hours 	// Edit 

	Name2: Tori Reynolds 	// Edit this accordingly
	NetId2: vmr4	 	// Edit
	Time spent: 15 hours 	// Edit 

/******************
 * Files to submit
 ******************/

	lab4.jar // An executable jar including all the source files and test cases.
	README	// This file filled with the lab implementation details
        DeFiler.log   // (optional) auto-generated log on execution of jar file

/************************
 * Implementation details
 *************************/

/* 
 * This section should contain the implementation details and a overview of the
 * results. You are required to provide a good README document along with the
 * implementation details. In particular, you can pseudocode to describe your
 * implementation details where necessary. However that does not mean to
 * copy/paste your Java code. Rather, provide clear and concise text/pseudocode
 * describing the primary algorithms (for e.g., scheduling choices you used)
 * and any special data structures in your implementation. We expect the design
 * and implementation details to be 3-4 pages. A plain textfile is encouraged.
 * However, a pdf is acceptable. No other forms are permitted.
 *
 * In case of lab is limited in some functionality, you should provide the
 * details to maximize your partial credit.  
 * */

As per the lab pdf, Defiler has "three layers of file system functionality," all
of which are implemented as singletons: 

		if (instance == null)
			instantiate(volname, format)
		else if (format)
			instance.formatStore()
		return _instance

1) DFS controls the high-level logic of the file system; it manages reading from, 
writing to, creating, deleting, and listing files. DFS contains an array of inodes
and an array of used blocks; this allows us to access the inodes directly and to 
easily return the disk blocks which are available for write requests. 

Integration of this code with our LockState class ensures that access requests don't 
conflict, i.e. that only one thread is reading from/writing to a file at a time, 
helping us avoid multithreading errors. 

		acquireWriteLock(newFile)
		allocateBlocks(new DFileID(dfileID), 0)
		releaseWriteLock(newFile)

2) The DBuffer and DBufferCache handle the "buffering and caching of blocks from 
the VDF." We implemented the LRU caching strategy as required by the lab guidelines.

We utilized a linked list structure for our buffer; to support LRU, the list is sorted
by the recentness of use, with the most recently accessed element at the head of the
list. This implementation allows access to buffers in O(n) time and replacement of an 
element in O(1) time. To ensure that we don't have multithreading errors here, we
synchronized our methods, allowing only 1 thread access at any given time. 

		if (!buf.isBusy() && !buf.checkValid()) {
					//unused
					buf.setBusy(true)
					buf.setBlockID(blockID)
					
					//remove and add to maintain LRU order
					_buffers.remove(buf)
					_buffers.add(buf)
					return buf
		}

 
3)The VirtualDisk is the "lowest layer," a block device. It doesn't require 
synchronization, as requests to access this device are handled sequentially by the logic 
in the overlying DBuffer & DFS layers. 

We tested file formatting, reading, writing, persistence, and multithreading and passed 
all of these. Our DFSTest seems to take a long time sometimes, but this is due to the 
time-consuming nature of the sync method. 

*Professor Chase & Vamsi have confirmed that we can assume BLOCKS_IN_DFILE = 50

/************************
 * Feedback on the lab
 ************************/

/*
 * Any comments/questions/suggestions/experiences that you would help us to
 * improve the lab.
 * */

/************************
 * References
 ************************/

/*
 * List of collaborators involved including any online references/citations.
 * */
Becky DeNardis, Dustin Alin