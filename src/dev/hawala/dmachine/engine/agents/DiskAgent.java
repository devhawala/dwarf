/*
Copyright (c) 2017, Dr. Hans-Walter Latz
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * The name of the author may not be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package dev.hawala.dmachine.engine.agents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PilotDefs;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;

/**
 * Agent for the harddisk of a Dwarf machine,
 * <p>
 * Only one single disk is currently supported, but this could be easily
 * extended if necessary, as the disk itself is handled by an internal
 * class.
 * </p>
 * <p>
 * This implementation of a simulated harddisk works with a fully cached
 * disk content, meaning that the complete disk is loaded in (Java) memory
 * at initialization and all changes (writes) to the disk are buffered, so
 * no I/O operations to the external medium (local or remote disk of the
 * computer where Java is running) occur while the mesa engine is running
 * and reads/writes to the disk.
 * <br>
 * All changes to the simulated disk are written back to a compressed delta
 * file when the agent is shut down. This delta (if present) is overlaid to
 * the content of the original disk content when the {@code DiskAgent} is
 * initialized. Before writing a new delta, the old delta file is renamed using
 * its creation timestamp, so fallback deltas are preserved. The number of old
 * deltas to keep when saving is configured when registering the disk with
 * the {@code DiskAgent}.  
 * </p>
 * <p>
 * The agent works synchronously, meaning that the disk i/o occurs during the
 * {@code call()} (resp.the CALLAGENT instruction). The interrupt signaling the
 * end of operation is therefore requested at the end of the {@code call()}
 * method call.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class DiskAgent extends Agent {
	
	/**
	 * Exception thrown if a delta file is invalid, the cached disk has
	 * possibly been partially altered by the delta file, so the disk
	 * is probably unusable.  
	 */
	private static class DeltaCorrupted extends Exception {
		private static final long serialVersionUID = 179736267648679038L;
	}
	
	/**
	 * Implementation of a single simulated hard disk providing the basic
	 * operation of the {@code DiskAgent} for disks (read, write). Thie class
	 * also provides the full caching of the disk and saving as delta file. 
	 */
	public static class DiskFile {
		
		// management info in delta files
		private final short DELTA_SIGNATURE = (short)0x65CA;
		private final short DELTA_VERSION1 = 1;
		
		// callback for reading a word in forward or revesed byte order.
		@FunctionalInterface
		private interface Reader {
			short get() throws IOException;
		}
		
		private final File f; // the "full" file for the disk 
		private final int cylinders; // the number of simulated cylinders (computed from the file size)
		private final boolean externalByteSwapped; // true if pilot physical disk seal word is present AND has swapped nibbles
		
		/** is the disk readonly? */
		public final boolean readonly;
		
		// deltas to preserve when writing a new (current) delta
		private final int deltasToKeep;
		
		// the cached disk content
		private final short[] content;
		
		// the delta information: which pages have been modified
		// (initial by loading the (last) delta and by writes by the running mesa engine
		private final short[] chunks; // 1 chunk => modified bits for 16 sectors (pages)
		
		// a chunk has 16 sectors, these are the bits in a chunk for each of these pages
		private static final short[] CHUNK_MODIFIED_BITS = {
			(short)0x8000, (short)0x4000, (short)0x2000, (short)0x1000,
			(short)0x0800, (short)0x0400, (short)0x0200, (short)0x0100,
			(short)0x0080, (short)0x0040, (short)0x0020, (short)0x0010,
			(short)0x0008, (short)0x0004, (short)0x0002, (short)0x0001
		};
		
		// has the disk been modified? 
		private boolean changed = false;
		
		// local logging function
		private void logf(String template, Object... args) {
			if (Config.AGENTS_LOG_DISK) {
				System.out.printf("DiskFile[" + f.getName() + "]: " + template, args);
			}
		}
		
		/**
		 * Constructor.
		 * 
		 * @param f the file containing the raw disk content,
		 *   the filename will be used to locate the current delta.
		 * @param readonly is the disk to be readonly?
		 * @param deltasToKeep the number of old delta files to preserve after saving the new delta.
		 * @throws DeltaCorrupted if the delta file is corrupted (i.e. not a valid delta file)
		 * @throws IOException in case of access or plausibility problems with the disk file
		 */
		public DiskFile(File f, boolean readonly, int deltasToKeep) throws DeltaCorrupted, IOException {
			int wordLength = ((int)(f.length() & 0xFFFFFFFF) + 1) / 2;
			
			this.f = f;
			this.cylinders = wordLength / (DISK_HEADS * DISK_SECTORS * PrincOpsDefs.WORDS_PER_PAGE);
			this.content = new short[wordLength];
			this.chunks = new short[this.cylinders * DISK_HEADS * DISK_SECTORS];
			for (int i = 0; i < this.chunks.length; i++) { this.chunks[i] = 0; }
			
			File dir = f.getParentFile();
			if (dir == null || !dir.canWrite()) {
				this.readonly = true; // as we won't be able to save a delta file
			} else {
				this.readonly = readonly;
			}
			
			this.deltasToKeep = deltasToKeep;
			
			logf("loading base file - byteLength = %d => wordLength = %d , cyls = %d , heads = %d , sects = %d\n",
					f.length(), wordLength, this.cylinders, DISK_HEADS, DISK_SECTORS);
			
			// test the byte order we must use when reading the file content
			byte[] physicalSeal = new byte[2]; // 1st 2 bytes must be 121212B = 0xA28A
			try (FileInputStream fis = new FileInputStream(f)) {
				fis.read(physicalSeal);
				this.externalByteSwapped = (physicalSeal[0] == (byte)0x8A && physicalSeal[1] == (byte)0xA2);
			}
			
			// load the whole file content into the cache
			try (FileInputStream fis = new FileInputStream(f)) {
				int offset = 0;
				for(int i = 0; i < cylinders; i++) {
					if (this.externalByteSwapped) {
						offset = this.loadCylinderSwapped(fis, offset);
					} else  {
						offset = this.loadCylinder(fis, offset);
					}
				}
			}
			
			logf("done loading base file\n");
			
			// load delta file only replace pages from there
			String deltaname = f.getPath() + ".zdelta";
			File delta = new File(deltaname);
			if (!delta.exists()) { return; }
			logf("loading delta from %s\n", deltaname);
			try (FileInputStream fis = new FileInputStream(delta); InflaterInputStream iis = new InflaterInputStream(fis)) {
				short signature = deltaReadShort(iis);
				short version = deltaReadShort(iis);
				if (signature != DELTA_SIGNATURE || version != DELTA_VERSION1) { return; } // ignore non delta file
				
				int chunksRead = 0;
				int pagesRead = 0;
				int chunkNo = deltaReadInt(iis);
				while(chunkNo >= 0) {
					if (chunkNo >= this.chunks.length) { throw new DeltaCorrupted(); }
					
					int chunkBaseOffset = chunkNo * PrincOpsDefs.WORDS_PER_PAGE * 16; // 16 pages in a chunk
					int chunkOffset = 0;
					
					short chunk = deltaReadShort(iis);
					for (int i = 0; i < 16; i++) {
						if ((chunk & CHUNK_MODIFIED_BITS[i]) != 0) {
							deltaReadPage(iis, chunkBaseOffset + chunkOffset);
							pagesRead++;
						}
						chunkOffset += PrincOpsDefs.WORDS_PER_PAGE;
					}
					this.chunks[chunkNo] = chunk;
					chunksRead++;
					
					chunkNo = deltaReadInt(iis);
				}

				int pagesWritten = deltaReadInt(iis);
				if (pagesWritten != pagesRead) { throw new DeltaCorrupted(); }
				int chunksWritten = deltaReadInt(iis);
				if (chunksWritten != chunksRead) { throw new DeltaCorrupted(); }
				logf("-> loaded %d pages in %d chunks\n", pagesRead, chunksRead);
			}
		}
		
		// functionality for loading the raw disk
		
		private final byte[] cylBuffer = new byte[DISK_HEADS * DISK_SECTORS * PrincOpsDefs.BYTES_PER_PAGE];
		
		private int loadCylinder(FileInputStream fis, int wordOffset) throws IOException {
			int bytesRead = fis.read(this.cylBuffer);
			if (bytesRead != this.cylBuffer.length) {
				throw new IOException("short read: got " + bytesRead + " instead of " + this.cylBuffer.length + " bytes");
			}
			int b = 0;
			int rest = DISK_HEADS * DISK_SECTORS * PrincOpsDefs.WORDS_PER_PAGE;
			while(rest > 0) {
				int upper = this.cylBuffer[b++] & 0xFF;
				int lower = this.cylBuffer[b++] & 0xFF;
				short word = (short)(((upper << 8) | lower) & 0xFFFF);
				this.content[wordOffset++] = word;
				rest--;
			}
			return wordOffset;
		}
		
		private int loadCylinderSwapped(FileInputStream fis, int wordOffset) throws IOException {
			int bytesRead = fis.read(this.cylBuffer);
			if (bytesRead != this.cylBuffer.length) {
				throw new IOException("short read: got " + bytesRead + " instead of " + this.cylBuffer.length + " bytes");
			}
			int b = 0;
			int rest = DISK_HEADS * DISK_SECTORS * PrincOpsDefs.WORDS_PER_PAGE;
			while(rest > 0) {
				int lower = this.cylBuffer[b++] & 0xFF;
				int upper = this.cylBuffer[b++] & 0xFF;
				short word = (short)(((upper << 8) | lower) & 0xFFFF);
				this.content[wordOffset++] = word;
				rest--;
			}
			return wordOffset;
		}
		
		/**
		 * Save a new delta file for the disk (if necessary) and do the
		 * housekeeping for the old delta files.
		 * 
		 * @return the operation state for the save operation.
		 */
		public DiskState saveDisk() {
			if (!this.changed) {
				return DiskState.OK;
			}
			if (this.readonly) {
				return DiskState.ReadOnly;
			}
			
			// write new delta to temp file
			String deltatempname = f.getPath() + ".temp_zdelta";
			File deltatemp = new File(deltatempname);
			if (deltatemp.exists()) { deltatemp.delete(); }
			logf("writing temp delta to %s\n", deltatempname);
			try (FileOutputStream fos = new FileOutputStream(deltatempname); DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
				deltaWriteShort(dos, DELTA_SIGNATURE);
				deltaWriteShort(dos, DELTA_VERSION1);
				int chunksWritten = 0;
				int pagesWritten = 0;
				for (int chunkNo = 0; chunkNo < this.chunks.length; chunkNo++) {
					if (this.chunks[chunkNo] != 0) {
						short chunk = this.chunks[chunkNo];
						deltaWriteInt(dos, chunkNo);
						deltaWriteShort(dos, chunk);
						chunksWritten++;
						int chunkBaseOffset = chunkNo * PrincOpsDefs.WORDS_PER_PAGE * 16; // 16 pages in a chunk
						int chunkOffset = 0;
						for (int i = 0; i < 16; i++) {
							if ((chunk & CHUNK_MODIFIED_BITS[i]) != 0) {
								deltaWritePage(dos, chunkBaseOffset + chunkOffset);
								pagesWritten++;
							}
							chunkOffset += PrincOpsDefs.WORDS_PER_PAGE;
						}	
					}
				}
				deltaWriteInt(dos, -1);
				deltaWriteInt(dos, pagesWritten);
				deltaWriteInt(dos, chunksWritten);
			} catch (FileNotFoundException e) {
				return DiskState.SaveDeltaFailed;
			} catch (IOException e) {
				return DiskState.SaveDeltaFailed;
			}
			
			// do the housekeeping on delta files
			String deltaname = f.getPath() + ".zdelta";
			File delta = new File(deltaname);
			if (delta.exists()) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss.SSS");
				String oldDeltaname = deltaname + "-" + sdf.format(delta.lastModified());
				File oldDelta = new File(oldDeltaname);
				delta.renameTo(oldDelta);
				delta = new File(deltaname);
			}
			deltatemp.renameTo(delta);
			
			// delete oldest files to reach deltasToKeep
			File dir = this.f.getParentFile();
			String filterFnStart = delta.getName() + "-";
			File[] oldDeltas = dir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File file, String fn) {
					return fn.startsWith(filterFnStart);
				}
			});
			Arrays.sort(oldDeltas, new Comparator<File>() {
				@Override
				public int compare(File left, File right) {
					return right.getName().compareTo(left.getName()); // descending order
				}
				
			});
			int fileno = 0;
			for (File df: oldDeltas) {
				System.out.printf("old delta: %s [%s]%s\n", df.getName(), (new Date(df.lastModified())).toString(),
						(fileno >= this.deltasToKeep) ? " -> will be deleted" : "");
				if (fileno >= this.deltasToKeep) {
					df.delete();
				}
				fileno++;
			}
			// done
			return DiskState.OK;
		}
		
		/**
		 * @return the number of simulated cylinders for the disk.
		 */
		public int getCylinders() {
			return this.cylinders;
		}
		
		/**
		 * Read a single page (sector) from the disk into mesa memory.
		 * 
		 * @param diskWordOffset the linear (word-)offset of the page to read from the disk 
		 * @param memAddress the mesa virtual memory address where to copy the disk page content
		 * @return the dcb-status for this disk operation
		 */
		public short readPage(int diskWordOffset, int memAddress) {
			// log and basic plausibility checks
			logf("readpage ( diskWordOffset = 0x%08X , memAddress = 0x%08X => realPage = 0x%06X )\n",
					diskWordOffset, memAddress, Mem.getVPageRealPage(memAddress >>> 8));
			if (diskWordOffset < 0 || (diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) >= this.content.length) {
				logf(" *error* diskWordOffset[+WORDS_PER_PAGE] out of range\n");
				return Status_seekTimeout; // TODO: better status code
			}
			if (!Mem.isWritable(memAddress) || !Mem.isWritable(memAddress + PrincOpsDefs.WORDS_PER_PAGE - 1)) {
				logf(" *error* target memory not writable\n");
				return Status_memoryFault;
			}
			
			// copy page content
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE; i++) {
//				if ((i % 16) == 0) { System.out.printf("\n         0x%02X : 0x", i); }
				short w = this.content[diskWordOffset++];
//				System.out.printf(" %04X", w);
				Mem.writeWord(memAddress++, w);
			}
//			System.out.printf("\n\n");
			
			// done
			return Status_goodCompletion;
		}
		
		/**
		 * Write a single page (sector) from mesa memory to the disk.
		 * 
		 * @param diskWordOffset the linear (word-)offset of the page to write on the disk 
		 * @param memAddress the mesa virtual memory address from where to copy the disk page content
		 * @return the dcb-status for this disk operation
		 */
		public short writePage(int diskWordOffset, int memAddress) {
			// log and basic plausibility checks
			logf("writepage ( diskWordOffset = 0x%08X , memAddress = 0x%08X )\n", diskWordOffset, memAddress);
			if (diskWordOffset < 0 || (diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) >= this.content.length) {
				logf(" *error* diskWordOffset[+WORDS_PER_PAGE] out of range\n");
				return Status_seekTimeout; // TODO: better status code
			}
			if (!Mem.isReadable(memAddress) || !Mem.isReadable(memAddress + PrincOpsDefs.WORDS_PER_PAGE - 1)) {
				logf(" *error* target memory not readable\n");
				return Status_memoryFault;
			}
			
			// remember which disk page is changed => save only changed pages to delta file
			int chunkNo = diskWordOffset >>> 12; // 8 for a page and 4 for the chunk
			int pageOffsetInChunk = (diskWordOffset >> 8) & 0x0F;
			this.chunks[chunkNo] |= CHUNK_MODIFIED_BITS[pageOffsetInChunk];
			this.changed = true;
			
			// copy page content
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE; i++) {
				this.content[diskWordOffset++] = Mem.readWord(memAddress++);
			}
			
			// done
			return Status_goodCompletion;
		}
		
		/**
		 * Verify that the given page (sector) on the disk and the given page in
		 * mesa memory have the same content.
		 * 
		 * @param diskWordOffset the linear (word-)offset of the page on the disk 
		 * @param memAddress the mesa virtual memory start address where to compare the disk page content from
		 * @return the dcb-status for this disk operation
		 */
		public short verifyPage(int diskWordOffset, int memAddress) {
			// log and basic plausibility checks
			logf("verifypage ( diskWordOffset = 0x%08X , memAddress = 0x%08X )\n", diskWordOffset, memAddress);
			if (diskWordOffset < 0 || (diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) >= this.content.length) {
				logf(" *error* diskWordOffset[+WORDS_PER_PAGE] out of range\n");
				return Status_seekTimeout; // TODO: better status code
			}
			if (!Mem.isReadable(memAddress) || !Mem.isReadable(memAddress + PrincOpsDefs.WORDS_PER_PAGE - 1)) {
				logf(" *error* target memory not readable\n");
				return Status_memoryFault;
			}
			
			// verify page content
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE; i++) {
				if (this.content[diskWordOffset++] != Mem.readWord(memAddress++)) {
					return Status_dataVerifyError;
				}
			}
			
			// done
			return Status_goodCompletion;
		}
		
		// read a word from a delta file input stream
		private static short deltaReadShort(InputStream i) throws DeltaCorrupted {
			try {
				int b1 = i.read();
				if (b1 < 0) { throw new DeltaCorrupted(); }
				int b2 = i.read();
				if (b2 < 0) { throw new DeltaCorrupted(); }
				return (short)(((b1 << 8) | b2) & 0xFFFF);
			} catch (IOException e) {
				throw new DeltaCorrupted();
			}
		}
		
		// write a word to a delta file output stream
		private static void deltaWriteShort(OutputStream o, short v) throws IOException {
			o.write((v >> 8) & 0xFF);
			o.write(v & 0xFF);
		}
		
		// read an integer from a delta file input stream
		private static int deltaReadInt(InputStream i) throws DeltaCorrupted {
			try {
				int b1 = i.read();
				if (b1 < 0) { throw new DeltaCorrupted(); }
				int b2 = i.read();
				if (b2 < 0) { throw new DeltaCorrupted(); }
				int b3 = i.read();
				if (b3 < 0) { throw new DeltaCorrupted(); }
				int b4 = i.read();
				if (b4 < 0) { throw new DeltaCorrupted(); }
				return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
			} catch (IOException e) {
				throw new DeltaCorrupted();
			}
		}
		
		// write an integer to a delta file output stream
		private static void deltaWriteInt(OutputStream o, int v) throws IOException {
			o.write((v >> 24) & 0xFF);
			o.write((v >> 16) & 0xFF);
			o.write((v >> 8) & 0xFF);
			o.write(v & 0xFF);
		}
		
		// load a page from a delta file input stream
		private void deltaReadPage(InputStream i, int offset) throws DeltaCorrupted {
			int limit = offset + PrincOpsDefs.WORDS_PER_PAGE;
			if ((offset < 0) || (limit >= this.content.length)) {
				throw new DeltaCorrupted();
			}
			for (int o = offset; o < limit; o++) {
				this.content[o] = deltaReadShort(i);
			}
		}
		
		// write a page to a delta file output stream
		private void deltaWritePage(OutputStream o, int offset) throws IOException {
			int limit = offset + PrincOpsDefs.WORDS_PER_PAGE;
			for (int i = offset; i < limit; i++) {
				deltaWriteShort(o, this.content[i]);
			}
		}
	}

	// the list of attached disks (however only one is used/supported)
	private static final List<DiskFile> diskFiles = new ArrayList<>();  
	
	/**
	 * Add a harddisk to the mesa engine. 
	 * 
	 * @param filePath the filename for the harddisk file
	 * @param readonly is the disk to be attached in readonly mode?
	 * @param deltasToKeep number of old delta files to preserve after saving the new delta
	 * @return the diskstate after loading the disk file.
	 */
	public static final DiskState addFile(String filePath, boolean readonly, int deltasToKeep) {
		if (diskFiles.size() > 0) {
			Cpu.logWarning("DiskAgent.addFile :: only 1 disk currently supported, ignored disk file: " + filePath);
			return DiskState.Other;
		}
		
		File f = new File(filePath);
		if (!f.exists() && !f.canWrite()) {
			Cpu.ERROR("DiskAgent.addFile :: file not found or not writable: " + filePath);
		}
		long diskSizeInBytes = f.length();
		if (diskSizeInBytes < DISK_MINIMAL_BYTE_SIZE) {
			Cpu.ERROR("DiskAgent.addFile :: smaller than minimal size: " + diskSizeInBytes +  " bytes (" + filePath + ")");
		}
		
		// create DiskFile and append to files
		try {
			if (Config.AGENTS_LOG_DISK) {
				Cpu.logInfo("DiskAgent.addFile :: adding file '" + filePath + "'");
			}
			DiskFile diskfile = new DiskFile(f, readonly, deltasToKeep);
			diskFiles.add(diskfile);
			return (!readonly && diskfile.readonly) ? DiskState.ReadOnly : DiskState.OK;
		} catch(DeltaCorrupted dc) {
			System.out.printf("-> delta file corrupt, loaded disk possibly unusable\n");
			return DiskState.Corrupted;
		} catch (IOException e) {
			Cpu.ERROR(
					"DiskAgent.addFile :: unable to initialize with file: " + filePath
					+ "\n"
					+ e.getClass() + ": " + e.getMessage());
			return null; // keep the compiler happy, does not know that Cpu.ERROR does not return...
		}
	}
	
	/*
	 * faked disk characteristics
	 */
	private static final int DISK_HEADS = 2;
	private static final int DISK_SECTORS = 16;
	private static final long DISK_MINIMAL_BYTE_SIZE = DISK_HEADS * DISK_SECTORS * PrincOpsDefs.BYTES_PER_PAGE; // at least 1 cylinder!
	
	/*
	 * DiskFCBType
	 * DiskDCBType x 1 (we support exactly one disk!)
	 */
	private static final int fcb_lp_nextIOCB = 0;
	private static final int fcb_w_interruptSelector = 2;
	private static final int fcb_w_stopAgent = 3;
	private static final int fcb_w_agentStopped = 4;
	private static final int fcb_w_numberOfDCBs = 5;
	private static final int dcb_w_deviceType = 6;
	private static final int dcb_w_numberOfCylinders = 7;
	private static final int dcb_w_numberOfHeads = 8;
	private static final int dcb_w_sectorsPerTrack = 9;
	private static final int dcb_w6_agentDeviceData = 10;
	private static final int FCB_with_DCB_SIZE = 16;
	
	private static final int Command_noOp = 0;
	private static final int Command_read = 1;
	private static final int Command_write = 2;
	private static final int Command_verify = 3;
	private static final int Command_format = 4;
	private static final int Command_readHeader = 5;
	private static final int Command_readHeaderAndData = 6;
	private static final int Command_makeBootable = 7;
	private static final int Command_makeUnbootable = 8;
	private static final int Command_getBootLocation = 9;
	// not defined: reserved10 .. reserved19
	
	private static final short Status_inProgress = 0;
	private static final short Status_goodCompletion = 1;
	private static final short Status_notReady = 2;
	private static final short Status_recalibrateError = 3;
	private static final short Status_seekTimeout = 4;
	private static final short Status_headerCRCError = 5;
	private static final short Status_reserved6 = 6;
	private static final short Status_dataCRCError = 7;
	private static final short Status_headerNotFound = 8;
	private static final short Status_reserved9 = 9;
	private static final short Status_dataVerifyError = 10;
	private static final short Status_overrunError = 11;
	private static final short Status_writeFault = 12;
	private static final short Status_memoryError = 13;
	private static final short Status_memoryFault = 14;
	private static final short Status_clientError = 15;
	private static final short Status_operationReset = 16;
	private static final short Status_otherError = 17;
	
	private static final int iocb_w_op_clientHeader_cylinder = 0;
	private static final int iocb_w_op_clientHeader_sectorHead = 1; // high-nibble: sector, low-nibble: head
	private static final int iocb_dbl_op_reserved1 = 2;
	private static final int iocb_lp_op_dataPtr = 4; // page(!)-aligned data address
	private static final int iocb_w_op_ctl = 6; // tries(8), command(6), enableTrackBuffer(1), incrementDataPtr(1)
	private static final int iocb_w_op_pageCount = 7;
	private static final int iocb_dbl_op_deviceStatus = 8;
	private static final int iocb_dbl_op_diskHeader = 10; // target for read header
	private static final int iocb_w_op_device = 12;
	private static final int iocb_w_deviceIndex = 13;
	private static final int iocb_w_diskAddress_cylinder = 14;
	private static final int iocb_w_diskAddress_sectorHead = 15; // high-nibble: sector, low-nibble: head
	private static final int iocb_lp_dataPtr = 16;
	private static final int iocb_w_incrementDataPtr = 18;
	private static final int iocb_w_command = 19;
	private static final int iocb_w_pageCount = 20;
	private static final int iocb_w_status = 21;
	private static final int iocb_lp_nextIocb = 22;
	private static final int iocb_w10_agentOperationData = 24;
	
	/**
	 * Constructor.
	 * 
	 * @param fcbAddress the base address of the disk agent FCB.
	 */
	public DiskAgent(int fcbAddress) {
		super(AgentDevice.diskAgent, fcbAddress, FCB_with_DCB_SIZE);
		this.enableLogging(Config.AGENTS_LOG_DISK);
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		logf("shutdown\n");
		for (DiskFile f : diskFiles) {
			f.saveDisk();
		}
	}
	
	@Override
	public void refreshMesaMemory() {
		// disk agent works synchronously, so currently nothing to transfer to mesa memory
	}
	
	/*
	 * statistical data
	 */
	
	private int reads = 0;
	private int writes = 0;
	
	public int getReads() { return this.reads; }
	
	public int getWrites() { return this.writes; }

	/*
	 * implementation of the public agent methods 
	 */

	@Override
	protected void initializeFcb() {
		if (diskFiles.size() != 1) {
			Cpu.ERROR("DiskAgent.initializeFcb :: disk count not 1 (actual: " + diskFiles.size() + ")");
		}
		
		this.setFcbDblWord(fcb_lp_nextIOCB, 0);
		this.setFcbWord(fcb_w_interruptSelector, 0);
		this.setFcbWord(fcb_w_stopAgent, PrincOpsDefs.FALSE);
		this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.TRUE);
		this.setFcbWord(fcb_w_numberOfDCBs, 1);
		
		this.setFcbWord(dcb_w_deviceType, PilotDefs.Device_anyPilotDisk);
		this.setFcbWord(dcb_w_numberOfHeads, DISK_HEADS);
		this.setFcbWord(dcb_w_sectorsPerTrack, DISK_SECTORS);
		this.setFcbWord(dcb_w_numberOfCylinders, diskFiles.get(0).getCylinders());
		for (int i = 0; i < 6; i++) {
			this.setFcbWord(dcb_w6_agentDeviceData + i, 0);
		}
	}
	
	@Override
	public void call() {
		int iocb = this.getFcbDblWord(fcb_lp_nextIOCB);
		short interruptSelector = this.getFcbWord(fcb_w_interruptSelector);
		
		// check for stopping the agent
		boolean stopAgent = (this.getFcbWord(fcb_w_stopAgent) != PrincOpsDefs.FALSE);
		logf("call() - stopAgent = %s\n", (stopAgent) ? "true" : "false");
		if (stopAgent) {
			logf("call() - stop agent\n");
			this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.TRUE);
			return;
		}
		this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.FALSE);
		
		// is there something to do?
		if (iocb == 0) {
			logf("call() - iocb == 0 (done, no iocb)\n");
			return; // no IOCB, nothing to be done,,,
		}
		
		logf("call() - interruptSelector = 0x%04X\n", interruptSelector & 0xFFFF);
		
		// process all IOCBs
		while(iocb != 0) {
			logf("call() - processing IOCB 0x%08X\n", iocb);
			
			int diskIndex = Mem.readWord(iocb + iocb_w_deviceIndex);
			int cylinder = Mem.readWord(iocb + iocb_w_diskAddress_cylinder);
			int sectorHead = Mem.readWord(iocb + iocb_w_diskAddress_sectorHead);
			int diskWordOffset = this.getDiskWordOffset(cylinder, sectorHead);
			
			logf("call() -    diskIndex = %d , cylinder = 0x%04X , sectorHead = 0x%04X , diskWordOffset = 0x%08X\n",
					diskIndex, cylinder, sectorHead, diskWordOffset);
			
			int dataPtr = Mem.readDblWord(iocb + iocb_lp_dataPtr);
			boolean incrementDataPtr = (Mem.readWord(iocb + iocb_w_incrementDataPtr) != PrincOpsDefs.FALSE);
			int command = Mem.readWord(iocb + iocb_w_command) & 0xFFFF;
			int pageCount = Mem.readWord(iocb + iocb_w_pageCount) & 0xFFFF;
			
			logf("call() -    command = %d , dataPtr = 0x%08X , pageCount = %d , incrementDataPtr = %s\n",
					command, dataPtr, pageCount, (incrementDataPtr) ? "true" : "false");
			
			int currIocb = iocb;
			short currIocbStatus = Status_goodCompletion;
			
			iocb = Mem.readDblWord(iocb + iocb_lp_nextIocb);
			
			if (diskIndex >= diskFiles.size()) {
				Mem.writeWord(currIocb + iocb_w_status, Status_clientError);
				continue;
			}
			DiskFile disk = diskFiles.get(diskIndex);
			if (disk == null) {
				Mem.writeWord(currIocb + iocb_w_status, Status_notReady);
				continue;
			}
			
			if (dataPtr == 0) {
				Mem.writeWord(currIocb + iocb_w_status, Status_memoryError);
				continue;
			}
			
			while(pageCount > 0) {
				// execute requested disk operation, if implemented
				switch(command) {
				case Command_noOp:
					currIocbStatus = Status_goodCompletion; // just for completeness
					break;
				case Command_read:
					currIocbStatus = disk.readPage(diskWordOffset, dataPtr);
					this.reads++;
					break;
				case Command_write:
					currIocbStatus = disk.writePage(diskWordOffset, dataPtr);
					this.writes++;
					break;
				case Command_verify:
					currIocbStatus = disk.verifyPage(diskWordOffset, dataPtr);
					break;
				default:
					// some unsupported disk operation
					currIocbStatus = Status_otherError;
				}				
				if (currIocbStatus != Status_goodCompletion) {
					break;
				}
				
				// done with this page
				pageCount--;
				
				// prepare for next sector
				diskWordOffset += PrincOpsDefs.WORDS_PER_PAGE;
				dataPtr += PrincOpsDefs.WORDS_PER_PAGE;
				
				// update iocb data
				Mem.writeWord(currIocb + iocb_w_pageCount, (short)(pageCount & 0xFFFF));
				if (incrementDataPtr) {
					Mem.writeDblWord(currIocb + iocb_lp_dataPtr, dataPtr);
				}
			}
			
			// save state for this IOCB
			Mem.writeWord(currIocb + iocb_w_status, currIocbStatus);
			
			logf("call() - done processing IOCB 0x%08X => dataPtr = 0x%08X , pageCount = %d , status = %d\n",
				currIocb,
				Mem.readDblWord(currIocb + iocb_lp_dataPtr),
				Mem.readWord(currIocb + iocb_w_pageCount),
				Mem.readWord(currIocb + iocb_w_status)
				);
		}
		
		// raise interrupt after having processed all IOCBs
		Processes.requestMesaInterrupt(interruptSelector);
	}
	
	// compute the (word) offset for the given cylinder/sector/head
	private int getDiskWordOffset(int cylinder, int sectorHead) {
		int head = (sectorHead >> 8) & 0x00FF;
		int sector = sectorHead & 0x00FF;

		int absoluteSector
				= (cylinder * DISK_HEADS * DISK_SECTORS)
				+ (head * DISK_SECTORS)
				+ sector;
		
		logf("call() -    cyl = %d - head = %d - sect = %d => abs-sector = %d\n",
				cylinder, head, sector, absoluteSector);
		
		return absoluteSector * PrincOpsDefs.WORDS_PER_PAGE;
	}
}
