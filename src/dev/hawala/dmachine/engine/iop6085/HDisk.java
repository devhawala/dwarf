/*
Copyright (c) 2019, Dr. Hans-Walter Latz
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

package dev.hawala.dmachine.engine.iop6085;

import static dev.hawala.dmachine.engine.iop6085.IORegion.mkBoolField;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkByteSwappedWord;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkField;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkIOPBoolean;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkWord;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.ByteSwappedPointer;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.ClientCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.IOPCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.NotifyMask;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.OpieAddress;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.TaskContextBlock;
import dev.hawala.dmachine.engine.iop6085.IORegion.BoolField;
import dev.hawala.dmachine.engine.iop6085.IORegion.DblWord;
import dev.hawala.dmachine.engine.iop6085.IORegion.Field;
import dev.hawala.dmachine.engine.iop6085.IORegion.IOPBoolean;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;
import dev.hawala.dmachine.engine.iop6085.IORegion.IOStruct;
import dev.hawala.dmachine.engine.iop6085.IORegion.Word;

/**
 * IOP device handler for the emulated rigid disk of a Daybreak/6085 machine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class HDisk extends DeviceHandler {
	
	/*
	 * general constants
	 */
	
	private static final int MAX_DISKS = 2; // max. *one* disk??
	
	private static final int[] driveSelectMasks = { 2 , 1 }; // must be MAX_DISK long 
	
	private static final short anyPilotDisk = 64; // Device.Type = [FIRST[Device.PilotDisk]];    -- a Pilot disk of unknown nature
	
	/*
	 * Function Control Block and Device Context Block
	 */
	
	private static final String DiskFCB = "DiskFCB";
	
	private interface DiskCommand {
		public static final int goToIdleLoop = 0;
		public static final int xferDOBToController = 1;
		public static final int executeDOB = 2;
		public static final int xferDOBFromController = 3;
	}
	private static String getDiskCommandString(int cmd) {
		switch(cmd) {
		case DiskCommand.goToIdleLoop: return "goToIdleLoop";
		case DiskCommand.xferDOBToController: return "xferDOBToController";
		case DiskCommand.executeDOB: return "executeDOB";
		case DiskCommand.xferDOBFromController: return "xferDOBFromController";
		default: return "invalid(" + cmd + ")";
		}
	}
	
	private static class DeviceContextBlock {
		public final Word mesaHead;        // IOCBPtr
		public final Word handlerMesaNext; // IOCBPtr
		public final Word mesaTail;        // IOCBPtr
		public final IOPBoolean blockMesaQueue;
		public final OpieAddress iopHead;
		public final OpieAddress handlerIOPNext;
		public final OpieAddress iopTail;
		public final IOPBoolean blockIOPQueue;
		public final Word currentDriveMaskAndDiskCommand;
		/**/public final Field currentDriveMask;
		/**/public final Field diskCommand; // values from interface DiskCommand
		public final ClientCondition mesaClientCondition;
		public final ClientCondition iopClientCondition;
		public final OpieAddress currentIOCB;
		public final Word flagsMaskAndDiskState;
		/**/public final BoolField recalibrate;
		/**/public final BoolField driveExists;
		/**/public final BoolField useEcc;
		/**/public final Field selectMask;
		/**/public final Field diskState;
		/**/public final Field dcbOffset;
		public final Word devInfo_driveType;
		public final Word devInfo_sectorsPerTrackAndheadsPerCylinder;
		/**/public final Field devInfo_sectorsPerTrack;
		/**/public final Field devInfo_headsPerCylinder;
		public final Word devInfo_cylindersPerDrive;
		public final Word devInfo_reduceWriteCurrentCylinder;
		public final Word devInfo_precompensationCylinder;
		
		private DeviceContextBlock(String name) {
			this.mesaHead = mkWord(name, "mesaHead");
			this.handlerMesaNext = mkWord(name, "handlerMesaNext");
			this.mesaTail = mkWord(name, "mesaTail");
			this.blockMesaQueue = mkIOPBoolean(name, "blockMesaQueue");
			this.iopHead = new OpieAddress(name, "iopHead");
			this.handlerIOPNext = new OpieAddress(name, "handlerIOPNext");
			this.iopTail = new OpieAddress(name, "iopTail");
			this.blockIOPQueue = mkIOPBoolean(name, "blockIOPQueue");
			this.currentDriveMaskAndDiskCommand = mkWord(name, "currentDriveMaskAndDiskCommand");
			/**/this.currentDriveMask = mkField("currentDriveMask", this.currentDriveMaskAndDiskCommand, 0xFF00);
			/**/this.diskCommand = mkField("diskCommand", this.currentDriveMaskAndDiskCommand, 0x00FF);
			this.mesaClientCondition = new ClientCondition(name, "mesaClientCondition");
			this.iopClientCondition = new ClientCondition(name, "iopClientCondition");
			this.currentIOCB = new OpieAddress(name, "currentIOCB");
			this.flagsMaskAndDiskState = mkWord(name, "flagsMaskAndDiskState");
			/**/this.recalibrate = mkBoolField("recalibrate", this.flagsMaskAndDiskState, 0x8000);
			/**/this.driveExists = mkBoolField("driveExists", this.flagsMaskAndDiskState, 0x4000);
			/**/this.useEcc = mkBoolField("useEcc", this.flagsMaskAndDiskState, 0x3000);
			/**/this.selectMask = mkField("selectMask", this.flagsMaskAndDiskState, 0x0F00);
			/**/this.diskState = mkField("diskState", this.flagsMaskAndDiskState, 0x00F0);
			/**/this.dcbOffset = mkField("dcbOffset", this.flagsMaskAndDiskState, 0x000F);
			this.devInfo_driveType = mkWord(name, "devInfo.driveType");
			this.devInfo_sectorsPerTrackAndheadsPerCylinder = mkWord(name, "devInfo_sectorsPerTrackAndheadsPerCylinder");
			/**/this.devInfo_sectorsPerTrack = mkField("devInfo.sectorsPerTrack", this.devInfo_sectorsPerTrackAndheadsPerCylinder, 0xFF00);
			/**/this.devInfo_headsPerCylinder = mkField("devInfo.headsPerCylinder", this.devInfo_sectorsPerTrackAndheadsPerCylinder, 0x00FF);
			this.devInfo_cylindersPerDrive = mkByteSwappedWord(name, "devInfo.cylindersPerDrive");
			this.devInfo_reduceWriteCurrentCylinder = mkByteSwappedWord(name, "devInfo.reduceWriteCurrentCylinder");
			this.devInfo_precompensationCylinder = mkByteSwappedWord(name, "devInfo.precompensationCylinder");
		}
	}
	

	private static class FCB implements IORAddress {
		private final int startAddress;
		
		public final TaskContextBlock diskTask;
		public final TaskContextBlock diskDMATask;
		public final IOPCondition conditionDMAWork;
		public final IOPCondition conditionDMADone;
		public final IOPCondition conditionWork;
		public final NotifyMask workMask;
		public final Word lockMask;
		public final IOPBoolean mesaCleanupRequest;
		public final IOPBoolean iopCleanupRequest;
		public final IOPBoolean handlerStoppedForMesa;
		public final IOPBoolean handlerStoppedForIOP;
		public final IOPBoolean handlerStoppedForMesaCleanup;
		public final IOPBoolean handlerStoppedForIOPCleanup;
		public final IOPBoolean startHandlerForMesa;
		public final IOPBoolean startHandlerForIOP;
		public final Word handlerStatecode;
		public final Word clientInfo_currentClientAndclientsToTest;
		/**/public final Field clientInfo_currentClient;
		/**/public final Field clientInfo_clientsToTest;
		public final Word clientInfo_numberOfPossibleClientsAndlastDriveMask;
		/**/public final Field clientInfo_numberOfPossibleClients;
		/**/public final Field clientInfo_lastDriveMask;
		public final ByteSwappedPointer currentDrivePtr;
		public final Word controllerRegisters_word0; // simplified for starters, possibly to be detailled if accessed by Head and important
		public final Word controllerRegisters_word1; // simplified for starters, possibly to be detailled if accessed by Head and important
		public final Word driveInfoAndDMAStatus;  // simplified for starters, possibly to be detailled if accessed by Head and important
		public final Word unexpectedDiskInterruptCount;
		public final Word unexpectedDiskDMAInterruptCount;
		public final DeviceContextBlock dcb0;
		public final DeviceContextBlock dcb1;
		
		public final DeviceContextBlock[] dcb;
		
		
		private FCB() {
			this.startAddress = IORegion.syncToSegment() + IORegion.IOR_BASE;
			
			this.diskTask = new TaskContextBlock(DiskFCB, "diskTask");
			this.diskDMATask = new TaskContextBlock(DiskFCB, "diskDMATask");
			this.conditionDMAWork = new IOPCondition(DiskFCB, "conditionDMAWork");
			this.conditionDMADone = new IOPCondition(DiskFCB, "conditionDMADone");
			this.conditionWork = new IOPCondition(DiskFCB, "conditionWork");
			this.workMask = new NotifyMask(DiskFCB, "workMask");
			this.lockMask = mkWord(DiskFCB, "lockMask");
			this.mesaCleanupRequest = mkIOPBoolean(DiskFCB, "mesaCleanupRequest");
			this.iopCleanupRequest = mkIOPBoolean(DiskFCB, "iopCleanupRequest");
			this.handlerStoppedForMesa = mkIOPBoolean(DiskFCB, "handlerStoppedForMesa");
			this.handlerStoppedForIOP = mkIOPBoolean(DiskFCB, "handlerStoppedForIOP");
			this.handlerStoppedForMesaCleanup = mkIOPBoolean(DiskFCB, "handlerStoppedForMesaCleanup");
			this.handlerStoppedForIOPCleanup = mkIOPBoolean(DiskFCB, "handlerStoppedForIOPCleanup");
			this.startHandlerForMesa = mkIOPBoolean(DiskFCB, "startHandlerForMesa");
			this.startHandlerForIOP = mkIOPBoolean(DiskFCB, "startHandlerForIOP");
			this.handlerStatecode = mkWord(DiskFCB, "handlerState");
			this.clientInfo_currentClientAndclientsToTest = mkWord(DiskFCB, "clientInfo_currentClientAndclientsToTest");
			/**/this.clientInfo_currentClient = mkField("clientInfo.currentClient", this.clientInfo_currentClientAndclientsToTest, 0xFF00);
			/**/this.clientInfo_clientsToTest = mkField("clientInfo.clientsToTest", this.clientInfo_currentClientAndclientsToTest, 0x00FF);
			this.clientInfo_numberOfPossibleClientsAndlastDriveMask = mkWord(DiskFCB, "clientInfo_numberOfPossibleClientsAndlastDriveMask");
			/**/this.clientInfo_numberOfPossibleClients = mkField("clientInfo.numberOfPossibleClients", this.clientInfo_numberOfPossibleClientsAndlastDriveMask, 0xFF00);
			/**/this.clientInfo_lastDriveMask = mkField("clientInfo.lastDriveMask", this.clientInfo_numberOfPossibleClientsAndlastDriveMask, 0x00FF);
			this.currentDrivePtr = new ByteSwappedPointer(DiskFCB, "currentDrivePtr");
			this.controllerRegisters_word0 = mkWord(DiskFCB, "controllerRegisters_word0");
			this.controllerRegisters_word1 = mkWord(DiskFCB, "controllerRegisters_word1");
			this.driveInfoAndDMAStatus = mkWord(DiskFCB, "driveInfoAndDMAStatus");
			this.unexpectedDiskInterruptCount = mkByteSwappedWord(DiskFCB, "unexpectedDiskInterruptCount");
			this.unexpectedDiskDMAInterruptCount = mkByteSwappedWord(DiskFCB, "unexpectedDiskDMAInterruptCount");
			this.dcb0 = new DeviceContextBlock(DiskFCB + ":dcb[0]");
			this.dcb1 = new DeviceContextBlock(DiskFCB + ":dcb[1]");
			this.dcb = new DeviceContextBlock[] { this.dcb0 , this.dcb1 };
			
			this.workMask.byteMaskAndOffset.set(mkMask());
			this.lockMask.set(mkMask());
		}

		@Override
		public String getName() {
			return DiskFCB;
		}

		@Override
		public int getRealAddress() {
			return this.startAddress;
		}
		
	}
	
	private enum HandlerState {
		normalDiskHandlerState(0),
		diskControllerNotIdling(0x100),
		badDiskInterrupt(0x200),
		badDiskDMAInterrupt(0x300),
		DMAerror(0x400),
		resettingDMATask(0x500),
		resettingDiskTask(0x600),
		resettingHandler(0xFFFF);
		
		private final int code;
		private HandlerState(int c) { this.code = c; }
		private int getCode() { return this.code; }
		
	}
	
	/*
	 * Input Output Control Block
	 */
	
	private static class DiskAddress extends IOStruct {
		
		public final Word cylinder;
		public final Word headAndSector;
		/**/public final Field head;
		/**/public final Field sector;

		public DiskAddress(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
			
			this.cylinder = mkWord("cylinder");
			this.headAndSector = mkWord("headAndSector");
			/**/this.head = mkField("head", this.headAndSector, 0xFF00);
			/**/this.sector = mkField("sector", this.headAndSector, 0x00FF);
			
			this.endStruct();
		}
	}
	
	private static class ByteSwappedDiskAddress extends IOStruct {
		
		public final Word cylinder;
		public final Word sectorAndHead;
		/**/public final Field head;
		/**/public final Field sector;

		public ByteSwappedDiskAddress(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
			
			this.cylinder = mkByteSwappedWord("cylinder");
			this.sectorAndHead = mkWord("headAndSector");
			/**/this.sector = mkField("sector", this.sectorAndHead, 0xFF00);
			/**/this.head = mkField("head", this.sectorAndHead, 0x00FF);
			
			this.endStruct();
		}
		
	}
	
	private static class CDF_Operation extends IOStruct {
		
		public final DiskAddress clientHeader; // address of first sector of request
		public final DblWord labelPtr; // LONG POINTER TO Label, -- first label of request.  MUST NOT BE NIL
		public final DblWord dataPtr; // LONG POINTER,  -- first (page aligned) data address of operation
		public final Word opInfo;
		/**/public final BoolField incrementDataPtr;
		/**/public final BoolField enableTrackBuffer;
		/**/public final Field command;
		/**/public final Field tries;
		public final Word pageCount; // sectors remaining for this operation.
		public final Word deviceStatus_a;
		public final Word deviceStatus_b;
		public final DiskAddress diskHeader; // if command.header=Op[read], place the header here.
		public final Word device;

		public CDF_Operation(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
			
			this.clientHeader = new DiskAddress(this, "clientHeader");
			this.labelPtr = mkDblWord("labelPtr");
			this.dataPtr = mkDblWord("dataPtr");
			this.opInfo = mkWord("opInfo");
			/**/this.incrementDataPtr = mkBoolField("incrementDataPtr", this.opInfo, 0x8000);
			/**/this.enableTrackBuffer = mkBoolField("enableTrackBuffer", this.opInfo, 0x4000);
			/**/this.command = mkField("command", this.opInfo, 0x3F00);
			/**/this.tries = mkField("tries", this.opInfo, 0x00FF);
			this.pageCount = mkWord("pageCount");
			this.deviceStatus_a = mkWord("deviceStatus.a");
			this.deviceStatus_b = mkWord("deviceStatus.b");
			this.diskHeader = new DiskAddress(this, "diskHeader");
			this.device = mkWord("device");
			
			this.endStruct();
		}
		
		public void setDeviceStatus(int status) {
			this.deviceStatus_a.set((short)(status >>> 16));
			this.deviceStatus_b.set((short)(status & 0xFFFF));
		}
		
	}
	
//	/** OK :: complete + ready + track00 */
//	private static final int DeviceStatus_OK = 0b0100_0001_0001_0000_0000_0000_0000_0000;
	
	/** OK :: complete + ready */
	private static final int DeviceStatus_OK = 0b0100_0001_0000_0000_0000_0000_0000_0000;
	
	/** Failed :: complete + errorDetected + ready + track00 + protocolViolation */
	private static final int DeviceStatus_FAILED_protocolViolation = 0b0110_0001_1001_0000_0000_0000_0000_0000;
	
	/** Failed :: complete + errorDetected + ready + track00 + sectorNotFound */
	private static final int DeviceStatus_FAILED_sectorNotFound = 0b0110_0001_0001_0000_1000_0000_0000_0000;
	
	/** Failed :: complete + errorDetected + ready + track00 + labelVerifyError */
	private static final int DeviceStatus_FAILED_labelVerifyError = 0b0110_0001_0001_0000_0000_1000_0000_0000;
	
	/** Failed :: complete + errorDetected + ready + track00 + dataVerifyError */
	private static final int DeviceStatus_FAILED_dataVerifyError = 0b0110_0001_0001_0000_0000_0000_1000_0000;
	
	/** Failed :: complete + errorDetected + ready + track00 + illegalCylinder */
	private static final int DeviceStatus_FAILED_illegalCylinder = 0b0110_0001_0011_0000_0000_0000_0000_0000;
	
	private interface ErrorType {
		
		public final short noError = 0;
		
		public final short fifoEmptyAtGetCommandBlock = 1;
		public final short fifoNotEmpty = 3;
		public final short fifoFull = 4;
		public final short fifoEmpty = 5;
		public final short fifoNotEmptyAtLoadCommandBlock = 7;
		public final short lastFIFOError = 0x000F;
		
		public final short firstHeaderError = 0x0010;
		public final short headerAddressMarkNotFound = 0x0011;
		public final short headerIDError = 0x0012;
		public final short headerVerifyError = 0x0013;
		public final short headerCRCorECCError = 0x0014;
		public final short lastHeaderError = 0x001F;
		
		public final short firstLabelError = 0x0020;
		public final short labelAddressMarkNotFound = 0x0021;
		public final short labelIDError = 0x0022;
		public final short labelVerifyError = 0x0023;
		public final short labelCRCorECCError = 0x0024;
		public final short labelCRCAndVerifyError = 0x0025;
		public final short lastLabelError = 0x002F;
		
		public final short firstDataError = 0x0030;
		public final short dataAddressMarkNotFound = 0x0031;
		public final short dataIDError = 0x0032;
		public final short dataVerifyError = 0x0033;
		public final short dataCRCorECCError = 0x0034;
		public final short dataCRCOrECCAndVerifyError = 0x0035;
		public final short lastDataError = 0x003F;
		
		public final short oldSectorNotFound = 0x0080;
		public final short sectorNotFound = 0x0081;
		public final short cylinderTooBig = 0x0082;
		public final short currentCylinderUnknown = 0x0084;
		
		public final short writeFault = 0x0085;
		
		public final short illegalOperation = 0x008A;
		public final short illegalDiagnosticOperation = 0x008B;
		public final short protocolViolation = 0x008C;
		
		public final short last = 0x00FF;
		
	}
	
	private static class DriveAndControllerStatus {
	    // << DriveStatus >>
		public final int notReady = 0x8000;
		public final int notSeekCompleted = 0x4000;
		public final int zero = 0x2000;
		public final int addressMarkOut = 0x1000; //<< Address Mark Detected >>
		public final int notStoredIndexMark = 0x0800;
		public final int notTrack0 = 0x0400;
		public final int notWriteFault = 0x0200;
		public final int lockDetected = 0x0100;
	    
	    // << Controller Status >>
		public final int readDataFound = 0x0080;
		public final int notBDone = 0x0040;
		public final int fifoEmptyAtRead = 0x0020;
		public final int notSPMABit3 = 0x0010;
		public final int notSPMAMaxCount = 0x0008;
		public final int fifoA1B1Same = 0x0004;
		public final int fifoEmptySynchonized = 0x0002;
		public final int fifoFullSynchonized = 0x0001;
		
		public static boolean is(Word w, int what) {
			return (w.get() & what) != 0;
		}
		
		public static void set(Word w, int what) {
			w.set((short)(w.get() | what));
		}
		
		public static void unset(Word w, int what) {
			int inverse = 0xFFFF ^ what;
			w.set((short)(w.get() & inverse));
		}
	}
	
	private interface Operation {
		public final int restore = 0;
		public final int formatTracks = 1;
		public final int readData = 2;
		public final int writeData = 3;
		public final int writeLabelAndData = 4;
		public final int readLabel = 5;
		public final int readLabelAndData = 6;
		public final int verifyData = 7;
		public final int readDiagnostic = 8;
		public final int readTrack = 16;
		public final int last = 0xFF;
	};
	
	private static String operationName(int op) {
		switch(op) {
		case Operation.restore : return "restore";
		case Operation.formatTracks: return "formatTracks";
		case Operation.readData: return "readData";
		case Operation.writeData: return "writeData";
		case Operation.writeLabelAndData: return "writeLabelAndData";
		case Operation.readLabel: return "readLabel";
		case Operation.readLabelAndData: return "readLabelAndData";
		case Operation.verifyData: return "verifyData";
		case Operation.readDiagnostic: return "readDiagnostic";
		case Operation.readTrack: return "readTrack";
		case Operation.last: return "last";
		default: return "invalid(" + op + ")";
		}
	}
	
	private static class CDF_Label extends IOStruct {
		
		public final Word fileID_0;
		public final Word fileID_1;
		public final Word fileID_2;
		public final Word fileID_3;
		public final Word fileID_4;
		/**/public final Word[] fileID;
		public final Word filePageLo;
		public final Word filePageHiAndPageZeroAttributes;
		/**/public final Field filePageHi;
		/**/public final Field pageZeroAttributes;
		public final Word attributesInAllPages;
		public final Word dontCare0;
		public final Word dontCare1;
		
		public CDF_Label(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
			
			this.fileID_0 = mkWord("fileID[0]");
			this.fileID_1 = mkWord("fileID[1]");
			this.fileID_2 = mkWord("fileID[2]");
			this.fileID_3 = mkWord("fileID[3]");
			this.fileID_4 = mkWord("fileID[4]");
			/**/this.fileID = new Word[] { this.fileID_0, this.fileID_1, this.fileID_2, this.fileID_3, this.fileID_4 };
			this.filePageLo = mkWord("filePageLo");
			this.filePageHiAndPageZeroAttributes = mkWord("filePageHiAndPageZeroAttributes");
			/**/this.filePageHi = mkField("filePageHi", this.filePageHiAndPageZeroAttributes, 0xFF00);
			/**/this.pageZeroAttributes = mkField("pageZeroAttributes", this.filePageHiAndPageZeroAttributes, 0x00FF);
			this.attributesInAllPages = mkWord("attributesInAllPages");
			this.dontCare0 = mkWord("dontCare[0]");
			this.dontCare1 = mkWord("dontCare[1]");
			
			this.endStruct();
		}
		
		@Override
		public String toString() {
			return String.format(
				"Label(fileID[ %04X %04X %04X %04X %04X ], filePage+page0attrs[ %04X %04X ], attrsInAllPages[ %04X ], dontCare[ %04X %04X ])", 
				this.fileID_0.get()&0xFFFF, this.fileID_1.get()&0xFFFF, this.fileID_2.get()&0xFFFF, this.fileID_3.get()&0xFFFF, this.fileID_4.get()&0xFFFF,
				this.filePageLo.get()&0xFFFF, this.filePageHiAndPageZeroAttributes.get()&0xFFFF,
				this.attributesInAllPages.get()&0xFFFF,
				this.dontCare0.get()&0xFFFF, this.dontCare1.get()&0xFFFF);
		}
		
		public int getPageNo() {
			int lo = wordSwapBytes(this.filePageLo.get() & 0xFFFF);
			int pageNo = (this.filePageHi.get() << 16) | lo;
			return pageNo;
		}
		
		public void setPageNo(int pageNo) {
			this.filePageLo.set((short)wordSwapBytes(pageNo & 0xFFFF));
			this.filePageHi.set((short)(pageNo >>> 16));
		}
	}
	
	private static class DOB extends IOStruct {
		
		public final Word eccSyndrome_a;
		public final Word eccSyndrome_b;
		public final Word negativeSectorCount; // << INTEGER >>
		public final Word w3;
		/**/public final Field sectorsPerTrack;
		public final Word w4;
		/**/public final Field headsPerCylinder;
		/**/public final Field currentVersion;
		public final Word cylindersPerDrive; // << CARDINAL >>
		public final Word w6;
		/**/public final Field startingSectorOnTrack;
		public final Word reducedWriteCylinder; // << CARDINAL >>
		public final Word preCompensationCylinder; // << CARDINAL >>
		public final Word w9;
		/**/public final Field writeEndCount;
		public final Word headerError;
		public final Word labelError;
		public final Word dataError;
		public final Word lastError;
		public final Word currentCylinder;
		public final Word w15;
		/**/public final Field eccFlag;
		public final ByteSwappedDiskAddress header;
		public final Word sectorValid; // <<ARRAY OF BOOLEAN>>
		public final Word reserved2;
		public final Word driveAndControllerStatus;
		/**/public final BoolField notReady;
		/**/public final BoolField notSeekCompleted;
		/**/public final Field zero;
		/**/public final BoolField addressMarkOut;
		/**/public final BoolField notStoredIndexMark;
		/**/public final BoolField notTrack0;
		/**/public final BoolField notWriteFault;
		/**/public final BoolField lockDetected;
		/**/public final BoolField readDataFound;
		/**/public final BoolField notBDone;
		/**/public final BoolField fifoEmptyAtRead;
		/**/public final BoolField notSPMABit3;
		/**/public final BoolField notSPMAMaxCount;
		/**/public final BoolField fifoA1B1Same;
		/**/public final BoolField fifoEmptySynchonized;
		/**/public final BoolField fifoFullSynchonized;
		public final Word w21;
		/**/public final Field operation;
		public final Word negativeFormatTrackCount;
		public final CDF_Label label;
		
		public DOB(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
			
			this.eccSyndrome_a = mkWord("eccSyndrome.a");
			this.eccSyndrome_b = mkWord("eccSyndrome.b");
			this.negativeSectorCount = mkByteSwappedWord("negativeSectorCount");
			this.w3 = mkWord("word3");
			/**/this.sectorsPerTrack = mkField("sectorsPerTrack", this.w3, 0xFF00);
			this.w4 = mkWord("word4");
			/**/this.headsPerCylinder = mkField("headsPerCylinder", this.w4, 0xFF00);
			/**/this.currentVersion = mkField("currentVersion", this.w4, 0x00FF);
			this.cylindersPerDrive = mkByteSwappedWord("cylindersPerDrive");
			this.w6 = mkWord("word6");
			/**/this.startingSectorOnTrack = mkField("startingSectorOnTrack", this.w6, 0xFF00);
			this.reducedWriteCylinder = mkByteSwappedWord("reducedWriteCylinder");
			this.preCompensationCylinder = mkByteSwappedWord("preCompensationCylinder");
			this.w9 = mkWord("word9");
			/**/this.writeEndCount = mkField("writeEndCount", this.w9, 0xFF00);
			this.headerError = mkByteSwappedWord("headerError"); // -> high-byte comes last -> matches ErrorType
			this.labelError = mkByteSwappedWord("labelError"); // dito...
			this.dataError = mkByteSwappedWord("dataError"); // dito...
			this.lastError = mkByteSwappedWord("lastError"); // dito...
			this.currentCylinder = mkByteSwappedWord("currentCylinder");
			this.w15 = mkWord("word5");
			/**/this.eccFlag = mkField("eccFlag", this.w15, 0xFF00);
			this.header = new ByteSwappedDiskAddress(this, "header");
			this.sectorValid = mkByteSwappedWord("sectorValid");
			this.reserved2 = mkWord("reserved2");
			this.driveAndControllerStatus = mkWord("driveAndControllerStatus");
			/**/this.notReady = mkBoolField("notReady", this.driveAndControllerStatus, 0x8000);
			/**/this.notSeekCompleted = mkBoolField("notSeekCompleted", this.driveAndControllerStatus, 0x4000);
			/**/this.zero = mkField("zero", this.driveAndControllerStatus, 0x2000);
			/**/this.addressMarkOut = mkBoolField("addressMarkOut", this.driveAndControllerStatus, 0x1000);
			/**/this.notStoredIndexMark = mkBoolField("notStoredIndexMark", this.driveAndControllerStatus, 0x0800);
			/**/this.notTrack0 = mkBoolField("notTrack0", this.driveAndControllerStatus, 0x0400);
			/**/this.notWriteFault = mkBoolField("notWriteFault", this.driveAndControllerStatus, 0x0200);
			/**/this.lockDetected = mkBoolField("lockDetected", this.driveAndControllerStatus, 0x0100);
			/**/this.readDataFound = mkBoolField("readDataFound", this.driveAndControllerStatus, 0x0080);
			/**/this.notBDone = mkBoolField("notBDone", this.driveAndControllerStatus, 0x0040);
			/**/this.fifoEmptyAtRead = mkBoolField("fifoEmptyAtRead", this.driveAndControllerStatus, 0x0020);
			/**/this.notSPMABit3 = mkBoolField("notSPMABit3", this.driveAndControllerStatus, 0x0010);
			/**/this.notSPMAMaxCount = mkBoolField("notSPMAMaxCount", this.driveAndControllerStatus, 0x0008);
			/**/this.fifoA1B1Same = mkBoolField("fifoA1B1Same", this.driveAndControllerStatus, 0x0004);
			/**/this.fifoEmptySynchonized = mkBoolField("fifoEmptySynchonized", this.driveAndControllerStatus, 0x0002);
			/**/this.fifoFullSynchonized = mkBoolField("fifoFullSynchonized", this.driveAndControllerStatus, 0x0001);
			this.w21 = mkWord("word21");
			/**/this.operation = mkField("operation", this.w21, 0xFF00);
			this.negativeFormatTrackCount = mkByteSwappedWord("negativeFormatTrackCount");
			this.label = new CDF_Label(this, "label");
			
			this.endStruct();
		}
	}
	
	private static final String DiskIOCB = "DiskIOCB";
	
	private static class IOCB extends IOStruct {
		
		// << Operation. >>
		public final CDF_Operation op;
		
		// << Head only information. >>
		public final Word mesaNext; // IOCBPtr
		public final OpieAddress iopNext;
		public final Word labelOpInfo;
		/**/public final Field type; // {normal, restore, labelFixup}, <<Set by InitIOCB for Poll>>
		/**/public final Field labelFixupType; // {none, readLabel, fixed, verifyErrorExpected}
		/**/public final Field labelFixupTry;
		public final Word tryNo; // "try" is reserved in Java...
		public final Word command;
		public final Word runLength;
		public final Word pageLocalization;
		public final Word preRestored; // BOOLEAN
		private final Word[] filler = new Word[10];
		public final Word useBuffer; // BOOLEAN
		public final Word bufferHit; // BOOLEAN
		public final Word mapEntry;
		
		// << Handler information. >>
		public final OpieAddress dataPtr;
		public final Word flags1;
		/**/public final BoolField dataCommandTransfer; // BOOLEAN
		/**/public final BoolField dataCommandDirection_isToMesa; // {fromMesa,toMesa}
		/**/public final BoolField incrementDataPtr; // BOOLEAN
		/**/public final BoolField complementDOB; // BOOLEAN
		/**/public final Field etch_isEtch2; // {etch1(0), etch2(1)} ¬ etch1
		/**/public final BoolField useLEDs; // BOOLEAN
		/**/public final BoolField halt; // BOOLEAN
		/**/public final BoolField diagnosticCommand; // BOOLEAN
		public final Word pageCount;
		public final Word word47B;
		/**/public final IOPBoolean stopHandlerOnCompletion;
		/**/public final IOPBoolean onlyDOBFromController;
		public final Word word50B;
		/**/public final IOPBoolean error;
		/**/public final IOPBoolean diskOperationBlockError;
		public final Word word51B;
		/**/public final Field controllerErrorType;
		/**/public final Field dmaErrorType;
		public final Word word52B;
		/**/public final IOPBoolean complete;
		/**/public final IOPBoolean inProgress;
		public final Word word53B;
		/**/public final Field dataTransferDirection;
		/**/public final IOPBoolean dmaTimedOut;
		public final OpieAddress nextIOCB;
		
		// << Handler & Controller information. >>
		
		// << Controller information. >>
		public final DOB dob;
		

		public IOCB(int base) {
			super(base, DiskIOCB);
			
			this.op = new CDF_Operation(this, "op");
			
			this.mesaNext = mkWord("mesaNext");
			this.iopNext = new OpieAddress(this, "iopNext");
			this.labelOpInfo = mkWord("labelOpInfo");
			/**/this.type = mkField("type", this.labelOpInfo, 0xC000);
			/**/this.labelFixupType = mkField("labelFixupType", this.labelOpInfo, 0x3000);
			/**/this.labelFixupTry = mkField("labelFixupTry", this.labelOpInfo, 0x0FFF);
			this.tryNo = mkWord("try");
			this.command = mkWord("command");
			this.runLength = mkWord("runLength");
			this.pageLocalization = mkWord("pageLocalization");
			this.preRestored = mkWord("preRestored");
			for (int i = 0; i < this.filler.length; i++) { this.filler[i] = mkWord("filler[" + i + "]"); }
			this.useBuffer = mkWord("useBuffer");
			this.bufferHit = mkWord("bufferHit");
			this.mapEntry = mkWord("mapEntry");
			
			this.dataPtr = new OpieAddress(this, "dataPtr");
			this.flags1 = mkWord("flags1"); // mkByteSwappedWord("flags1"); // mkWord("flags1");
			/**/this.dataCommandTransfer = mkBoolField("dataCommandTransfer", this.flags1, 0x8000);
			/**/this.dataCommandDirection_isToMesa = mkBoolField("dataCommandDirection_toMesa", this.flags1, 0x0100);
			/**/this.incrementDataPtr = mkBoolField("incrementDataPtr", this.flags1, 0x0080);
			/**/this.complementDOB = mkBoolField("complementDOB", this.flags1, 0x0040);
			/**/this.etch_isEtch2 = mkField("etch_isEtch2", this.flags1, 0x0020);
			/**/this.useLEDs = mkBoolField("useLEDs", this.flags1, 0x0004);
			/**/this.halt = mkBoolField("halt", this.flags1, 0x0002);
			/**/this.diagnosticCommand = mkBoolField("diagnosticCommand", this.flags1, 0x0001);
			this.pageCount = mkByteSwappedWord("pageCount");
			this.word47B = mkWord("word47B");
			/**/this.stopHandlerOnCompletion = mkIOPShortBoolean("stopHandlerOnCompletion", this.word47B, true);
			/**/this.onlyDOBFromController = mkIOPShortBoolean("onlyDOBFromController", this.word47B, false);
			this.word50B = mkWord("word50B");
			/**/this.error = mkIOPShortBoolean("error", this.word50B, true);
			/**/this.diskOperationBlockError = mkIOPShortBoolean("diskOperationBlockError", this.word50B, false);
			this.word51B = mkWord("word51B");
			/**/this.controllerErrorType = mkField("controllerErrorType", this.word51B, 0xFF00);
			/**/this.dmaErrorType = mkField("dmaErrorType", this.word51B, 0x00FF);
			this.word52B = mkWord("word52B");
			/**/this.complete = mkIOPShortBoolean("complete", this.word52B, true);
			/**/this.inProgress = mkIOPShortBoolean("inProgress", this.word52B, false);
			this.word53B = mkWord("word53B");
			/**/this.dataTransferDirection = mkField("dataTransferDirection", this.word53B, 0xFF00);
			/**/this.dmaTimedOut = mkIOPShortBoolean("dmaTimedOut", this.word53B, false);
			this.nextIOCB = new OpieAddress(this, "nextIOCB");
			
			this.dob = new DOB(this, "dob");
		}
		
	}
	
	/*
	 * statistical data
	 */
	
	private int reads = 0;
	private int writes = 0;
	
	public int getReads() { return this.reads; }
	
	public int getWrites() { return this.writes; }
	
	/*
	 * implementation of the iop6085 disk interface
	 */
	
	/** operation on the label */
	public enum VerifyLabelOp {
		/** check disk label against value provided by Pilot (probably expected bahiour) */
		verify,
		/** if the disk label does not match the value provided by Pilot, update the label on disk */
		updateDisk,
		/** do not check labels when required by the disk operation (readData, writeData, verifyData) */
		noVerify
	};
	
	private final FCB fcb;
	
	private final IOCB workIocb = new IOCB(0); // will be rebased to access a specific IOCB
	
	private final VerifyLabelOp labelOpOnRead;
	private final VerifyLabelOp labelOpOnWrite;
	private final VerifyLabelOp labelOpOnVerify;
	private final boolean logLabelProblems;
	
	public HDisk(VerifyLabelOp labelOpOnRead, VerifyLabelOp labelOpOnWrite, VerifyLabelOp labelOpOnVerify, boolean logLabelProblems) {
		super(DiskFCB, Config.IO_LOG_DISK);
		this.labelOpOnRead = labelOpOnRead;
		this.labelOpOnWrite = labelOpOnWrite;
		this.labelOpOnVerify = labelOpOnVerify;
		this.logLabelProblems = logLabelProblems;
		
		// allocate Function Context Block
		this.fcb = new FCB();
		
		// initialize the fcb and the dcb(s) according to the disk(s) registered up to now
		this.fcb.mesaCleanupRequest.set(false);
		this.fcb.iopCleanupRequest.set(false);
		this.fcb.handlerStoppedForMesa.set(false);
		this.fcb.handlerStoppedForIOP.set(false);
		this.fcb.handlerStoppedForMesaCleanup.set(false);
		this.fcb.handlerStoppedForIOPCleanup.set(false);
		this.fcb.startHandlerForMesa.set(false);
		this.fcb.startHandlerForIOP.set(false);
		this.fcb.handlerStatecode.set((short)0);
		// ?? this.fcb.clientInfo_currentClient.set(...)
		// ?? this.fcb.clientInfo_clientsToTest.set(...)
		// ?? this.fcb.clientInfo_lastDriveMask.set(...)
		// ?? this.fcb.clientInfo_numberOfPossibleClients.set(...)
		for (int i = 0; i < MAX_DISKS; i++) {
			DeviceContextBlock dcb = this.fcb.dcb[i];
			if (i >= diskFiles.size()) {
				dcb.driveExists.set(false);
				dcb.diskState.set(0); // not disk at this index
			} else {
				DiskFile disk = diskFiles.get(i);
				dcb.driveExists.set(true);
				dcb.recalibrate.set(false);
				dcb.selectMask.set(driveSelectMasks[i]);
				dcb.useEcc.set(false);
				dcb.diskState.set(3); // ready
				dcb.dcbOffset.set(i * 26); // a DCB is 26 words long
				dcb.devInfo_driveType.set(anyPilotDisk);
				dcb.devInfo_sectorsPerTrack.set(DiskFile.sectorsPerTrack);
				dcb.devInfo_headsPerCylinder.set(disk.headCount);
				dcb.devInfo_cylindersPerDrive.set((short)disk.cylCount);
				dcb.devInfo_reduceWriteCurrentCylinder.set((short)disk.cylCount);
				dcb.devInfo_precompensationCylinder.set((short)disk.cylCount);
			}
		}
	}

	@Override
	public int getFcbRealAddress() {
		return this.fcb.getRealAddress();
	}

	@Override
	public short getFcbSegment() {
		return this.fcb.getIOPSegment();
	}
	
	private static boolean dumpMap = false; // for debugging: dump the VM-map for the IO region (1st bank = addr 0x0000..0xFFFF)
	
	private static int wordSwapBytes(int val) {
		val &= 0xFFFF;
		int res = (val << 8) | (val >>> 8);
		return res & 0xFFFF;
	}
	
	private static boolean doAltLogs = false;
	
	private static void altlogf(String format, Object... args) {
		if (doAltLogs) { System.out.printf(format, args); }
	}

	@Override
	public boolean processNotify(short notifyMask) {
		// check if it's for us
		if (notifyMask != this.fcb.workMask.byteMaskAndOffset.get()) {
			return false;
		}
		
		this.slogf("\n");
		this.logf("IOP::HDisk.processNotify() - begin (insn: %d)\n", Cpu.insns);
		this.logf("IOP::HDisk.processNotify() : currentClient = %d , clientsToTest = %d\n",
				this.fcb.clientInfo_currentClient.get(),
				this.fcb.clientInfo_clientsToTest.get());
		this.logf("IOP::HDisk.processNotify() : handlerStoppedForMesaCleanup = %s , mesaCleanupRequest = %s\n",
				Boolean.toString(this.fcb.handlerStoppedForMesaCleanup.get()),
				Boolean.toString(this.fcb.mesaCleanupRequest.get()));
		this.logf("IOP::HDisk.processNotify() : handlerStoppedForMesa = %s , startHandlerForMesa = %s\n",
				Boolean.toString(this.fcb.handlerStoppedForMesa.get()),
				Boolean.toString(this.fcb.startHandlerForMesa.get()));
		
		if (dumpMap) {
			Mem.dumpVmMap(0, 256);
			dumpMap = false;
		}
		
		// check for cleanup request
		if (this.fcb.mesaCleanupRequest.get()) {
			this.logf("IOP::HDisk.processNotify() -> mesaCleanupRequest\n");
			this.logf("IOP::HDisk.processNotify() - done\n\n");
			altlogf("IOP::HDisk.processNotify() -> mesaCleanupRequest\n");
			this.fcb.handlerStoppedForMesaCleanup.set(true);
			return true;
		}
		this.fcb.handlerStoppedForMesaCleanup.set(false);
		
		// handle (re)start handler request
		boolean forceProtocolViolation = false;
		if (this.fcb.startHandlerForMesa.get()) {
			this.fcb.handlerStoppedForMesa.set(false);
			this.fcb.startHandlerForMesa.set(false);
			this.logf("IOP::HDisk.processNotify() -> (re)started handler\n");
		} else if (this.fcb.handlerStoppedForMesa.get()) {
			forceProtocolViolation = true;
			this.logf("IOP::HDisk.processNotify() -> invoked stopped handler -> force protocolViolation on all IOCBs\n");
		}
		this.fcb.handlerStatecode.set((short)0); // = normalDiskHandlerState
		this.fcb.unexpectedDiskInterruptCount.set((short)0);
		this.fcb.unexpectedDiskDMAInterruptCount.set((short)0);
		// ?? this.fcb.driveInfoAndDMAStatus.set(...);
		// ?? this.fcb.controllerRegisters_word0.set(...);
		// ?? this.fcb.controllerRegisters_word1.set(...);
		
		// process IOCBs for the disks
		for (int dcbNo = 0; dcbNo < MAX_DISKS; dcbNo++) {
			if (!this.fcb.dcb[dcbNo].driveExists.is()) { continue; } // disk not present

			DiskFile disk = diskFiles.get(dcbNo);
			if (disk == null) {
				continue; // this disk is not present...
			}
			
			// process IOCBs registered for this drive
			int iocbPtr = this.fcb.dcb[dcbNo].mesaHead.get() & 0xFFFF; // ?? iopHead 
			int iopIocbPtr = this.fcb.dcb[dcbNo].iopHead.toLP();
			boolean allowInterrupt = false;
			
			this.logf("IOP::HDisk.processNotify() -> dcb %d :: mesaHead: 0x%08X , iopHead: 0x%08X , recalibrate: %5s , diskCommand: %s\n",
					dcbNo, iocbPtr, iopIocbPtr, Boolean.toString(this.fcb.dcb[dcbNo].recalibrate.is()), getDiskCommandString(this.fcb.dcb[dcbNo].diskCommand.get()));
			altlogf("\nIOP::HDisk.processNotify() -> dcb %d :: mesaHead: 0x%08X , iopHead: 0x%08X , recalibrate: %5s , diskCommand: %s\n",
					dcbNo, iocbPtr, iopIocbPtr, Boolean.toString(this.fcb.dcb[dcbNo].recalibrate.is()), getDiskCommandString(this.fcb.dcb[dcbNo].diskCommand.get()));
			
			while (iocbPtr != 0) {
				iocbPtr = wordSwapBytes(iocbPtr);
				this.logf("IOP::HDisk.processNotify() -> dcb %d, processing IOCB 0x%08X\n", dcbNo, iocbPtr);
				
				allowInterrupt = true; // as we have seen an IOCB
				
				this.workIocb.rebaseToVirtualAddress(iocbPtr);
				
//				if (this.workIocb.complementDOB.is() || this.workIocb.halt.is() || this.workIocb.diagnosticCommand.is()) {
//					String tmp = this.workIocb.complementDOB.is() ? " complementDOB" : "";
//					tmp += this.workIocb.halt.is() ? " halt" : "";
//					tmp += this.workIocb.diagnosticCommand.is() ? " diagnosticCommand" : "";
//					System.out.printf(">>>>> HDisk->SPECIAL-FLAGS:%s >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n", tmp);
//				}
				
				// prepare error state members
				boolean failed = false;
				this.workIocb.dmaTimedOut.set(false);
				this.workIocb.dob.headerError.set(ErrorType.noError);
				this.workIocb.dob.labelError.set(ErrorType.noError);
				this.workIocb.dob.dataError.set(ErrorType.noError);
				this.workIocb.dob.lastError.set(ErrorType.noError);
				this.workIocb.dob.driveAndControllerStatus.set((short)0);
				this.workIocb.diskOperationBlockError.set(false);
				this.workIocb.op.setDeviceStatus(DeviceStatus_OK);
				
				// check for invoking the handler while stopped
				if (forceProtocolViolation) {
					this.workIocb.dob.lastError.set(ErrorType.protocolViolation);
					this.workIocb.diskOperationBlockError.set(true);
					this.workIocb.op.setDeviceStatus(DeviceStatus_FAILED_protocolViolation);
					
					this.workIocb.inProgress.set(false);
					this.workIocb.complete.set(true);
					this.workIocb.error.set(true);
					this.workIocb.diskOperationBlockError.set(false);
					logf("IOP::HDisk.processNotify() -> handler invoked while stopped, aborting IOCB with error: protocolViolation !!!\n");
					
					iocbPtr = this.workIocb.mesaNext.get() & 0xFFFF;
					iopIocbPtr = this.workIocb.nextIOCB.toLP();
					logf("IOP::HDisk.processNotify() -> mesaNext: 0x%08X , iopNext: 0x%08X\n", iocbPtr, iopIocbPtr);
					continue;
				}
				
				// get operation data
				int vDataPtr = this.workIocb.dataPtr.toLP();
				int pageCount = this.workIocb.pageCount.get();
				int negativeSectorCount = this.workIocb.dob.negativeSectorCount.get();
				boolean incrementDataPtr = this.workIocb.incrementDataPtr.is();
				int operation = this.workIocb.dob.operation.get();
				logf("IOP::HDisk.processNotify()    -> command = %s , dataPtr = 0x%08X , pageCount = %d , negativeSectorCount = %d , incrementDataPtr = %s\n",
						operationName(operation), vDataPtr, pageCount, negativeSectorCount, Boolean.toString(incrementDataPtr));
				
				// special case 'recalibrate'
				if (operation == Operation.restore) {
					altlogf(">>>>> HDisk->restore (recalibrate) >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
					logf("IOP::HDisk.processNotify()    -> restore (recalibrate) => ignored (iocb complete, no error)\n");
					this.workIocb.complete.set(true);
					this.workIocb.error.set(false);
					iocbPtr = this.workIocb.mesaNext.get() & 0xFFFF;
					iopIocbPtr = this.workIocb.nextIOCB.toLP();
					logf("IOP::HDisk.processNotify() -> mesaNext: 0x%08X , iopNext: 0x%08X\n", iocbPtr, iopIocbPtr);
					continue;
				}
				
				// get disk coordinates to work with
				int cyl = this.workIocb.dob.header.cylinder.get() & 0xFFFF;
				int head = this.workIocb.dob.header.head.get();
				int sector = this.workIocb.dob.header.sector.get();
				boolean useBuffer = this.workIocb.useBuffer.get() != 0;
				int absSectorIdx;
				int lastCylSectIdx;
				try {
					absSectorIdx = disk.getLinearSector(cyl, head, sector);
					lastCylSectIdx = disk.getLinearSector(cyl, disk.headCount-1, disk.sectorsPerTrack-1);
				} catch(Exception e) {
					logf("IOP::HDisk.processNotify()    -> cylinder = %d , head = %d , sector = %d , absSectorIdx = INVALID , useBuffer = %s, ABORTING\n",
							 cyl, head, sector, Boolean.toString(useBuffer));
					
					this.workIocb.dob.lastError.set(ErrorType.sectorNotFound);
					this.workIocb.diskOperationBlockError.set(true);
					this.workIocb.op.setDeviceStatus(DeviceStatus_FAILED_sectorNotFound);
					
					this.workIocb.error.set(true);
					this.workIocb.complete.set(true);
					
					iocbPtr = this.workIocb.mesaNext.get() & 0xFFFF; // ?? iopNext
					continue; // try with next IOCB
				}
				int lastAbsSectIdx = absSectorIdx;
				logf("IOP::HDisk.processNotify()    -> cylinder = %d , head = %d , sector = %d , absSectorIdx = %d, useBuffer = %s\n",
					 cyl, head, sector, absSectorIdx, Boolean.toString(useBuffer));
				
				// disallow track-buffer usage (does not seem to be used anyway, but...)
				// (in the faint hope that the disk operation will be repeated without buffer usage)
				if (useBuffer) {
					logf("IOP::HDisk.processNotify() -> IOCB.useBuffer is not supported, aborting IOCB !!!\n");

					this.workIocb.bufferHit.set((short)0);
					this.workIocb.diskOperationBlockError.set(true);
					this.workIocb.dob.lastError.set(ErrorType.illegalOperation);
					this.workIocb.op.setDeviceStatus(DeviceStatus_FAILED_protocolViolation);
					this.workIocb.error.set(true);
					this.workIocb.complete.set(true);
					
					iocbPtr = this.workIocb.mesaNext.get() & 0xFFFF; // ?? iopNext
					continue; // try with next IOCB
				}
				
				// handle operation requested
				String opName = operationName(operation);
				altlogf(">> HDisk->%s: absSectorIdx = %d [ %d / %d / %d ] , pageCount = %d , incrDataPtr = %s , vMem = [ 0x%06X .. 0x%06X )\n",
						opName, absSectorIdx, cyl, head, sector, pageCount, incrementDataPtr, vDataPtr, vDataPtr + (pageCount*256));
				switch(operation) {
				
					// general disk i/o operations with the following functions on the sector-components:
					case Operation.readData:            // header: verify, label: verify, data: read
					case Operation.readLabel:           // header: verify, label: read,   data: -
					case Operation.readLabelAndData:    // header: verify, label: read,   data: read
					case Operation.writeData:           // header: verify, label: verify, data: write
					case Operation.writeLabelAndData:   // header: verify, label: write,  data: write
					case Operation.verifyData: {        // header: verify, label: verify, data: verify
						
						altlogf("   -- at start:  %s\n", this.workIocb.dob.label.toString());
						
						// save possibly changed label data for subsequent restore
						short lo = this.workIocb.dob.label.filePageLo.get();
						short hi = this.workIocb.dob.label.filePageHiAndPageZeroAttributes.get();
						boolean restoreLabelPage = false;
						
						int labelPageNoBase = this.workIocb.dob.label.getPageNo();
						int currPageIdx = 0;
						
						// process the sector components for the requested number of sectors
						while(pageCount > 0) {
							
							// check for a (still) valid disk position
							if (absSectorIdx >= disk.sectorCount) {
								// put the error in the iocb/dob ...
								this.workIocb.dob.lastError.set(ErrorType.sectorNotFound);
								this.workIocb.diskOperationBlockError.set(true);
								this.workIocb.op.setDeviceStatus(DeviceStatus_FAILED_sectorNotFound);
								
								System.out.printf("!! sector index out of range\n");
								
								logf("IOP::HDisk.processNotify() -> %s: sectorNotFound\n", operationName(operation));
								failed = true;
								break;
							}
							
							// set the label page number for label verify or write
							// (the label content will be overwritten on 'readLabel' or 'readLabelAndData', so we can ignore the current operation)
							this.workIocb.dob.label.setPageNo(labelPageNoBase + currPageIdx);
							
							if (operation == Operation.readData) {
								
								this.reads++;
								
								failed = this.doLabelVerification(disk, absSectorIdx, this.labelOpOnRead, "readData", cyl, head, sector);
								disk.readSectorData(absSectorIdx, vDataPtr);
								
							} else if (operation == Operation.readLabel) {
								
								this.reads++;
								
								disk.readSectorLabel(absSectorIdx, this.workIocb.dob.label);
								
							} else if (operation == Operation.readLabelAndData) {
								
								this.reads++;
								
								disk.readSectorLabel(absSectorIdx, this.workIocb.dob.label);
								disk.readSectorData(absSectorIdx, vDataPtr);
								
							} else if (operation == Operation.writeData) {
	
								this.writes++;
								
								failed = this.doLabelVerification(disk, absSectorIdx, this.labelOpOnWrite, "writeData", cyl, head, sector);
								
								disk.writeSectorData(absSectorIdx, vDataPtr);
								
							} else if (operation == Operation.writeLabelAndData) {
	
								this.writes++;
								
								disk.writeSectorLabel(absSectorIdx, this.workIocb.dob.label);
								disk.writeSectorData(absSectorIdx, vDataPtr);
								
							} else  { // this can only be: Operation.verifyData
								
								this.reads++;
								
								failed = this.doLabelVerification(disk, absSectorIdx, this.labelOpOnVerify, "verifyData", cyl, head, sector);
								
								if (!disk.verifySectorData(absSectorIdx, vDataPtr)) {
									// not same sector data => put the error in the iocb/dob and abort...
									this.workIocb.dob.labelError.set(ErrorType.dataVerifyError);
									this.workIocb.dob.lastError.set(ErrorType.dataVerifyError);
									this.workIocb.diskOperationBlockError.set(true);
									// ** .op is not for handler .. this.workIocb.op.setDeviceStatus(DeviceStatus_FAILED_dataVerifyError);
									
//									System.out.printf("!! verifyData-error: dataVerifyError\n");
									
									logf("IOP::HDisk.processNotify() -> verifyData-error: dataVerifyError\n");
									failed = true;
								}
								
							}
							
							// move to next sector
							disk.linearToDiskAddress(absSectorIdx, this.workIocb.dob.header);
							currPageIdx++;
							absSectorIdx++;
							pageCount--;
							negativeSectorCount++;
							if (incrementDataPtr) {
								vDataPtr += PrincOpsDefs.WORDS_PER_PAGE;
								this.workIocb.dataPtr.fromLP(vDataPtr);
							}
						}
						
						altlogf("   -- at end  :  %s\n\n", this.workIocb.dob.label.toString());
						
						// restore possibly changed label data
						if (restoreLabelPage) {
							this.workIocb.dob.label.filePageLo.set(lo);
							this.workIocb.dob.label.filePageHiAndPageZeroAttributes.set(hi);
						}
					
						break;
					}
					
					// format operation, never seen so far...
					case Operation.formatTracks: {
						
						logf("IOP::HDisk.processNotify() -> formatTracks: negativeFormatTrackCount = %d\n",
								this.workIocb.dob.negativeFormatTrackCount.get() & 0xFFFF);
						
						if (cyl >= disk.cylCount) {
							// put the error in the iocb/dob ...
							this.workIocb.dob.lastError.set(ErrorType.cylinderTooBig);
							this.workIocb.diskOperationBlockError.set(true);
							this.workIocb.op.setDeviceStatus(DeviceStatus_FAILED_illegalCylinder);
							
							logf("IOP::HDisk.processNotify() -> formatTracks-error: cylinderTooBig\n");
							failed = true;
							break;
						}
						
						// ?? trackCount: really tracks (i.e. headCount tracks per cylinder) or whole cylinders ??
						// lets stick with the name "track" => one track == sectorsPerTrack sectors => headCount tracks per cylinder 
						int remainingTracks = 0 - (this.workIocb.dob.negativeFormatTrackCount.get() & 0xFFFF);
						int currCyl = cyl;
						int currHead = 0;
						int linearSector = disk.getLinearSector(currCyl, 0, 0);
						short[] zeros = new short[256]; // should be allocated as all zeros, used for label and data in sector
						while (remainingTracks > 0 && currCyl < disk.cylCount) {
							for (int sect = 0; sect < DiskFile.sectorsPerTrack; sect++) {
								disk.writeSectorLabelAndDataRaw(linearSector++, zeros, zeros);
							}
							remainingTracks--;
							currHead++;
							if (currHead >= disk.headCount) {
								currCyl++;
								currHead = 0;
							}
						}
						
						this.workIocb.dob.negativeFormatTrackCount.set((short)(0 - remainingTracks));
						if (remainingTracks > 0) {
							// put the error in the iocb/dob ...
							this.workIocb.dob.lastError.set(ErrorType.cylinderTooBig);
							this.workIocb.diskOperationBlockError.set(true);
							this.workIocb.op.setDeviceStatus(DeviceStatus_FAILED_illegalCylinder);
							
							logf("IOP::HDisk.processNotify() -> formatTracks-error: cylinderTooBig\n");
							failed = true;
						}
						
						logf("IOP::HDisk.processNotify() -> formatTracks done\n");
						break;
					}
					
					case Operation.readDiagnostic: {
						// TODO: put "unsupported operation" somewhere into iocb/dob
						// for now: abort execution for now to see if and when it happens
						throw new IllegalArgumentException("HDisk::HDisk.processNotify() -> Operation.readDiagnostic not supported");
						
						// TODO: break;
					}
					
					case Operation.readTrack: {
						// TODO: put "unsupported operation" somewhere into iocb/dob
						// for now: abort execution for now to see if and when it happens
						throw new IllegalArgumentException("HDisk::HDisk.processNotify() -> Operation.readTrack not supported");
						
						// TODO: break;
					}
					
					default:
						// TODO: put "invalid operation" somewhere into iocb/dob
						// for now: abort execution for now to see if and when it happens
						throw new IllegalArgumentException("HDisk::HDisk.processNotify() -> Operation ( invalid code = " + operation + " ) not supported");
						
						// TODO: break;
				
				}
				
				// update status/counter fields in IOCB
				this.workIocb.complete.set(true);
				this.workIocb.error.set(failed);
				this.workIocb.pageCount.set((short)pageCount);
				this.workIocb.dob.negativeSectorCount.set((short)negativeSectorCount);
				this.workIocb.dob.currentCylinder.set((short)(((absSectorIdx > lastCylSectIdx) ? cyl + 1 : cyl) & 0xFFFF));
				this.workIocb.dob.sectorValid.set((short)0xFFFF); // all sectors are valid...
				this.workIocb.dob.notReady.set(false);
				this.workIocb.dob.notSeekCompleted.set(false);
				this.workIocb.dob.addressMarkOut.set(true);     // << Address Mark Detected >>
				this.workIocb.dob.notStoredIndexMark.set(false);
				this.workIocb.dob.notTrack0.set(cyl != 0 || head != 0);
				this.workIocb.dob.notWriteFault.set(true);
				this.workIocb.dob.lockDetected.set(false);
				this.workIocb.dob.readDataFound.set(true);
				this.workIocb.dob.notBDone.set(false);        // ?? bDone true, but what is this?
				this.workIocb.dob.fifoEmptyAtRead.set(false); // ??
				this.workIocb.dob.notSPMABit3.set(false);     // ??
				this.workIocb.dob.notSPMAMaxCount.set(false); // ??
				this.workIocb.dob.fifoA1B1Same.set(false);    // ??
				this.workIocb.dob.fifoEmptySynchonized.set(false);// ??
				this.workIocb.dob.fifoFullSynchonized.set(false); // ??
				
				logf("IOP::HDisk.processNotify() -> completed IOCB, error = %s [ pageCount = %d , negativesectorCount = %d , dataPtr = 0x%06X ]\n",
						Boolean.toString(failed), this.workIocb.pageCount.get(), this.workIocb.dob.negativeSectorCount.get(), this.workIocb.dataPtr.toLP());
				
				// possibly stop the handler
				if (failed || this.workIocb.stopHandlerOnCompletion.get()) {
					this.fcb.handlerStoppedForMesa.set(true);
					logf("IOP::HDisk.processNotify() -> stopped (cause: %s)\n", failed ? "failed" : "workIocb.stopHandlerOnCompletion");
				}
				
				// done with this IOCB, get next IOCB in list
				iocbPtr = this.workIocb.mesaNext.get() & 0xFFFF;
				iopIocbPtr = this.workIocb.iopNext.toLP();
				logf("IOP::HDisk.processNotify() -> mesaNext: 0x%08X , iopNext: 0x%08X\n", iocbPtr, iopIocbPtr);
			}
			
			// raise interrupt to inform Pilot about the end of the i/o-operations for this FCB
			if (allowInterrupt) {
				short intrMask = this.fcb.dcb[dcbNo].mesaClientCondition.maskValue.get();
				logf("IOP::HDisk.processNotify() -> dcb %d processed, raising interrupt 0x%04X\n", dcbNo, intrMask);
				Processes.requestMesaInterrupt(intrMask);
			}
		}
		
		// done
		this.logf("IOP::HDisk.processNotify() - done\n\n");
		return true;
	}
	
	private boolean /* label verify failed */ doLabelVerification(DiskFile disk, int absSectorIdx, VerifyLabelOp op, String diskOperation, int cyl, int head, int sector) {
		if (!disk.verifySectorLabel(absSectorIdx, this.workIocb.dob.label)) {
			if (op == VerifyLabelOp.verify || absSectorIdx == 0) {
				
				// put the error in the iocb/dob ...
				this.workIocb.dob.labelError.set(ErrorType.labelVerifyError);
				this.workIocb.dob.lastError.set(ErrorType.labelVerifyError);
				this.workIocb.diskOperationBlockError.set(true);
			
				if (this.logLabelProblems) {
					System.out.printf("!! %s-error: labelVerifyError on absSectorIdx = %d [ %d / %d / %d ]\n", diskOperation, absSectorIdx, cyl, head, sector);
					System.out.printf("   -- expected:  %s\n", this.workIocb.dob.label.toString());
					System.out.printf("   -- sect-lbl:  %s\n", disk.getLabelString(absSectorIdx));
				}
				logf("IOP::HDisk.processNotify() -> %s-error: labelVerifyError\n", diskOperation);
				
				return true;
				
			} else if (op == VerifyLabelOp.updateDisk) {
				
				if (this.logLabelProblems) {
					System.out.printf("!! %s: updating label on absSectorIdx = %d [ %d / %d / %d ]\n", diskOperation, absSectorIdx, cyl, head, sector);
					System.out.printf("   --  old(disk):  %s\n", disk.getLabelString(absSectorIdx));
					System.out.printf("   -- new(Pilot):  %s\n", this.workIocb.dob.label.toString());
				}
				disk.writeSectorLabel(absSectorIdx, this.workIocb.dob.label);
				
			}
		}
		
		return false;
	}

	@Override
	public void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue) {
		// disks do not synchronize access to fcb data (queue or the like) ?
		if (lockMask == this.fcb.lockMask.get()) {
			this.logf("IOP::HDisk.handleLockmem() ... ??\n");
		}
	}

	@Override
	public void handleLockqueue(int vAddr, int rAddr) {
		// not relevant for disk handler
		this.logf("IOP::HDisk.handleLockqueue() ... ??\n");
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		// disk operations are synchronous, so nothing to transfer to/from mesa memory at synchronization points
	}
	
	/*
	 * disk file management functions for the main program
	 */
	
	private static final List<DiskFile> diskFiles = new ArrayList<>();
	
	// add disk file
	
	public static boolean addFile(String filePath, boolean readonly, int deltasToKeep, StringBuilder sb) {
		if (diskFiles.size() >= MAX_DISKS) {
			logWarning(sb, "IOP::HDisk.addFile :: only %d disk currently supported, ignored disk file: %s", MAX_DISKS, filePath);
			return false;
		}
		
		File f = new File(filePath);
		if (!f.exists() && !readonly && !f.canWrite()) {
			Cpu.ERROR("IOP::HDisk.addFile :: file not found or not writable: " + filePath);
		}
		
		// create DiskFile and append to files
		try {
			if (Config.IO_LOG_DISK) {
				Cpu.logInfo("IOP::HDisk.addFile :: adding file '" + filePath + "'");
			}
			DiskFile diskfile = new DiskFile(f, readonly, deltasToKeep);
			diskFiles.add(diskfile);
			return true;
		} catch(DiskFileCorrupted dfc) {
			logWarning(sb, "delta file corrupt, loaded disk possibly unusable");
			return false;
		}
	}
	
	// shutdown => save disk file(s)
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		logf("shutdown\n");
		for (DiskFile f : diskFiles) {
			f.saveDisk(errMsgTarget);
		}
	}
	
	// merge disk base+delta files

	public static void mergeDisks(PrintStream ps) {
		for (DiskFile df : diskFiles) {
			ps.printf("Merging disk: %s\n", df.f.getName());
			try {
				df.mergeDelta(ps);
				ps.printf("Done merging disk: %s\n", df.f.getName());
			} catch(IOException e) {
				ps.printf("!! failed, due to: %s\n", e.getMessage());
			}
		}
	}
	
	public static int getAbsSectNo(int diskIdx, int cyl, int head, int sector) {
		if (diskIdx < 0 || diskIdx >= diskFiles.size()) {
			throw new IllegalArgumentException("Invalid disk index given");
		}
		DiskFile df = diskFiles.get(diskIdx);
		return df.getLinearSector(cyl, head, sector);
	}
	
	public static void rawRead(int diskIdx, int absSector, short[] label, short[] data) {
		if (diskIdx < 0 || diskIdx >= diskFiles.size()) {
			throw new IllegalArgumentException("Invalid disk index given");
		}
		DiskFile df = diskFiles.get(diskIdx);
		df.readSectorLabelAndDataRaw(absSector, label, data);
	}
	
	private static void logWarning(StringBuilder sb, String pattern, Object... args) {
		String msg = (args.length > 0) ? String.format(pattern, args) : pattern;
		Cpu.logError(msg);
		sb.append(msg).append("\n");
	}
	
	/*
	 * ***** in-memory representation of an 6085/Daybreak disk 
	 */
	
	public static class DiskFileCorrupted extends Exception {
		private static final long serialVersionUID = 6099209261004177132L;
		private DiskFileCorrupted() { super(); }
		private DiskFileCorrupted(String msg) { super(msg); }
	}
	
	private static class DiskFile {
		
		// structure of an externally stored disk file:
		// - header: 6 words (#cylinder/#heads/16 <-> #totalSectorCount must match!):
		//     signature1 , #heads , #cylinder , #totalSectorCount(dbl-word) , signature2
		// - sectors in ascending order by cylinder,head,sector
		//     1 dbl-word linear sector-pos , 10 word header , 256 words data
		// (all compressed as a zip stream, all (dbl-)words as big-endian (most significant bytes come first)
		// (same format for full/delta files: a full-file has all sectors, a delta only the changed sectors)
		
		private static final int signature1 = 0xDAAD;
		private static final int signature2 = 0x5CC5;
		
		// the header for a sector is ignored resp. saved, as it can be regenerated from the sector address if needed
		public static final int wordsForSectorLabel = 10;
		public static final int wordsPerSectorData = 256;
		public static final int wordsPerSector = wordsForSectorLabel + wordsPerSectorData;
		
		// we save 266 words per sector, here are the offsets of the components:
		public static final int offsetLabel = 0;
		public static final int offsetData = wordsForSectorLabel;
		
		// one track has 16 sectors => with at most 16 heads: 128 data-KByte == 64 data-KWords
		public static final int sectorsPerTrack = 16; // -> 8 data-KByte
		public static final int maxHeadCount = 16;    // -> 16 .. 128 data-KBytes per cylinder
		public static final int wordsPerTrack = sectorsPerTrack * wordsPerSector;
		
		// min. 40 cylinders => 5 data-MBytes
		public static final int minCylCount = 40;

		// extensions for disk image files
		public static final String EXT_ZDISK = ".zdisk";
		public static final String EXT_DELTA = ".zdelta";
		public static final String EXT_TEMP_DELTA = ".temp_zdelta";
		
		// the file information for this emulated disk
		private final File f;
		private final boolean readonly;
		private final int deltasToKeep;
		
		// constant disk geometry
		private final int cylCount;
		public final int headCount;
		public final int sectorsPerCyl;
		private final int wordsPerCylinder;
		private final int sectorCount;
		
		// disk content
		private final short[][] sectors; // for each sector: label + data
		private final boolean[] sectorsChanged; // which sectors must be written to a delta file
		private boolean changed = false; // has the disk been changed at all?
		
		// temp sector content buffer for persistence i/o
		private final byte[] sectorBuffer = new byte[wordsPerSector * 2];
		
		// local logging function
		private void logf(String template, Object... args) {
			if (Config.IO_LOG_DISK) {
				System.out.printf("DiskFile[" + f.getName() + "]: " + template, args);
			}
		}
		private void logf(StringBuilder sb, String template, Object... args) {
			String msg = String.format(template,  args);
			if (sb.length() > 0) { sb.append("\n"); }
			sb.append(msg);
			logf(msg);
		}
		
		// create a new disk
		private DiskFile(int cylinderCount, int headCount, File f) throws IOException {
			if (cylinderCount < minCylCount) {
				throw new IllegalArgumentException("a new disk must have at least 40 cylinders");
			}
			this.f = f;
			this.cylCount = cylinderCount;
			this.headCount = headCount;
			this.sectorsPerCyl = sectorsPerTrack * this.headCount;
			this.wordsPerCylinder = wordsPerTrack * this.headCount;
			this.readonly = false;
			this.deltasToKeep = 4;
			
			this.sectorCount = this.cylCount * this.sectorsPerCyl;
			this.sectors = new short[this.sectorCount][];
			for (int i = 0; i < this.sectorCount; i++) {
				this.sectors[i] = new short[wordsPerSector];
			}
			this.sectorsChanged = new boolean[this.sectorCount];
			
			wLog(" ... begin writeDiskFileContent\n");
			this.writeDiskFileContent(this.f, true);
			wLog(" ... end writeDiskFileContent\n");
		}

		// open an existing disk
		private DiskFile(File f, boolean readonly, int deltasToKeep) throws DiskFileCorrupted {
			this.f = f;
			this.readonly = readonly;
			this.deltasToKeep = deltasToKeep;
			
			try {
				// read full file
				try ( FileInputStream fis = new FileInputStream(f);
					  InflaterInputStream iis = new InflaterInputStream(fis)) {
					int sig1 = readWord(iis);
					int heads = readWord(iis);
					int cyls = readWord(iis);
					int sects = (readWord(iis) << 16) | readWord(iis);
					int sig2 = readWord(iis);
					int expectedSects = cyls * heads * sectorsPerTrack;
					if (sig1 != signature1 || cyls < minCylCount || sects != expectedSects || sig2 != signature2) {
						throw new DiskFileCorrupted();
					}
					
					this.cylCount = cyls;
					this.headCount = heads;
					this.sectorsPerCyl = sectorsPerTrack * this.headCount;
					this.wordsPerCylinder = wordsPerTrack * this.headCount;
					this.sectorCount = sects;
					this.sectors = new short[this.sectorCount][];
					for (int i = 0; i < this.sectorCount; i++) {
						this.sectors[i] = new short[wordsPerSector];
					}
					this.sectorsChanged = new boolean[this.sectorCount];
					
					this.readDiskFile(iis, false);
				}
				
				// update with delta, if available
				String deltaname = f.getPath() + EXT_DELTA;
				File delta = new File(deltaname);
				if (delta.exists()) {
					try ( FileInputStream fis = new FileInputStream(delta);
						  InflaterInputStream iis = new InflaterInputStream(fis)) {
						int sig1 = readWord(iis);
						int heads = readWord(iis);
						int cyls = readWord(iis);
						int sects = (readWord(iis) << 16) | readWord(iis);
						int sig2 = readWord(iis);
						int expectedSects = cyls * heads * sectorsPerTrack;
						if (sig1 != signature1 || cyls < minCylCount || sects != expectedSects || sig2 != signature2) {
							throw new DiskFileCorrupted();
						}
						if (cyls != this.cylCount || heads != this.headCount || sects != this.sectorCount) {
							throw new DiskFileCorrupted("delta file geometry does not match main file geometry");
						}
						
						this.readDiskFile(iis, true);
					}
				}
				
			} catch(IOException ioe) {
				throw new DiskFileCorrupted();
			}
		}
		
		private void readRawSector(InputStream i) throws IOException {
			int pos = 0;
			int remaining = this.sectorBuffer.length;
			while(remaining > 0) {
				int bytesRead = i.read(this.sectorBuffer, pos, remaining);
				remaining -= bytesRead;
				pos += bytesRead;
			}
		}
		
		private void readDiskFile(InputStream i, boolean isDelta) throws DiskFileCorrupted {
			System.out.printf("reading %s disk file: ", isDelta ? "delta" : "base");
			int currSectNo = 0;
			int totalBytes = 12; // disk file prefix size 
			while(true) {
				// get the absolute sector number
				final int absSector;
				try { // EOF while reading the intro for the next sector is OK
					absSector = (readWord(i) << 16) | readWord(i);
				} catch(DiskFileCorrupted dfc) {
					break;
				}
				if (absSector < 0 || absSector > this.sectorCount) {
					throw new DiskFileCorrupted();
				}
				if (isDelta) {
					this.sectorsChanged[absSector] = true;
					this.changed = true;
				}
				
				// get the sector content (label + data)
				short[] rawSector = this.sectors[absSector];
				try {
					this.readRawSector(i);
					totalBytes += this.sectorBuffer.length;
				} catch (IOException e) {
					throw new DiskFileCorrupted();
				}
				int b = 0;
				for (int w = 0; w < wordsPerSector; w++) {
					int b1 = this.sectorBuffer[b++] & 0x00FF;
					int b2 = this.sectorBuffer[b++] & 0x00FF;
					rawSector[w] = (short)((b1 << 8) | b2);
				}
				currSectNo++;
			}
			System.out.printf("loaded %d bytes for %d sectors\n", totalBytes, currSectNo);
		}
		
		private static int readWord(InputStream i) throws DiskFileCorrupted {
			try {
				int b1 = i.read();
				int b2 = i.read();
				if (b1 < 0 || b2 < 0) {
					throw new DiskFileCorrupted();
				}
				return ((b1 << 8) | b2) & 0xFFFF;
			} catch (IOException e) {
				throw new DiskFileCorrupted();
			}
		}
		
		private void writeDiskFileContent(File f, boolean asFullfile) throws IOException {
			int sectorsWritten = 0;
			int bytesWritten = 0;
			try ( FileOutputStream fos = new FileOutputStream(f);
				  DeflaterOutputStream dos = new DeflaterOutputStream(fos)
				) {
				// write disk file header
				writeWord(dos, signature1);
				writeWord(dos, this.headCount);
				writeWord(dos, this.cylCount);
				writeDblWord(dos, this.sectorCount);
				writeWord(dos, signature2);
				bytesWritten = 12;
				
				// write sectors
				for (int i = 0; i < this.sectorCount; i++) {
					if (asFullfile || this.sectorsChanged[i]) {
						short[] rawSector = this.sectors[i];
						writeDblWord(dos, i);
						int b = 0;
						for (int w = 0; w < wordsPerSector; w++) {
							//writeWord(dos, rawSector[w]);
							short word = rawSector[w];
							this.sectorBuffer[b++] = (byte)((word >> 8) & 0xFF);
							this.sectorBuffer[b++] = (byte)(word & 0xFF); 
						}
						dos.write(this.sectorBuffer);
						bytesWritten  += this.sectorBuffer.length;
						sectorsWritten++;
					}
				}
				dos.finish();
			}
			System.out.printf("writeDiskFileContent() -> %d bytes written for %d sectors\n", bytesWritten, sectorsWritten);
		}
		
		// write an 32-bit integer to a output stream as big-endian
		private static void writeWord(OutputStream o, int val) throws IOException {
			o.write((val >> 8) & 0xFF);
			o.write(val & 0xFF);
		}
		
		// write an 32-bit integer to a output stream as big-endian
		private static void writeDblWord(OutputStream o, int val) throws IOException {
			o.write((val >> 24) & 0xFF);
			o.write((val >> 16) & 0xFF);
			o.write((val >> 8) & 0xFF);
			o.write(val & 0xFF);
		}
		
		public boolean saveDisk(StringBuilder errors) {
			// checks for change
			if (this.readonly) {
				this.logf(errors, "disk is read only, no delta written");
				return false;
			}
			if (!this.changed) {
				this.logf(errors, "disk is not changed, no delta written");
				return false;
			}
			
			// write new delta to temp file
			String deltatempname = f.getPath() + EXT_TEMP_DELTA;
			File deltatemp = new File(deltatempname);
			if (deltatemp.exists()) { deltatemp.delete(); }
			logf("writing temp delta to %s\n", deltatempname);
			try {
				this.writeDiskFileContent(deltatemp, false);
			} catch (IOException e) {
				this.logf("failed to write (temp) delta-file: %s\n", e.getMessage());
				return false;
			}
			
			// do the housekeeping on delta files
			String deltaname = f.getPath() + EXT_DELTA;
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
			return true;
		}
		
		public void mergeDelta(PrintStream ps) throws IOException {
			// check for a delta
			String deltaname = this.f.getPath() + EXT_DELTA;
			File delta = new File(deltaname);
			if (!delta.exists()) {
				ps.printf("No delta found for disk '%s', nothing to merge\n", f.getName());
				return;
			}
			
			// copy the disk and delta files into an zip archive
			File dir = this.f.getParentFile();
			String filterFnStart = delta.getName() + "-";
			File[] deltas = dir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File file, String fn) {
					return fn.startsWith(filterFnStart);
				}
			});
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss.SSS");
			String zipName = f.getPath() + "-" + sdf.format(new Date()) + ".zip";
			ps.printf("Creating archive: %s\n", zipName);
			try (FileOutputStream fos = new FileOutputStream(zipName); ZipOutputStream zos = new ZipOutputStream(fos)) {
				addToZip(this.f, zos, ps);
				for (File df : deltas) {	
					addToZip(df, zos, ps);
				}
				addToZip(delta, zos, ps);
			}
			
			// remove the deltas
			delta.delete();
			for (File df : deltas) {	
				df.delete();
			}
			
			// write back the full disk file
			ps.printf("Writing full disk file for: %s\n", f.getName());
			this.writeDiskFileContent(this.f, true);
			ps.printf("Done writing full disk file for: %s\n", f.getName());
		}
		
		private void addToZip(File file, ZipOutputStream zos, PrintStream ps) throws FileNotFoundException, IOException {
			ps.printf("... adding: %s\n", file.getName());
			
			ZipEntry zipEntry = new ZipEntry(file.getName());
			zos.putNextEntry(zipEntry);

			try (FileInputStream fis = new FileInputStream(file)) {
				byte[] buffer= new byte[1024];
				int length = fis.read(buffer);
				while (length >= 0) {
					zos.write(buffer, 0, length);
					length = fis.read(buffer);
				}
			}

			zos.closeEntry();
		}
		
		/*
		 * primitive disk i/o operations, for direct use and assembling "higher level" operations
		 * like format, readDataAndLabel etc.
		 */
		
		public int getLinearSector(int cyl, int head, int sector) {
			if (cyl < 0 || cyl > this.cylCount || head < 0 || head > this.headCount || sector < 0 || sector > sectorsPerTrack) {
				throw new IllegalArgumentException("invalid cyl/head/sector: " + cyl + "/" + head + "/" + sector);
			}
			return (cyl * this.sectorsPerCyl) + (head * sectorsPerTrack) + sector;
		}
		
		public int getLinearSector(DiskAddress da) {
			return this.getLinearSector(da.cylinder.get() & 0xFFFF, da.head.get() & 0xFFFF, da.sector.get() & 0xFFFF);
		}
		
		public int getLinearSector(ByteSwappedDiskAddress da) {
			return this.getLinearSector(da.cylinder.get() & 0xFFFF, da.head.get() & 0xFFFF, da.sector.get() & 0xFFFF);
		}
		
		public void linearToDiskAddress(int linearSector, ByteSwappedDiskAddress da) {
//			System.out.printf("-- disk.linearToDiskAddress(%d)\n", linearSector);
			int cyl = linearSector / (this.headCount * sectorsPerTrack);
			int cylRelative = linearSector - (cyl * this.headCount * sectorsPerTrack);
			int head = cylRelative / sectorsPerTrack;
			int sect = cylRelative % sectorsPerTrack;
			if (linearSector != getLinearSector(cyl, head, sect)) { throw new IllegalStateException("linearSector != getLinearSector(cyl, head, sect)"); }
			da.cylinder.set((short)cyl);
			da.head.set(head);
			da.sector.set(sect);
		}
		
		// assuming that linearSector is valid!
		public int /* ErrorType */ readSectorData(int linearSector, int virtualLongPointer) {
			try {
				int realPtr = Mem.getRealAddress(virtualLongPointer, true); // check that the start address is mapped and writable
				Mem.getRealAddress(virtualLongPointer + PrincOpsDefs.WORDS_PER_PAGE - 1, true); // check that end address is also OK
//				System.out.printf("----> virtual 0x%06X => real 0x%06X\n", virtualLongPointer, realPtr);
			} catch (Exception e) {
				System.out.printf("***** unable to access vPtr = 0x%06X :: %s\n", virtualLongPointer, e.getMessage());
				throw e;
			}
			short[] rawSector = this.sectors[linearSector];
			for (int i = offsetData; i < rawSector.length; i++, virtualLongPointer++) {
				Mem.writeWord(virtualLongPointer, rawSector[i]);
			}
			return ErrorType.noError;
		}
		
		// assuming that linearSector is valid!
		public int /* ErrorType */ readSectorLabel(int linearSector, CDF_Label label) {
			short[] rawSector = this.sectors[linearSector];
			int sectorWord = offsetLabel;
			label.fileID_0.set(rawSector[sectorWord++]);
			label.fileID_1.set(rawSector[sectorWord++]);
			label.fileID_2.set(rawSector[sectorWord++]);
			label.fileID_3.set(rawSector[sectorWord++]);
			label.fileID_4.set(rawSector[sectorWord++]);
			label.filePageLo.set(rawSector[sectorWord++]);
			label.filePageHiAndPageZeroAttributes.set(rawSector[sectorWord++]);
			label.attributesInAllPages.set(rawSector[sectorWord++]);
			label.dontCare0.set(rawSector[sectorWord++]);
			label.dontCare1.set(rawSector[sectorWord++]);
			return ErrorType.noError;
		}
		
		// assuming that linearSector is valid!
		public boolean /* same? */ verifySectorData(int linearSector, int virtualLongPointer) {
			Mem.getRealAddress(virtualLongPointer, false); // check that the start address is mapped and readable
			Mem.getRealAddress(virtualLongPointer + PrincOpsDefs.WORDS_PER_PAGE - 1, false); // check that end address is also OK
			short[] rawSector = this.sectors[linearSector];
			for (int i = offsetData; i < rawSector.length; i++, virtualLongPointer++) {
				if (Mem.readWord(virtualLongPointer) != rawSector[i]) { return false; }
			}
			return true;
		}
		
		// assuming that linearSector is valid!
		public boolean /* same? */ verifySectorLabel(int linearSector, CDF_Label label) {
			short[] rawSector = this.sectors[linearSector];
			
			/*
			 * see: APilot/15.0.1/Faces/Private/CompatibilityDiskFace.mesa, lines 58..62 ::
			 * 
			 *     When verifying labels, the following fields must match and must be
			 *     the same in every page of a run of pages:
			 *         fileID and attributesInAllPages
			 *     The field pageZeroAttributes must match in page zero of a file and
			 *     must be zero in every other page of a file.
			 *     
			 */
			
			if (rawSector[offsetLabel+6] == 0 && (rawSector[offsetLabel+7] & 0xFF00) == 0
					&& (label.filePageHiAndPageZeroAttributes.get() & 0x00FF) != (rawSector[offsetLabel+7] & 0x00FF)) {
				return false;
			}

			int sectorWord = offsetLabel;
			return label.fileID_0.get() == rawSector[sectorWord++]
				&& label.fileID_1.get() == rawSector[sectorWord++]
				&& label.fileID_2.get() == rawSector[sectorWord++]
				&& label.fileID_3.get() == rawSector[sectorWord++]
				&& label.fileID_4.get() == rawSector[sectorWord++]
				&& sectorWord++ > 0 // skip: filePageLo
				&& sectorWord++ > 0 // skip: filePageHiAndPageZeroAttributes
				&& label.attributesInAllPages.get() == rawSector[sectorWord++]
				                    // ignore: dontCare0 and dontCare1
				;
		}
		
		// assuming that linearSector is valid!
		public int /* ErrorType */ writeSectorData(int linearSector, int virtualLongPointer) {
			Mem.getRealAddress(virtualLongPointer, false); // check that the start address is mapped and readably
			Mem.getRealAddress(virtualLongPointer + PrincOpsDefs.WORDS_PER_PAGE - 1, false); // check that end address is also OK
			short[] rawSector = this.sectors[linearSector];
			for (int i = offsetData; i < rawSector.length; i++, virtualLongPointer++) {
				rawSector[i] = Mem.readWord(virtualLongPointer);
			}
			this.changed = true;
			this.sectorsChanged[linearSector] = true;
			return ErrorType.noError;
		}
		
		// assuming that linearSector is valid!
		public int /* ErrorType */ writeSectorLabel(int linearSector, CDF_Label label) {
			short[] rawSector = this.sectors[linearSector];
			int sectorWord = offsetLabel;
			rawSector[sectorWord++] = label.fileID_0.get();
			rawSector[sectorWord++] = label.fileID_1.get();
			rawSector[sectorWord++] = label.fileID_2.get();
			rawSector[sectorWord++] = label.fileID_3.get();
			rawSector[sectorWord++] = label.fileID_4.get();
			rawSector[sectorWord++] = label.filePageLo.get();
			rawSector[sectorWord++] = label.filePageHiAndPageZeroAttributes.get();
			rawSector[sectorWord++] = label.attributesInAllPages.get();
			rawSector[sectorWord++] = label.dontCare0.get();
			rawSector[sectorWord++] = label.dontCare1.get();
			this.changed = true;
			this.sectorsChanged[linearSector] = true;
			return ErrorType.noError;
		}
		
		public int /* ErrorType */ readSectorLabelAndDataRaw(int linearSector, short[] label, short[] data) {
			if (label == null || label.length < 10) {
				throw new IllegalArgumentException("invalid label (not 10 words)");
			}
			if (data == null || data.length < 256) {
				throw new IllegalArgumentException("invalid sector data (not 256 words)");
			}
			
			short[] rawSector = this.sectors[linearSector];
			
			int sectorWord = offsetLabel;
			for (int i = 0; i < 10; i++) {
				label[i] = rawSector[sectorWord++]; 
			}
			
			sectorWord = offsetData;
			for (int i = 0; i < 256; i++) {
				data[i] = rawSector[sectorWord++];
			}
			
			return ErrorType.noError;
		}
		
		public int /* ErrorType */ writeSectorLabelAndDataRaw(int linearSector, short[] label, short[] data) {
			if (label == null || label.length != 10) {
				throw new IllegalArgumentException("invalid label (not 10 words)");
			}
			if (data == null || data.length != 256) {
				throw new IllegalArgumentException("invalid sector data (not 256 words)");
			}
			
			short[] rawSector = this.sectors[linearSector];
			
			int sectorWord = offsetLabel;
			for (int i = 0; i < 10; i++) {
				rawSector[sectorWord++] = label[i];
			}
			
			sectorWord = offsetData;
			for (int i = 0; i < 256; i++) {
				rawSector[sectorWord++] = data[i];
			}
			
			this.changed = true;
			this.sectorsChanged[linearSector] = true;
			
			return ErrorType.noError;
		}
		
		/*
		 * debugging support
		 */
		
		private String getLabelString(int linearSector) {
			short[] rawSector = this.sectors[linearSector];
			int sectorWord = offsetLabel;
			return String.format(
					"Label(fileID[ %04X %04X %04X %04X %04X ], filePage+page0attrs[ %04X %04X ], attrsInAllPages[ %04X ], dontCare[ %04X %04X ])", 
					rawSector[sectorWord++]&0xFFFF, rawSector[sectorWord++]&0xFFFF, rawSector[sectorWord++]&0xFFFF,rawSector[sectorWord++]&0xFFFF, rawSector[sectorWord++]&0xFFFF,
					rawSector[sectorWord++]&0xFFFF, rawSector[sectorWord++]&0xFFFF,
					rawSector[sectorWord++]&0xFFFF,
					rawSector[sectorWord++]&0xFFFF, rawSector[sectorWord++]&0xFFFF);
		}
		
		private void dumpSector(PrintStream ps, int linearSector, int cyl, int head, int sector, short[] rawData) {
			int checkLinear = this.getLinearSector(cyl, head, sector);
			ps.printf("\nsector: [ linear: %d , cyl: %d , head: %d , sector: %d ]\n", linearSector, cyl, head, sector);
			if (linearSector != checkLinear) {
				ps.printf("** deviating recomputation: checkLinear = %d\n", checkLinear);
			}
			ps.printf("label: 0x %04X %04X %04X %04X %04X %04X %04X %04X %04X %04X\n",
					rawData[0], rawData[1], rawData[2], rawData[3], rawData[4],
					rawData[5], rawData[6], rawData[7], rawData[8], rawData[9]);
			ps.printf("sector content:");
			StringBuilder sb = new StringBuilder();
			int cnt = 0;
			for (int i = 10; i < 266; i++) {
				if ((cnt % 16) == 0) {
					ps.printf("  %s\n", sb.toString());
					sb.setLength(0);
					ps.printf(" 0x%02X:", cnt);
				}
				cnt++;
				int w = rawData[i] & 0xFFFF;
				ps.printf(" %04X", w);
				int b1 = w >>> 8;
				if (b1 >= 32 && b1 < 127) { sb.append((char)b1); } else { sb.append("."); }
				int b2 = w & 0x00FF;
				if (b2 >= 32 && b2 < 127) { sb.append((char)b2); } else { sb.append("."); }
			}
			ps.printf("  %s\n", sb.toString());
		}
		
		public void dumpCylinders(PrintStream ps, int cylFirst, int cylCount) {
			int linear = this.getLinearSector(cylFirst, 0, 0);
			int cylLimit = Math.min(cylFirst + cylCount, this.cylCount);
			for (int cyl = cylFirst; cyl < cylLimit; cyl++) {
				for (int head = 0; head < this.headCount; head++) {
					for (int sect = 0; sect < sectorsPerTrack; sect++) {
						short[] rawSector = this.sectors[linear];
						this.dumpSector(ps, linear++, cyl, head, sect, rawSector);
					}
				}
			}
		}
		
	}
	
	/*
	 * temp code for transforming a pair of sector data and metadata files somehow
	 * regenerated from an emulated or real disk into out own format
	 */
	
	private static short rWord(InputStream i) throws IOException {
		int b2 = i.read() & 0x00FF;
		if (b2 < 0) { throw new IllegalStateException("unexpected premature end"); }
		int b1 = i.read() & 0x00FF;
		if (b1 < 0) { throw new IllegalStateException("unexpected premature end"); }
		return (short)(( (b1 << 8) | b2 ) & 0xFFFF);
	}
	
	private static void wLog(String format, Object... args) {
		System.out.printf(format, args);
	}
	
	public static void inactive_main(String[] args) throws IOException, DiskFileCorrupted {
		final String path = "/home/.../";
		final String sectFile = path + "test.disk";
		final String metaFile = sectFile + ".metadata";
		final String diskFile = path + "xde5.0" + DiskFile.EXT_ZDISK;
		
		// these 2 values must be found in the file information
		final int heads = 8;
		final int cyls = 960;
		
		final File zdiskFile = new File(diskFile);
		final DiskFile disk;
		final boolean saveDisk;
		
		if (zdiskFile.exists() && zdiskFile.canRead()) {
			// load existing disk
			wLog("loading disk file\n");
			disk = new DiskFile(zdiskFile, false, 32);
			saveDisk = false;
			wLog("done loadiing disk file\n");
		} else {
			// create new disk with matching geometry and load the xde5.0 disk into it
			wLog("begin creating disk file\n");
			disk = new DiskFile(cyls, heads, zdiskFile);
			saveDisk = true;
			wLog("end creating disk file\n");
			
			final short[] label = new short[10];
			final short[] sector = new short[256];
			
			// read the sector components from the 2 files
			try (
					InputStream iData = new BufferedInputStream(new FileInputStream(sectFile));
					InputStream iMeta = new BufferedInputStream(new FileInputStream(metaFile));
			) {
				// for all cylinders, heads/cylinder, sector/head
				wLog("Loading cylinders");
				for (int cyl = 0; cyl < cyls; cyl++) {
					if ((cyl % 64) == 0) { wLog("\n"); }
					wLog(".");
					for (int head = 0; head < heads; head++) {
						//wLog("reading track(%d,%d) ... ", cyl, head);
						for (int sect = 0; sect < 16; sect++) {
							
							// read label content
							for (int w = 0; w < label.length; w++) {
								label[w] = rWord(iMeta);
							}
							// read sector content
							for (int w = 0; w < sector.length; w++) {
								sector[w] = rWord(iData);
							}
							
							// fill sector
							int linearSector = disk.getLinearSector(cyl, head, sect);
							//if (sect == 0) {
							//	wLog("writing at linear %d .", linearSector);
							//} else {
							//	wLog(".");
							//}
							if (disk.writeSectorLabelAndDataRaw(linearSector, label, sector) != ErrorType.noError) {
								throw new IllegalStateException(String.format("write sector(%d,%d,%d) failed", cyl, head, sect));
							}
						}
						//wLog(" done\n");
					}
				}
				wLog("\n");
			}
		}
		
		// dump the disk content to hex
		try (FileOutputStream fos = new FileOutputStream(diskFile + ".hexdump");
			 BufferedOutputStream bos = new BufferedOutputStream(fos);
			 PrintStream ps = new PrintStream(bos);
			) {
			disk.dumpCylinders(ps, 0, disk.cylCount);
		}
		
		// create the new disk file
		if (saveDisk) {
			StringBuilder errors = new StringBuilder();
			if (!disk.saveDisk(errors)) {
				wLog("\nfailed to write disk: %s\n", errors.toString());
			}
		}
		
	}
	
}