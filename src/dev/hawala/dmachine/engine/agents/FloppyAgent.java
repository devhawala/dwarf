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
import java.io.FileOutputStream;
import java.io.IOException;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PilotDefs;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;

/**
 * Agent for a floppy disk drive of a Dwarf machine,
 * <p>
 * One single floppy drive is currently supported (it is also
 * unknown if Pilot can work with more than one drive).
 * <br>
 * The agent works synchronously, meaning that floppy disk i/o occurs during the
 * {@code call()} (resp.the CALLAGENT instruction). The interrupt signaling the
 * end of operation is therefore requested at the end of the {@code call()}
 * method call. 
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class FloppyAgent extends Agent {
	
	/*
	 * faked floppy properties for a 1.44MB, 3 1/2" disks
	 */
	private static final int FLOPPY_HEADS = 2;
	private static final int FLOPPY_SECTORS = 18;
	private static final int FLOPPY_CYLS = 80; // nominal number of tracks for a 3 1/2" disk, gives 1440000 bytes
	private static final int FLOPPY_TOTAL_CYLS = FLOPPY_HEADS * FLOPPY_SECTORS * FLOPPY_CYLS;
	
	
	/*
	 * FloppyFCBType
	 * FloppyDCBType (we support exactly one floppy drive, with a floppy inserted or not)
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
	private static final int dcb_w_ready = 10;
	private static final int dcb_w_diskChanged = 11;
	private static final int dcb_w_twoSided = 12;
	private static final int dcb_w_suggestedTries = 13;
	private static final int FCB_SIZE = 14;
	
	// status codes (FloppyDiskFace.Status)
	private static final short Status_inProgress       =  0;
	private static final short Status_goodCompletion   =  1;
	private static final short Status_diskChange       =  2;
	private static final short Status_notReady         =  3;
	private static final short Status_cylinderError    =  4;
	private static final short Status_deletedData      =  5;
	private static final short Status_recordNotFound   =  6;
	private static final short Status_headerError      =  7;
	private static final short Status_dataError        =  8;
	private static final short Status_dataLost         =  9;
	private static final short Status_writeFault       = 10;
	private static final short Status_memoryError      = 11;
	private static final short Status_invalidOperation = 12;
	private static final short Status_aborted          = 13;
	private static final short Status_otherError       = 14;
	
	private static final short Status_invalidCHS       = Status_recordNotFound; // TODO: better status code ??
	
	// operations
	private static final int Op_nop = 0;
	private static final int Op_readSector = 1;
	private static final int Op_writeSector = 2;
	private static final int Op_writeDeletedSector = 3;
	private static final int Op_readId = 4;
	private static final int Op_formatTrack = 5;
	private static final String[] OP_NAMES = {
		"nop", "readSector", "writeSector", "writeDeletedSector", "readId", "formatTrack"	
	};
	
	// FloppyIOCBType
	private static final int iocb_w_oper_device = 0;
	private static final int iocb_w_oper_function = 1;
	private static final int iocb_w_oper_address_cylinder = 2;
	private static final int iocb_w_oper_adddress_sectorHead = 3; // head(0..7) , sector(8..15)
	private static final int iocb_lp_oper_dataPtr = 4;
	private static final int iocb_w_oper_incrementDataPtrAndRetries = 6; // incrementDataPointer(6:0..0) , tries(6:1..15)
	private static final int iocb_w_oper_count = 7; // number of sectors to transfer OR number of tracks to format
	private static final int iocb_w_density = 8;
	private static final int iocb_w_sectorLength = 9;
	private static final int iocb_w_sectorsPertrack = 10;
	private static final int iocb_w_status = 11; // FloppyDiskFace.Status
	private static final int iocb_lp_nextIocb = 12;
	private static final int iocb_w_retries = 13;
	private static final int iocb_w_logStatus = 14;
	
	private static final int incrementDataPtrFlag = 0x00008000;
	private static final int retriesMask = 0x00007FFF;
	
	private  static class FloppyDisk3dot5 {
		
		private final static int WORD_SIZE = FLOPPY_HEADS * FLOPPY_CYLS * FLOPPY_SECTORS * PrincOpsDefs.WORDS_PER_PAGE;
		private final static int BYTE_SIZE = WORD_SIZE * 2;
		
		private static final byte[] ioBuffer = new byte[4096];
		
		private final File f;
		private final boolean swapBytes;
		public final boolean readonly;
		
		private final short[] content = new short[WORD_SIZE];
		
		private boolean changed = false;
		
		private void logf(String template, Object... args) {
			if (Config.AGENTS_LOG_FLOPPY) {
				System.out.printf("FloppyFile[" + f.getName() + "]: " + template, args);
			}
		}
		
		public FloppyDisk3dot5(File f, boolean readonly) throws IOException {
			this.f = f;
			if (!f.exists()) {
				logf("file not found\n");
				throw new IOException("Floppy file '" + f.getName() + "' not found");
			}
			if (!f.canWrite()) {
				this.readonly = true;
			} else {
				this.readonly = readonly;
			}
			if (f.length() != BYTE_SIZE) {
				throw new IOException("Floppy file has wrong size " + f.length() + " bytes (instead of " + BYTE_SIZE + ")");
			}
			
			// load floppy content
			synchronized(ioBuffer) {
				try (FileInputStream fis = new FileInputStream(f)) {
					// read first 8 sectors
					fis.read(ioBuffer);
					
					// check signatures on sectors 4 and 6 (counting from 0) to see if bytes must be swapped
					this.swapBytes                                                       // a xerox formatted floppy has the following signatures:
						 = ioBuffer[2048] == (byte)0xD9 && ioBuffer[2049] == (byte)0xC5  // 5. sector starts with 0xC5D9 => swap if reversed 
						&& ioBuffer[3072] == (byte)0xD6 && ioBuffer[3073] == (byte)0xE5; // 7. sector starts with 0xE5D6 => swap if reversed
					
					// load file content ioBuffer-wise converting byte-pairs to shorts
					int bufferPos = 0;
					for (int i = 0; i < WORD_SIZE; i++) {
						if (bufferPos >= ioBuffer.length) {
							fis.read(ioBuffer);
							bufferPos = 0;
						}
						int b1 = ((this.swapBytes) ? ioBuffer[bufferPos + 1] : ioBuffer[bufferPos]) & 0xFF;
						int b2 = ((this.swapBytes) ? ioBuffer[bufferPos] : ioBuffer[bufferPos + 1]) & 0xFF;
						bufferPos += 2;
						short w = (short)((b1 << 8) | b2);
						this.content[i] = w;
					}
				}
			}
		}
		
		public void saveFloppy(StringBuilder sb) {
			if (this.readonly) { return; }
			if (!this.changed) { return; }
			
			logf("saveFloppy(): writing floppy back back");
			synchronized(ioBuffer) {
				try (FileOutputStream fos = new FileOutputStream(this.f)) {
					int bufferPos = 0;
					for (int i = 0; i < WORD_SIZE; i++) {
						int w = this.content[i] & 0xFFFF;
						int b1 = ((this.swapBytes)) ? w & 0xFF : w >>> 8;
						int b2 = ((this.swapBytes)) ? w >>> 8 : w & 0xFF;
						ioBuffer[bufferPos++] = (byte)b1;
						ioBuffer[bufferPos++] = (byte)b2;
						if (bufferPos >= ioBuffer.length) {
							fos.write(ioBuffer);
							bufferPos = 0;
						}
					}
					if (bufferPos > 0) { // should not happen, just to be sure...
						fos.write(ioBuffer, 0, bufferPos);
					}
				} catch (IOException e) {
					if (sb != null) {
						if (sb.length() > 0) { sb.append("\n"); }
						sb.append("Error writing floppy content back: ").append(e.getMessage());
					}
				}
			}
		}
		
		public boolean isInvalidCHS(int cylinder, int head, int sector) {
			return cylinder < 0 || cylinder >= FLOPPY_CYLS
				|| head < 0 || head >= FLOPPY_HEADS
				|| sector < 1 || sector > FLOPPY_SECTORS; // sector counting starts with 1 ...??
		}
		
		public int getLinearSector(int cylinder, int head, int sector) {
			int absoluteSector = (cylinder * FLOPPY_HEADS + head) * FLOPPY_SECTORS + sector - 1; // sector counting starts with 1 ... 
			return absoluteSector;
		}
		
		public void storeSectorNoIntoIocb(int linearSector, int iocb) {
			int sector = (linearSector % FLOPPY_SECTORS) + 1;
			int track = linearSector / FLOPPY_SECTORS;
			int head = track % FLOPPY_HEADS;
			short cylinder = (short)(track / FLOPPY_HEADS);
			
			short sectorHead = (short)(((head << 8) | (sector & 0xFF)) & 0xFFFF);
			
			Mem.writeWord(iocb + iocb_w_oper_address_cylinder, cylinder);
			Mem.writeWord(iocb + iocb_w_oper_adddress_sectorHead, sectorHead);
		}
		
		public short readSector(int absSector, int memAddress, int sectorLength) {
			// log and basic plausibility checks
			logf("    readSector ( absSector = %d , memAddress = 0x%08X => realPage = 0x%06X )\n",
					absSector, memAddress, Mem.getVPageRealPage(memAddress >>> 8));
			if (!Mem.isWritable(memAddress) || !Mem.isWritable(memAddress + PrincOpsDefs.WORDS_PER_PAGE - 1)) {
				logf(" *error* readSector : target memory not writable\n");
				return Status_memoryError;
			}
			int diskWordOffset = absSector * PrincOpsDefs.WORDS_PER_PAGE;
			if ((diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) > this.content.length) {
				logf(" *error* readSector : absSector out of range\n");
				return Status_invalidCHS;
			}
			
			// copy sector content
			for (int i = 0; i < Math.min(sectorLength, PrincOpsDefs.WORDS_PER_PAGE); i++) {
//				if ((i % 16) == 0) { System.out.printf("\n         0x%02X : 0x", i); }
				short w = this.content[diskWordOffset++];
//				System.out.printf(" %04X", w);
				Mem.writeWord(memAddress++, w);
			}
//			System.out.printf("\n\n");

			// done
			return Status_goodCompletion;
		}
		
		public short writeSector(int absSector, int memAddress, int sectorLength) {
			// log and basic plausibility checks
			logf("    writeSector ( absSector = %d , memAddress = 0x%08X => realPage = 0x%06X )\n",
					absSector, memAddress, Mem.getVPageRealPage(memAddress >>> 8));
			if (this.readonly) {
				logf(" *error* writeSector : floppy is readonly\n");
				return Status_writeFault;
			}
			if (!Mem.isReadable(memAddress) || !Mem.isReadable(memAddress + PrincOpsDefs.WORDS_PER_PAGE - 1)) {
				logf(" *error* writeSector : target memory not readable\n");
				return Status_memoryError;
			}
			int diskWordOffset = absSector * PrincOpsDefs.WORDS_PER_PAGE;
			if ((diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) > this.content.length) {
				logf(" *error* writeSector : absSector out of range\n");
				return Status_invalidCHS;
			}
			
			// copy sector content
			for (int i = 0; i < Math.min(sectorLength, PrincOpsDefs.WORDS_PER_PAGE); i++) {
//				if ((i % 16) == 0) { System.out.printf("\n         0x%02X : 0x", i); }
				short w = Mem.readWord(memAddress++);
				this.content[diskWordOffset++] = w;
//				System.out.printf(" %04X", w);
			}
//			System.out.printf("\n\n");

			// done
			this.changed = true;
			return Status_goodCompletion;
		}
		
		public short writeDeletedSector(int absSector) {
			// log and basic plausibility checks
			logf("    writeDeletedSector ( absSector = %d )\n", absSector);
			if (this.readonly) {
				logf(" *error* writeDeletedSector : floppy is readonly\n");
				return Status_writeFault;
			}
			int diskWordOffset = absSector * PrincOpsDefs.WORDS_PER_PAGE;
			if ((diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) >= this.content.length) {
				logf(" *error* writeDeletedSector : absSector out of range\n");
				return Status_invalidCHS;
			}
			
			// zero out sector content
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE; i++) {
				this.content[diskWordOffset++] = 0;
			}

			// done
			this.changed = true;
			return Status_goodCompletion;
		}
		
		public short formatTrack(int cylinder, int head) {
			// log and basic plausibility checks
			logf("    formatTrack ( ch = ( %d , %d )\n", cylinder, head);
			if (this.isInvalidCHS(cylinder, head, 1)) {
				logf(" *error* formatTrack ch = ( %d , %d  ) out of range\n", cylinder, head);
				return Status_invalidCHS;
			}
			if (this.readonly) {
				logf(" *error* formatTrack : floppy is readonly\n");
				return Status_writeFault;
			}
			
			// zero out sector content
			int diskWordOffset = this.getLinearSector(cylinder, head, 1) * PrincOpsDefs.WORDS_PER_PAGE;
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE * FLOPPY_SECTORS; i++) {
				this.content[diskWordOffset++] = 0;
			}

			// done
			this.changed = true;
			return Status_goodCompletion;
		}
	}
	
	public FloppyAgent(int fcbAddress) {
		super(AgentDevice.floppyAgent, fcbAddress, FCB_SIZE);
		this.enableLogging(Config.AGENTS_LOG_FLOPPY);
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// save current floppy if present
		if (this.currFloppy != null) {
			this.currFloppy.saveFloppy(errMsgTarget);
		}
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		if (this.nextFloppy != null || this.nextEjected) {
			// save current floppy if present
			if (this.currFloppy != null) {
				this.currFloppy.saveFloppy(null);
			}
			
			// load the floppy
			this.currFloppy = this.nextFloppy;
			this.floppyChanged = (this.currFloppy != null);
			
			// reset the "next" data
			this.nextFloppy = null;
			this.nextEjected = false;
		}
	}
	
	private FloppyDisk3dot5 nextFloppy = null;
	private boolean nextEjected = false;
	
	private FloppyDisk3dot5 currFloppy = null;
	private boolean floppyChanged = false;
	
	public boolean insertFloppy(File f, boolean readonly) throws IOException {
		this.nextFloppy = new FloppyDisk3dot5(f, readonly);
		this.nextEjected = false;
		return this.nextFloppy.readonly;
	}
	
	public void ejectFloppy() {
		this.nextFloppy = null;
		this.nextEjected = true;
	}
	
	private int reads = 0;
	private int writes = 0;
	
	public int getReads() { return this.reads; }
	
	public int getWrites() { return this.writes; }

	@Override
	public void call() {
		// process the stopAgent-flag 
		boolean stopAgent = (this.getFcbWord(fcb_w_stopAgent) != PrincOpsDefs.FALSE);
		logf("call() - stopAgent = %s\n", (stopAgent) ? "true" : "false");
		if (stopAgent) {
			logf("call() - stop agent\n");
			this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.TRUE);
			return;
		}
		this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.FALSE);

		// get the (first) io control block to process
		int iocb = this.getFcbDblWord(fcb_lp_nextIOCB);
		if (iocb == 0) {
			logf("call() - iocb == 0 (done, no iocb)\n");
			return; // no IOCB, nothing to be done,,,
		}

		// get the interrupt to use when i/o is complete
		short interruptSelector = this.getFcbWord(fcb_w_interruptSelector);
		logf("call() - interruptSelector = 0x%04X\n", interruptSelector & 0xFFFF);
		
		// process all io control blocks passed
		while(iocb != 0) {
			// fetch the IOCB data
			int deviceIndex = Mem.readWord(iocb + iocb_w_oper_device) & 0xFFFF;
			int function = Mem.readWord(iocb + iocb_w_oper_function) & 0xFFFF;
			int cylinder = Mem.readWord(iocb + iocb_w_oper_address_cylinder) & 0xFFFF;
			int sectorHead = Mem.readWord(iocb + iocb_w_oper_adddress_sectorHead) & 0xFFFF;
			int head = sectorHead >>> 8;
			int sector = sectorHead & 0xFF;
			int dataPtr = Mem.readDblWord(iocb + iocb_lp_oper_dataPtr);
			int incrementDataPtrAndRetries = Mem.readWord(iocb + iocb_w_oper_incrementDataPtrAndRetries) & 0xFFFF;
			boolean incrementDataPtr = (incrementDataPtrAndRetries & incrementDataPtrFlag) != 0;
			int retries = incrementDataPtrAndRetries & retriesMask;
			int count = Mem.readWord(iocb + iocb_w_oper_count) & 0xFFFF;
			int density = Mem.readWord(iocb + iocb_w_density) & 0xFFFF;
			int sectorLength = Mem.readWord(iocb + iocb_w_sectorLength) & 0xFFFF;
			int sectorsPerTrack = Mem.readWord(iocb + iocb_w_sectorsPertrack) & 0xFFFF;
			short status = Mem.readWord(iocb + iocb_w_status);
			
			// log the next operation to perform
			String functionName = (function < OP_NAMES.length) ? OP_NAMES[function] : "*invalid*";
			logf(
				"  iocb = 0x%08X : function = %d (%s)\n" + 
				"       -> chs = ( %d , %d , %d ) , dataPtr = 0x%08X, incrDataPtr = %s\n" + 
				"       -> retries = %d , count = %d , density = %d , sectorLength = %d , sectorsPerTrack = %d\n",
				iocb, function, functionName,
				cylinder, head, sector, dataPtr, (incrementDataPtr) ? "true" : "false",
				retries, count, density, sectorLength, sectorsPerTrack
				);
			
			// check if the only floppy drive is accessed and a floppy is inserted
			if (deviceIndex != 0) {
				Mem.writeWord(iocb + iocb_w_status, Status_otherError);
				iocb = Mem.readDblWord(iocb + iocb_lp_nextIocb);
				continue;
			}
			if (this.currFloppy == null) {
				Mem.writeWord(iocb + iocb_w_status, Status_notReady);
				iocb = Mem.readDblWord(iocb + iocb_lp_nextIocb);
				continue;
			}
			
			// do the operation requested in this iocb
			switch(function) {
			
			case Op_nop: {
					status = Status_goodCompletion;
				}
				break;
				
			case Op_readSector: {
					// check the specified sector address
					if (this.currFloppy.isInvalidCHS(cylinder, head, sector)) { 
						status = Status_invalidCHS;
						break;
					}
					
					// initialize and process the number of requested sectors
					int linearSector = this.currFloppy.getLinearSector(cylinder, head, sector);
					int memAddress = dataPtr;
					status = Status_goodCompletion;
					while (count > 0 && linearSector < FLOPPY_TOTAL_CYLS) {
						// do it
						status = this.currFloppy.readSector(linearSector, memAddress, sectorLength);
						if (status != Status_goodCompletion) { break; }
						
						// decrement the sector count and inform Pilot
						count--;
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						
						// go to the next sector and inform Pilot
						linearSector++;
						this.currFloppy.storeSectorNoIntoIocb(linearSector, iocb);
						
						// go to next memory target block and inform Pilot (if requested) 
						if (incrementDataPtr) {
							memAddress += sectorLength;
							Mem.writeDblWord(iocb + iocb_lp_oper_dataPtr, memAddress);
						}
						
						// update the stats
						this.reads++;
					}
				}
				break;
				
			case Op_writeSector: {
					// write a different disk without reading it is a problem 
					if (this.floppyChanged) {
						status = Status_diskChange;
						break;
					}
					// check the specified sector address
					if (this.currFloppy.isInvalidCHS(cylinder, head, sector)) { 
						status = Status_invalidCHS;
						break;
					}
					
					// initialize and process the number of requested sectors
					int linearSector = this.currFloppy.getLinearSector(cylinder, head, sector);
					int memAddress = dataPtr;
					status = Status_goodCompletion;
					while (count > 0 && linearSector < FLOPPY_TOTAL_CYLS) {
						// do it
						status = this.currFloppy.writeSector(linearSector, memAddress, sectorLength);
						if (status != Status_goodCompletion) { break; }
						
						
						// decrement the sector count and inform Pilot
						count--;
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						
						// go to the next sector and inform Pilot
						linearSector++;
						this.currFloppy.storeSectorNoIntoIocb(linearSector, iocb);
						
						// go to next memory source block and inform Pilot (if requested) 
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						if (incrementDataPtr) {
							memAddress += sectorLength;
							Mem.writeDblWord(iocb + iocb_lp_oper_dataPtr, memAddress);
						}
						
						// update the stats
						this.writes++;
					}
				}
				break;
				
			case Op_writeDeletedSector: {
					// write a different disk without reading it is a problem 
					if (this.floppyChanged) {
						status = Status_diskChange;
						break;
					}
					// check the specified sector address
					if (this.currFloppy.isInvalidCHS(cylinder, head, sector)) { 
						status = Status_invalidCHS;
						break;
					}
					
					// initialize and process the number of requested sectors
					int linearSector = this.currFloppy.getLinearSector(cylinder, head, sector);
					status = Status_goodCompletion;
					while (count > 0 && linearSector < FLOPPY_TOTAL_CYLS) {
						// do it
						status = this.currFloppy.writeDeletedSector(linearSector);
						if (status != Status_goodCompletion) { break; }
						
						// decrement the sector count and inform Pilot
						count--;
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						
						// go to the next sector and inform Pilot
						linearSector++;
						this.currFloppy.storeSectorNoIntoIocb(linearSector, iocb);
						
						// do the stats
						this.writes++;
					}
				}
				break;
				
			case Op_readId: {
					status = Status_invalidOperation;
				}
				break;
				
			case Op_formatTrack: {
					// write a different disk without reading it is a problem 
					if (this.floppyChanged) {
						status = Status_diskChange;
						break;
					}
					// check the specified sector address
					if (this.currFloppy.isInvalidCHS(cylinder, head, sector)) { 
						status = Status_invalidCHS;
						break;
					}
					
					// format the requested number of tracks
					while (count > 0) {
						// do it
						status = this.currFloppy.formatTrack(cylinder, head);
						if (status != Status_goodCompletion) { break; }
						
						// decrement the track count and inform Pilot
						count--;
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						
						// switch to the next track
						head++;
						if (head >= FLOPPY_HEADS) {
							head = 0;
							cylinder++;
							if (cylinder > FLOPPY_CYLS) { break; }
						}
						
						// do the stats
						this.writes++;
					}
				}
				break;
				
			default:
				status = Status_notReady;
			}
			
			logf("  iocb = 0x%08X  => status = %d\n", iocb, status);
			
			// put the status of this io control block
			Mem.writeWord(iocb + iocb_w_status, status);
			
			// go to next IOCB
			iocb = Mem.readDblWord(iocb + iocb_lp_nextIocb);
		}
		
		// Pilot was already informed that the disk changed, so it is already used on the next operation
		this.floppyChanged = false;
		
		logf("call() - done\n");
		
		// unconditionally enqueue an interrupt `
		Processes.requestMesaInterrupt(interruptSelector);
	}

	@Override
	protected void initializeFcb() {
		this.setFcbDblWord(fcb_lp_nextIOCB, 0);
		this.setFcbWord(fcb_w_interruptSelector, 0);
		this.setFcbWord(fcb_w_stopAgent, PrincOpsDefs.FALSE);
		this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.TRUE);
		this.setFcbWord(fcb_w_numberOfDCBs, 1);
		
		this.setFcbWord(dcb_w_deviceType, PilotDefs.Device_microFloppy);
		this.setFcbWord(dcb_w_numberOfHeads, FLOPPY_HEADS);
		this.setFcbWord(dcb_w_sectorsPerTrack, FLOPPY_SECTORS);
		this.setFcbWord(dcb_w_numberOfCylinders, FLOPPY_CYLS);
		this.setFcbWord(dcb_w_ready, PrincOpsDefs.FALSE);
		this.setFcbWord(dcb_w_diskChanged, PrincOpsDefs.TRUE);
		this.setFcbWord(dcb_w_twoSided, PrincOpsDefs.TRUE);
		this.setFcbWord(dcb_w_suggestedTries, 1);
	}

}
