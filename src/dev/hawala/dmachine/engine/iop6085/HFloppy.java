/*
Copyright (c) 20192020, Dr. Hans-Walter Latz
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
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkField;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkIOPBoolean;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkIOPShortBoolean;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkWord;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.ClientCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.IOPCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.NotifyMask;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.OpieAddress;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.QueueBlock;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.TaskContextBlock;
import dev.hawala.dmachine.engine.iop6085.IORegion.BoolField;
import dev.hawala.dmachine.engine.iop6085.IORegion.DblWord;
import dev.hawala.dmachine.engine.iop6085.IORegion.Field;
import dev.hawala.dmachine.engine.iop6085.IORegion.IOPBoolean;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;
import dev.hawala.dmachine.engine.iop6085.IORegion.IOStruct;
import dev.hawala.dmachine.engine.iop6085.IORegion.Word;

/**
 * IOP device handler for the floppy drive of a Daybreak/6085 machine.
 * <p>
 * Current restrictions: only IMD and DMK floppies converted from real 6085 5,25" (360K) floppies
 * can be mounted, floppies are always read-only.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class HFloppy extends DeviceHandler {
	
	/*
	 * Function Control Block
	 */
	
	private static final String FloppyFCB = "FloppyFCB";
	
	
	private static class FDF_Attributes {
		
		public final Word type; // Device.Type, --type of drive (defined in DeviceTypes)
		public final Word numberOfCylinders; // CARDINAL, --Number of cylinders available for recording on the drive connected to the controller.
		public final Word numberOfHeadsAndSectors;
		/**/public final Field numberOfHeads; // --Number of read/write heads available for recording on the drive connected to the controller.
		/**/public final Field maxSectorsPerTrack; // --Maximum number of sectors per track (based on context setting)
		public final Word formatLength; // --Length in words of the buffer that the client must supply in order to format a track.
		public final Word flags;
		/**/public final BoolField ready; // BOOLEAN, --Whether drive is ready (contains a diskette)
		/**/public final BoolField diskChange; // BOOLEAN, --Whether drive has gone not ready since last successful operation (use DiskChangeClear to reset)
		/**/public final BoolField twoSided; // BOOLEAN, --(If ready=TRUE) Whether diskette currently installed has data on both sides
		/**/public final BoolField busy; // BOOLEAN, --Whether drive is busy (operation in progress)
		
		private FDF_Attributes(String name) {
			this.type = mkWord(name, "type");
			this.numberOfCylinders = mkWord(name, "numberOfCylinders");
			this.numberOfHeadsAndSectors = mkWord(name, "numberOfHeadsAndSectors");
			/**/this.numberOfHeads = mkField("numberOfHeads", this.numberOfHeadsAndSectors, 0xFF00);
			/**/this.maxSectorsPerTrack = mkField("maxSectorsPerTrack", this.numberOfHeadsAndSectors, 0x00FF);
			this.formatLength = mkWord(name, "formatLength");
			this.flags = mkWord(name, "flags");
			/**/this.ready = mkBoolField("ready", this.flags, 0x8000);
			/**/this.diskChange = mkBoolField("diskChange", this.flags, 0x4000);
			/**/this.twoSided = mkBoolField("twoSided", this.flags, 0x2000);
			/**/this.busy = mkBoolField("busy", this.flags, 0x1000);
		}	
	}
	
	private static class FDF_Context extends IOStruct {
		
		public final Word w;
		/**/public final BoolField protect;
		/**/public final BoolField isTroyFormat;
		/**/public final BoolField isDoubleDensity;
		/**/public final Field sectorLength; // SectorLength: TYPE = [0..1024)
		
		public FDF_Context(String name) {
			super(null, name);
			this.w = IORegion.mkWord(name, "w");
			/**/this.protect = mkBoolField("protect", this.w, 0x8000);
			/**/this.isTroyFormat = mkBoolField("isTroyFormat", this.w, 0x4000);
			/**/this.isDoubleDensity = mkBoolField("isDoubleDensity", this.w, 0x2000);
			/**/this.sectorLength = mkField("sectorLength", this.w, 0x1FFF);
		}
		
		public FDF_Context(IOStruct embeddingParent, String locationName) {
			super(embeddingParent, locationName);
		
			this.w = mkWord("word");
			/**/this.protect = mkBoolField("protect", this.w, 0x8000);
			/**/this.isTroyFormat = mkBoolField("isTroyFormat", this.w, 0x4000);
			/**/this.isDoubleDensity = mkBoolField("isDoubleDensity", this.w, 0x2000);
			/**/this.sectorLength = mkField("sectorLength", this.w, 0x1FFF);
			
			this.endStruct();
		}
	}
	
	private static class Port80ControlWordRecord {
		
		private static Word w;
		/**/public final BoolField enableMainMemory;
		/**/public final BoolField enableTimerZero;
		/**/public final BoolField fddMotorOn;
		/**/public final BoolField fddInUse;
		/**/public final BoolField allowTimerTC;
		/**/public final BoolField fddLowSpeed;
		/**/public final BoolField selectChAIntClk;
		/**/public final BoolField enableDCEClk;
		/**/public final BoolField driveSelect3;
		/**/public final BoolField driveSelect2;
		/**/public final BoolField driveSelect1;
		/**/public final BoolField driveSelect0;
		/**/public final BoolField select250KbDataRate;
		/**/public final Field preCompensation;
		
		public Port80ControlWordRecord(String name) {
			this.w = mkWord(name, "w");
			/**/this.enableMainMemory = mkBoolField("enableMainMemory", this.w, 0x8000);
			/**/this.enableTimerZero = mkBoolField("enableTimerZero", this.w,   0x4000);
			/**/this.fddMotorOn = mkBoolField("fddMotorOn", this.w,             0x2000);
			/**/this.fddInUse = mkBoolField("fddInUse", this.w,                 0x1000);
			/**/this.allowTimerTC = mkBoolField("allowTimerTC", this.w,         0x0800);
			/**/this.fddLowSpeed = mkBoolField("fddLowSpeed", this.w,           0x0400);
			/**/this.selectChAIntClk = mkBoolField("selectChAIntClk", this.w,   0x0200);
			/**/this.enableDCEClk = mkBoolField("enableDCEClk", this.w,         0x0100);
			/**/this.driveSelect3 = mkBoolField("driveSelect3", this.w,         0x0080);
			/**/this.driveSelect2 = mkBoolField("driveSelect2", this.w,         0x0040);
			/**/this.driveSelect1 = mkBoolField("driveSelect1", this.w,         0x0020);
			/**/this.driveSelect0 = mkBoolField("driveSelect0", this.w,         0x0010);
			/**/this.select250KbDataRate = mkBoolField("select250KbDataRate", this.w, 0x0008);
			/**/this.preCompensation = mkField("preCompensation", this.w,       0x0007);
		}
	}
	
	private static class FdcStatusRegister3TypeAndSpecifyAndRecalFlags {
		private static Word w;
		/**/public final BoolField fault;
		/**/public final BoolField writeProtected;
		/**/public final BoolField ready;
		/**/public final BoolField track0;
		/**/public final BoolField twoSided;
		/**/public final BoolField theHeadAddress;
		/**/public final Field theDriveNumber;
		/**/public final Field specifyAndRecalFlags;
		
		public FdcStatusRegister3TypeAndSpecifyAndRecalFlags(String name) {
			this.w = mkWord(name, "w");
			/**/this.fault = mkBoolField("fault", this.w, 0x8000);
			/**/this.writeProtected = mkBoolField("writeProtected", this.w, 0x4000);
			/**/this.ready = mkBoolField("ready", this.w, 0x2000);
			/**/this.track0 = mkBoolField("track0", this.w, 0x1000);
			/**/this.twoSided = mkBoolField("twoSided", this.w, 0x0800);
			/**/this.theHeadAddress = mkBoolField("theHeadAddress", this.w, 0x0400);
			/**/this.theDriveNumber = mkField("theDriveNumber", this.w, 0x0300);
			/**/this.specifyAndRecalFlags = mkField("specifyAndRecalFlags", this.w, 0x00FF);
			
		}
	}

	private static class DeviceContextBlock {
		
		public final FDF_Attributes deviceAttributes; // -- only used by Head
		public final Word w5;
		/**/public final Field dcbExtraByte1;
		/**/public final IOPBoolean driveBusy;        // -- set by Handler, read by Head
		public final IOPBoolean diagnosticDiskChanged;// -- set by Handler, cleared and/or read by Head
		public final IOPBoolean pilotDiskChanged;     // -- set by Handler, cleared and/or read by Head
		public final FDF_Context diagnosticContext;   // -- only used by Head
		public final FDF_Context pilotContext;        // -- only used by Head
		public final IOPBoolean doorOpen;             // -- set by Handler, read by Head
		public final FdcStatusRegister3TypeAndSpecifyAndRecalFlags statusRegister3; // -- FdcStatusRegister3Type set by Handler, read by Head if floppy.  head sets this if tape.
		public final Port80ControlWordRecord port80ControlWord;
		public final Word w13;
		/**/public final Field stepRateTimePlusHeadUnloadTime;
		/**/public final Field headLoadTimePlusNotInDMAmode;
		
		private DeviceContextBlock(String name) {
			this.deviceAttributes = new FDF_Attributes(name + ".deviceAttributes");
			this.w5 = mkWord(name, "word5");
			/**/this.dcbExtraByte1 = mkField("dcbExtraByte1", this.w5, 0xFF00);
			/**/this.driveBusy = mkIOPShortBoolean("driveBusy", this.w5, false);
			this.diagnosticDiskChanged = mkIOPBoolean(name, "diagnosticDiskChanged");
			this.pilotDiskChanged = mkIOPBoolean(name, "pilotDiskChanged");
			this.diagnosticContext = new FDF_Context(name + ".diagnosticContext");
			this.pilotContext = new FDF_Context(name + ".pilotContext");
			this.doorOpen = mkIOPBoolean(name, "doorOpen");
			this.statusRegister3 = new FdcStatusRegister3TypeAndSpecifyAndRecalFlags("w11");
			this.port80ControlWord = new Port80ControlWordRecord(name + ".port80ControlWord");
			this.w13 = mkWord(name, "word13");
			/**/this.stepRateTimePlusHeadUnloadTime = mkField("stepRateTimePlusHeadUnloadTime", this.w13, 0xFF00);
			/**/this.headLoadTimePlusNotInDMAmode = mkField("headLoadTimePlusNotInDMAmode", this.w13, 0x00FF);
		}
	}
	
	private static class CounterControlWord extends IOStruct {
		
		public final Word w;
		/**/public final BoolField enable;
		/**/public final BoolField changeEnable;
		/**/public final BoolField counterInterruptWhenDone;
		/**/public final BoolField registerInUse;
		/**/public final Field notUsed;
		/**/public final BoolField maximumCount;
		/**/public final BoolField retrigger;
		/**/public final BoolField prescaler;
		/**/public final BoolField external;
		/**/public final BoolField alternate;
		/**/public final BoolField continuous;
		
		public CounterControlWord(String name) {
			this(null, name);
		}
		
		public CounterControlWord(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
			
			this.w = (embeddingParent == null) ? IORegion.mkByteSwappedWord(name, "w") : mkByteSwappedWord("w");
			/**/this.enable = mkBoolField("enable", this.w, 0x8000);
			/**/this.changeEnable = mkBoolField("changeEnable", this.w, 0x4000);
			/**/this.counterInterruptWhenDone = mkBoolField("counterInterruptWhenDone", this.w, 0x2000);
			/**/this.registerInUse = mkBoolField("registerInUse", this.w, 0x1000);
			/**/this.notUsed = mkField("notUsed", this.w, 0x0FC0);
			/**/this.maximumCount = mkBoolField("maximumCount", this.w, 0x0020);
			/**/this.retrigger = mkBoolField("retrigger", this.w, 0x0010);
			/**/this.prescaler = mkBoolField("prescaler", this.w, 0x0008);
			/**/this.external = mkBoolField("external", this.w, 0x0004);
			/**/this.alternate = mkBoolField("alternate", this.w, 0x0002);
			/**/this.continuous = mkBoolField("continous", this.w, 0x0001);
			
			this.endStruct();
		}
	}
	
	private static class DmaControlWord extends IOStruct {
		
		public final Word w;
		/**/public final BoolField isMemoryDestination;
		/**/public final BoolField decrementDestination;
		/**/public final BoolField incrementDestination;
		/**/public final BoolField isMemorySource;
		/**/public final BoolField decrementSource;
		/**/public final BoolField incrementSource;
		/**/public final BoolField stopWhenTransferCountIsZero;
		/**/public final BoolField dmaInterruptWhenDone;
		/**/public final Field synchronization;
		/**/public final BoolField highChannelPriority;
		/**/public final BoolField transmitDataRequest;
		/**/public final BoolField changeStartChannel;
		/**/public final BoolField startChannel;
		/**/public final BoolField byteOrWordTransfer;
		
		public DmaControlWord(String name) {
			this(null, name);
		}
		
		public DmaControlWord(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
			
			this.w = (embeddingParent == null) ? IORegion.mkByteSwappedWord(name, "word") : mkByteSwappedWord("word");
			/**/this.isMemoryDestination = mkBoolField("isMemoryDestination", this.w, 0x8000);
			/**/this.decrementDestination = mkBoolField("decrementDestination", this.w, 0x4000);
			/**/this.incrementDestination = mkBoolField("incrementDestination", this.w, 0x2000);
			/**/this.isMemorySource = mkBoolField("isMemorySource", this.w, 0x1000);
			/**/this.decrementSource = mkBoolField("decrementSource", this.w, 0x0800);
			/**/this.incrementSource = mkBoolField("incrementSource", this.w, 0x0400);
			/**/this.stopWhenTransferCountIsZero = mkBoolField("stopWhenTransferCountIsZero", this.w, 0x0200);
			/**/this.dmaInterruptWhenDone = mkBoolField("dmaInterruptWhenDone", this.w, 0x0100);
			/**/this.synchronization = mkField("synchronization", this.w, 0x00C0);
			/**/this.highChannelPriority = mkBoolField("highChannelPriority", this.w, 0x0020);
			/**/this.transmitDataRequest = mkBoolField("transmitDataRequest", this.w, 0x0010);
			/**/this.changeStartChannel = mkBoolField("changeStartChannel", this.w, 0x0004);
			/**/this.startChannel = mkBoolField("startChannel", this.w, 0x0002);
			/**/this.byteOrWordTransfer = mkBoolField("byteOrWordTransfer", this.w, 0x0001);
			
			this.endStruct();
		}
	}
	
	private static class FCB implements IORAddress {
		private final int startAddress;
		
		public final TaskContextBlock task;
		public final TaskContextBlock dmaTask;
		public final Word w18;
		/**/public final IOPBoolean stopHandler;
		/**/public final IOPBoolean resetFDC;
		public final Word w19;
		/**/public final IOPBoolean handlerIsStopped;
		/**/public final IOPBoolean fdcHung;
		public final Word w20;
		/**/public final IOPBoolean waitingForDMAInterrupt;
		/**/public final IOPBoolean firstDMAInterrupt;
		public final Word w21;
		/**/public final Field driveMotorControlCount;
		/**/public final IOPBoolean timeoutOccurred;
		public final Word w22;
		/**/public final Field badDMAInterruptCount;
		/**/public final Field badFDCInterruptCount;
		public final Word w23;
		/**/public final IOPBoolean tapeThisIOCB;
		public final Word w24;
		/**/public final Field fillerByteForFormatting;
		/**/public final IOPBoolean diagnosticsOn;
		public final Word encodedDeviceTypes; // EncodedDeviceType,	-- read from EEPROM by handler
		public final NotifyMask workMask;
		public final IOPCondition workNotify;
		public final Word lockMask;
		public final OpieAddress currentIOCB;
		public final QueueBlock diagnosticQueue;
		public final QueueBlock pilotQueue;
		public final QueueBlock p80186Queue;
		public final DeviceContextBlock dcb0;
		public final DeviceContextBlock dcb1;
		public final DeviceContextBlock dcb2;
		public final DeviceContextBlock dcb3;
		/**/public final DeviceContextBlock[] dcb;
		public final Word totalBytesToTransfer;
		public final CounterControlWord counterControlRegister;
		public final Word firstDMAtransferCount;
		public final DmaControlWord firstDmaControlWord;
		public final Word numberOfMiddleDMAtransfers;
		public final Word middleDMAtransferCount;
		public final DmaControlWord middleDmaControlWord;
		public final Word lastDMAtransferCount;
		public final DmaControlWord lastDmaControlWord;
		public final Word wx;
		/**/public final Field currentTrack; // MACHINE DEPENDENT {None(0), MiddleTrack(1), LastTrack(3), (255)}
		/**/public final Field extraByte1;
		public final Word queueSemaphore;
		
		private FCB() {
			this.startAddress = IORegion.syncToSegment() + IORegion.IOR_BASE;
			
			this.task = new TaskContextBlock(FloppyFCB, "task");
			this.dmaTask = new TaskContextBlock(FloppyFCB, "dmaTask");
			this.w18 = mkWord(FloppyFCB, "word18");
			/**/this.stopHandler = mkIOPShortBoolean("stopHandler", this.w18, true);
			/**/this.resetFDC = mkIOPShortBoolean("resetFDC", this.w18, false);
			this.w19 = mkWord(FloppyFCB, "word19");
			/**/this.handlerIsStopped = mkIOPShortBoolean("handlerIsStopped", this.w19, true);
			/**/this.fdcHung = mkIOPShortBoolean("fdcHung", this.w19, false);
			this.w20 = mkWord(FloppyFCB, "word20");
			/**/this.waitingForDMAInterrupt = mkIOPShortBoolean("waitingForDMAInterrupt", this.w20, true);
			/**/this.firstDMAInterrupt = mkIOPShortBoolean("firstDMAInterrupt", this.w20, false);
			this.w21 = mkWord(FloppyFCB, "word21");
			/**/this.driveMotorControlCount = mkField("driveMotorControlCount", this.w21, 0xFF00);
			/**/this.timeoutOccurred = mkIOPShortBoolean("timeoutOccurred", this.w21, false);
			this.w22 = mkWord(FloppyFCB, "word22");
			/**/this.badDMAInterruptCount = mkField("badDMAInterruptCount", this.w22, 0xFF00);
			/**/this.badFDCInterruptCount = mkField("badFDCInterruptCount", this.w22, 0x00FF);
			this.w23 = mkWord(FloppyFCB, "word23");
			/**/this.tapeThisIOCB = mkIOPShortBoolean("tapeThisIOCB", this.w23, true);
			this.w24 = mkWord(FloppyFCB, "word24");
			/**/this.fillerByteForFormatting = mkField("fillerByteForFormatting", this.w24, 0xFF00);
			/**/this.diagnosticsOn = mkIOPShortBoolean("diagnosticsOn", this.w24, false);
			this.encodedDeviceTypes = mkWord(FloppyFCB, "encodedDeviceTypes");
			this.workMask = new NotifyMask(FloppyFCB, "workMask");
			this.workNotify = new IOPCondition(FloppyFCB, "workNotify");
			this.lockMask = mkWord(FloppyFCB, "lockMask");
			this.currentIOCB = new OpieAddress(FloppyFCB, "currentIOCB");
			this.diagnosticQueue = new QueueBlock(FloppyFCB, "diagnosticQueue");
			this.pilotQueue = new QueueBlock(FloppyFCB, "pilotQueue");
			this.p80186Queue = new QueueBlock(FloppyFCB, "80186Queue");
			this.dcb0 = new DeviceContextBlock(FloppyFCB + ".dcb[0]");
			this.dcb1 = new DeviceContextBlock(FloppyFCB + ".dcb[1]");
			this.dcb2 = new DeviceContextBlock(FloppyFCB + ".dcb[2]");
			this.dcb3 = new DeviceContextBlock(FloppyFCB + ".dcb[3]");
			this.dcb = new DeviceContextBlock[] { this.dcb0 , this.dcb1 , this.dcb2 , this.dcb3 };
			this.totalBytesToTransfer = mkWord(FloppyFCB, "totalBytesToTransfer");
			this.counterControlRegister = new CounterControlWord(FloppyFCB + ".counterControlRegister");
			this.firstDMAtransferCount = mkWord(FloppyFCB, "firstDMAtransferCount");
			this.firstDmaControlWord = new DmaControlWord(FloppyFCB + ".firstDmaControlWord");
			this.numberOfMiddleDMAtransfers = mkWord(FloppyFCB, "numberOfMiddleDMAtransfers");
			this.middleDMAtransferCount = mkWord(FloppyFCB, "middleDMAtransferCount");
			this.middleDmaControlWord = new DmaControlWord(FloppyFCB + ".middletDmaControlWord");
			this.lastDMAtransferCount = mkWord(FloppyFCB, "lastDMAtransferCount");
			this.lastDmaControlWord = new DmaControlWord(FloppyFCB + ".lastDmaControlWord");
			this.wx = mkWord(FloppyFCB, "wx");
			/**/this.currentTrack = mkField("currentTrack", this.wx, 0xFF00);
			/**/this.extraByte1 = mkField("extraByte1", this.wx, 0x00FF);
			this.queueSemaphore = mkWord(FloppyFCB, "queueSemaphore");
			
			// initialize masks for communication with mesahead
			this.workMask.byteMaskAndOffset.set(mkMask());
			this.lockMask.set(mkMask());
		}

		@Override
		public String getName() {
			return FloppyFCB;
		}

		@Override
		public int getRealAddress() {
			return this.startAddress;
		}
		
	}
	
	/*
	 * Input Output Control Block
	 */
	
	private interface Function {
	    public static final int nop = 0;                 // Does not transfer any data but does create an asychronous operation and returns a valid endng status.
	    public static final int readSector = 1;          // Reads one or more sectors from the diskette starting at the specified disk address.
	    public static final int writeSector = 2;         // Writes one or more sectors with a data addres mark starting at the specified disk address.
	    public static final int writeDeletedSector = 3;  // Writes one or more sectors with a deleted data addres mark starting at the specified disk address.
	    public static final int readID = 4;              // Reads first encountered record ID from the specified disk address (the value of sector in the disk address is ignored).
	    public static final int formatTrack = 5;         // format one or more tracks starting at the specified disk cylinder & head
	}
	
	private static String getFunctionName(int code) {
		switch(code) {
		case Function.nop: return "nop";
		case Function.readSector: return "readSector";
		case Function.writeSector: return "writeSector";
		case Function.writeDeletedSector: return "writeDeletedSector";
		case Function.readID: return "readID";
		case Function.formatTrack: return "formatTrack";
		default: return String.format("invalid Function-code[%d]", code);
		}
	}

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
	
	private static class FDF_Operation extends IOStruct {
		
		public final Word device;
		public final Word function;
		public final DiskAddress address; // ignored if nop.  Sector is ignored in formatTrack operations.
		public final DblWord dataPtr; // LONG POINTER, ignored if nop
		public final Word w6;
		/**/public final BoolField incrementDataPointer; // ignored if nop, formatTrack
		/**/public final Field tries;
		public final Word count;
		
		public FDF_Operation(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
		
			this.device = mkWord("device");
			this.function = mkWord("function");
			this.address = new DiskAddress(this, "address");
			this.dataPtr = mkDblWord("dataPtr");
			this.w6 = mkWord("word6");
			/**/this.incrementDataPointer = mkBoolField("incrementDataPointer", this.w6, 0x8000);
			/**/this.tries = mkField("tries", this.w6, 0x7FFF);
			this.count = mkWord("count");
			
			this.endStruct();
		}
		
		@Override
		public String toString() {
			return String.format(
				"Operation[ device: %d, function: %s , address: %d/%d/%d . dataPtr: 0x%06X , incrDataPointer: %s , count = %d ]",
				this.device.get(),
				getFunctionName(this.function.get()),
				this.address.cylinder.get(), this.address.head.get(), this.address.sector.get(),
				this.dataPtr.get(),
				Boolean.toString(this.incrementDataPointer.get() != 0),
				this.count.get()
				);
		}
	}
	
	private interface ExtendedFDCcommandType {
	    public static final int NullCommand = 0;
	    public static final int FormatTrack = 1;
	    public static final int ReadData = 2;
	    public static final int ReadDeletedData = 3;
	    public static final int ReadID = 4;
	    public static final int ReadTrack = 5;
	    public static final int Recalibrate = 6;
	    public static final int ScanEqual = 7;
	    public static final int ScanHighOrEqual = 8;
	    public static final int ScanLowOrEqual = 9;
	    public static final int Seek = 10;
	    public static final int SenseDriveStatus = 11;
	    public static final int SenseInterruptStatus = 12;
	    public static final int Specify = 13;
	    public static final int WriteData = 14;
	    public static final int WriteDeletedData = 15;
	    public static final int lastAndInvalid = 255;
	}
	
	private static String getExtendedFDCcommandTypeName(int code) {
		switch(code) {
		case ExtendedFDCcommandType.NullCommand: 			return "NullCommand";
		case ExtendedFDCcommandType.FormatTrack:			return "FormatTrack";
		case ExtendedFDCcommandType.ReadData:				return "ReadData";
		case ExtendedFDCcommandType.ReadDeletedData:		return "ReadDeletedData";
		case ExtendedFDCcommandType.ReadID:					return "ReadID";
		case ExtendedFDCcommandType.ReadTrack:				return "ReadTrack";
		case ExtendedFDCcommandType.Recalibrate:			return "Recalibrate";
		case ExtendedFDCcommandType.ScanEqual:				return "ScanEqual";
		case ExtendedFDCcommandType.ScanHighOrEqual:		return "ScanHighOrEqual";
		case ExtendedFDCcommandType.Seek:					return "Seek";
		case ExtendedFDCcommandType.SenseDriveStatus:		return "SenseDriveStatus";
		case ExtendedFDCcommandType.SenseInterruptStatus:	return "SenseInterruptStatus";
		case ExtendedFDCcommandType.Specify:				return "Specify";
		case ExtendedFDCcommandType.WriteData:				return "WriteData";
		case ExtendedFDCcommandType.WriteDeletedData:		return "WriteDeletedData";
		case ExtendedFDCcommandType.lastAndInvalid:         return "lastAndInvalid";
		default: return String.format("invalid ExtendedFDCcommandType-code[%d]", code);
		}
	}
	
	private interface FDF_Status {
		public static final int inProgress = 0; // operation is not yet complete
		public static final int goodCompletion = 1; // operation has completed normally
		public static final int diskChange = 2; // drive has gone not ready since last successful operation (use DiskChangeClear to reset, then resubmit operation if desired)
		public static final int notReady = 3; // drive is not ready
		public static final int cylinderError = 4; // can't locate specified cylinder
		public static final int deletedData = 5; // The sector contained a deleted data address mark.
		public static final int recordNotFound = 6; // can't find record for specified disk address
		public static final int headerError = 7; // bad checksum in header
		public static final int dataError = 8; // bad checksum in data
		public static final int dataLost = 9; // sector contained more data than expected (from context)
		public static final int writeFault = 10; // disk is write-protected (hardware or from context)
		public static final int memoryError = 11; // dataPtr does not point to valid memory (not resident, too small, write-protected, etc.)
		public static final int invalidOperation = 12; // operation does not make sense
		public static final int aborted = 13; // Reset has been called
		public static final int otherError = 14; // unexpected software or hardware problem
	}
	
	public interface OperationStateType { 
		public static final int OperationDoesNotExist = 0;
		public static final int OperationInvalid = 1;
		public static final int OperationBuilt = 2;
		public static final int OperationWaiting = 3;
		public static final int OperationInProgress = 4;
		public static final int OperationAborted = 5;
		public static final int OperationCompleted = 6;
		public static final int OperationFailed = 7;
		public static final int none = 255;
	}
	
	private static class TrackDMAandCounterControl extends IOStruct {
		
		public final Word TotalBytesToTransfer;
		public final Word TotalBytesActuallyTransfered;
		public final CounterControlWord CounterControlRegister;
		public final Word FirstDMAtransferCount;
		public final DmaControlWord FirstDMAcontrolWord;
		public final Word NumberOfMiddleDMAtransfers;
		public final Word MiddleDMAtransferCount;
		public final DmaControlWord MiddleDMAcontrolWord;
		public final Word LastDMAtransferCount;
		public final DmaControlWord LastDMAcontrolWord;
		
		public TrackDMAandCounterControl(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
		
			this.TotalBytesToTransfer = mkByteSwappedWord("TotalBytesToTransfer");
			this.TotalBytesActuallyTransfered = mkByteSwappedWord("TotalBytesActuallyTransfered");
			this.CounterControlRegister = new CounterControlWord(this, "CounterControlRegister");
			this.FirstDMAtransferCount = mkByteSwappedWord("FirstDMAtransferCount");
			this.FirstDMAcontrolWord = new DmaControlWord(this, "FirstDMAcontrolWord");
			this.NumberOfMiddleDMAtransfers = mkByteSwappedWord("NumberOfMiddleDMAtransfers");
			this.MiddleDMAtransferCount = mkByteSwappedWord("MiddleDMAtransferCount");
			this.MiddleDMAcontrolWord = new DmaControlWord(this, "MiddleDMAcontrolWord");
			this.LastDMAtransferCount = mkByteSwappedWord("LastDMAtransferCount");
			this.LastDMAcontrolWord = new DmaControlWord(this, "LastDMAcontrolWord");
			
			this.endStruct();
		}
	}
	
	private static class FdcCommandRecord extends IOStruct {
		
		public final Word w0;
		/**/public final Field fdcCode; // ExtendedFDCcommandType
		/**/public final Field DataTransferCode; // DataTransferType = MACHINE DEPENDENT { None(0), Read(1), Write(2), (255) } 
		public final Word w1;
		/**/public final Field anExtraByte;
		/**/public final IOPBoolean MustWaitForInterrupt;
		public final Word w2;
		/**/public final Field NumberOfCommandBytes;
		/**/public final Field NumberOfCommandBytesWritten; //direct from FDCdirect from FDC
		public final Word CommandBytes_0_1; // direct to FDC
		public final Word CommandBytes_2_3; // direct to FDC
		public final Word CommandBytes_4_5; // direct to FDC
		public final Word CommandBytes_6_7; // direct to FDC
		public final Word CommandBytes_8_9; // direct to FDC
		public final Word w8;
		/**/public final Field NumberOfResultBytes;
		/**/public final Field NumberOfResultBytesRead;
		public final Word ResultBytes_0_1;
		public final Word ResultBytes_2_3;
		public final Word ResultBytes_4_5;
		public final Word ResultBytes_6_7;
		
		
		public FdcCommandRecord(IOStruct embeddingParent, String name) {
			super(embeddingParent, name);
			
			this.w0 = mkWord("word0");
			/**/this.fdcCode = mkField("fdcCode", this.w0, 0xFF00);
			/**/this.DataTransferCode = mkField("DataTransferCode", this.w0, 0x00FF);
			this.w1 = mkWord("word1");
			/**/this.anExtraByte = mkField("anExtraByte", this.w1, 0xFF00);
			/**/this.MustWaitForInterrupt = mkIOPShortBoolean("MustWaitForInterrupt", this.w1, false);
			this.w2 = mkWord("word2");
			/**/this.NumberOfCommandBytes = mkField("NumberOfCommandBytes", this.w2, 0xFF00);
			/**/this.NumberOfCommandBytesWritten = mkField("NumberOfCommandBytesWritten", this.w2, 0x00FF);
			this.CommandBytes_0_1 = mkWord("CommandBytes_0+1");
			this.CommandBytes_2_3 = mkWord("CommandBytes_2+3");
			this.CommandBytes_4_5 = mkWord("CommandBytes_4+5");
			this.CommandBytes_6_7 = mkWord("CommandBytes_6+7");
			this.CommandBytes_8_9 = mkWord("CommandBytes_8+9");
			this.w8 = mkWord("word8");
			/**/this.NumberOfResultBytes = mkField("NumberOfResultBytes", this.w8, 0xFF00);
			/**/this.NumberOfResultBytesRead = mkField("NumberOfResultBytesRead", this.w8, 0x00FF);
			this.ResultBytes_0_1 = mkWord("ResultBytes_0+1");
			this.ResultBytes_2_3 = mkWord("ResultBytes_2+3");
			this.ResultBytes_4_5 = mkWord("ResultBytes_4+5");
			this.ResultBytes_6_7 = mkWord("ResultBytes_6+7");
			
			this.endStruct();
		}
		
	}

	
	private static final String FloppyIOCB = "FloppyIOCB";
	
	private static class IOCB extends IOStruct {
		
		public final FDF_Operation operation;
		
		public final Word w8;
		/**/public final Field generalizedFDCOperation; // ==> ExtendedFDCcommandType
		/**/public final Field savedStatus; // ==> FDF_Status
		public final FDF_Context theContext;
		public final Word w10;
		/**/public final BoolField alternateSectors;
		/**/public final BoolField multiTrackMode;
		/**/public final BoolField skipDeletedSector;
		/**/public final Field currentTryCount;
		
		public final Word w11;
		/**/public final IOPBoolean operationIsQueued;
		/**/public final Field operationState; // ==> OperationStateType
		public final OpieAddress nextIOCB;
		public final OpieAddress dataAddress;
		public final ClientCondition actualClientCondition;
		
		public final Word w19; // FinalStateOfFDC & SpecifyFlag
		/**/public final BoolField finalStateOfFDC_RequestForMaster;
		/**/public final BoolField finalStateOfFDC_DataInputOutput;
		/**/public final BoolField finalStateOfFDC_nonDMAmode;
		/**/public final BoolField finalStateOfFDC_fdcBusy;
		/**/public final BoolField finalStateOfFDC_DiskDrive3busy;
		/**/public final BoolField finalStateOfFDC_DiskDrive2busy;
		/**/public final BoolField finalStateOfFDC_DiskDrive1busy;
		/**/public final BoolField finalStateOfFDC_DiskDrive0busy;
		/**/public final IOPBoolean specifyFlag;
		public final Word w20;
		/**/public final IOPBoolean PCEResetFDCFlag;
		/**/public final Field PCEStartMotorFlags;
		public final Word w21;
		/**/public final IOPBoolean ResetAndFlushFlag;
		/**/public final IOPBoolean RecalFlag;
		public final Word w22;
		/**/public final Field daDriveNumber;
		/**/public final IOPBoolean FDCHung;
		public final TrackDMAandCounterControl firstTrack;
		public final Word FinalDMACount;
		public final Word w34;
		/**/public final IOPBoolean incrementDataPointer;
		/**/public final IOPBoolean TimeoutOccurred;
		public final Word numberOfFDCCommands;
		public final Word currentFDCCommand;
		public final FdcCommandRecord fdcCommands0;
		public final FdcCommandRecord fdcCommands1;
		public final FdcCommandRecord fdcCommands2;
		/**/public final FdcCommandRecord[] fdcCommands;
		public final Word w76;
		/**/public final Field tapeSelection; // {none, tapeOn, tapeOnStreamSelection, formatSelection}
		/**/public final Field stream;
		public final Word numberOfMiddleTrackTransfers;
		public final TrackDMAandCounterControl middleTrack;
		public final TrackDMAandCounterControl lastTrack;
		
		public IOCB(int base) {
			super(base, FloppyIOCB);
			
			this.operation = new FDF_Operation(this, "operation");
			
			this.w8 = mkWord("word8");
			/**/this.generalizedFDCOperation = mkField("generalizedFDCOperation", this.w8, 0xFF00);
			/**/this.savedStatus = mkField("savedStatus", this.w8, 0x00FF);
			this.theContext = new FDF_Context(this, "theContext");
			this.w10 = mkWord("word10");
			/**/this.alternateSectors = mkBoolField("alternateSectors", this.w10, 0x8000);
			/**/this.multiTrackMode = mkBoolField("multiTrackMode", this.w10, 0x4000);
			/**/this.skipDeletedSector = mkBoolField("skipDeletedSector", this.w10, 0x2000);
			/**/this.currentTryCount = mkField("currentTryCount", this.w10, 0x00FF);
			
			this.w11 = mkWord("word11");
			/**/this.operationIsQueued = mkIOPShortBoolean("operationIsQueued", this.w11, true);
			/**/this.operationState = mkField("operationState", this.w11, 0x00FF);
			this.nextIOCB = new OpieAddress(this, "nextIOCB");
			this.dataAddress = new OpieAddress(this, "dataAddress");
			this.actualClientCondition = new ClientCondition(this, "actualClientCondition");
			
			this.w19 = mkWord("word19");
			/**/this.finalStateOfFDC_RequestForMaster = mkBoolField("finalStateOfFDC.RequestForMaster", this.w19, 0x8000);
			/**/this.finalStateOfFDC_DataInputOutput = mkBoolField("finalStateOfFDC.DataInputOutput", this.w19, 0x4000);
			/**/this.finalStateOfFDC_nonDMAmode = mkBoolField("finalStateOfFDC.nonDMAmode", this.w19, 0x2000);
			/**/this.finalStateOfFDC_fdcBusy = mkBoolField("finalStateOfFDC.fdcBusy", this.w19, 0x1000);
			/**/this.finalStateOfFDC_DiskDrive3busy = mkBoolField("finalStateOfFDC.DiskDrive3busy", this.w19, 0x0800);
			/**/this.finalStateOfFDC_DiskDrive2busy = mkBoolField("finalStateOfFDC.DiskDrive2busy", this.w19, 0x0400);
			/**/this.finalStateOfFDC_DiskDrive1busy = mkBoolField("finalStateOfFDC.DiskDrive1busy", this.w19, 0x0200);
			/**/this.finalStateOfFDC_DiskDrive0busy = mkBoolField("finalStateOfFDC.DiskDrive0busy", this.w19, 0x0100);
			/**/this.specifyFlag = mkIOPShortBoolean("specifyFlag", this.w19, false);
			this.w20 = mkWord("word20");
			/**/this.PCEResetFDCFlag = mkIOPShortBoolean("PCEResetFDCFlag", this.w20, true);
			/**/this.PCEStartMotorFlags = mkField("PCEStartMotorFlags", this.w20, 0x00FF);
			this.w21 = mkWord("word21");
			/**/this.ResetAndFlushFlag = mkIOPShortBoolean("ResetAndFlushFlag", this.w21, true);
			/**/this.RecalFlag = mkIOPShortBoolean("RecalFlag", this.w21, false);
			this.w22 = mkWord("word22");
			/**/this.daDriveNumber = mkField("DaDriveNumber", this.w22, 0xFF00);
			/**/this.FDCHung = mkIOPShortBoolean("FDCHung", this.w22, false);
			this.firstTrack = new TrackDMAandCounterControl(this, "firstTrack");
			this.FinalDMACount = mkByteSwappedWord("FinalDMACount");
			this.w34 = mkWord("word34");
			/**/this.incrementDataPointer = mkIOPShortBoolean("IncrementDataPointer", this.w34, true);
			/**/this.TimeoutOccurred = mkIOPShortBoolean("TimeoutOccurred", this.w34, false);
			this.numberOfFDCCommands = mkByteSwappedWord("NumberOfFDCCommands");
			this.currentFDCCommand = mkByteSwappedWord("CurrentFDCCommand");
			this.fdcCommands0 = new FdcCommandRecord(this, "fdcCommands[0]");
			this.fdcCommands1 = new FdcCommandRecord(this, "fdcCommands[1]");
			this.fdcCommands2 = new FdcCommandRecord(this, "fdcCommands[2]");
			this.fdcCommands = new FdcCommandRecord[] { this.fdcCommands0 , this.fdcCommands1 , this.fdcCommands2 };
			this.w76 = mkWord("word76");
			/**/this.tapeSelection = mkField("tapeSelection", this.w76, 0xFF00);
			/**/this.stream = mkField("stream", this.w76, 0x00FF);
			this.numberOfMiddleTrackTransfers = mkByteSwappedWord("numberOfMiddleTrackTransfers");
			this.middleTrack = new TrackDMAandCounterControl(this, "middleTrack");
			this.lastTrack = new TrackDMAandCounterControl(this, "lastTrack");
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
	 * implementation
	 */
	
	private final FCB fcb;
	private final IOCB workIocb = new IOCB(0); // will be rebased for each access IOCB enqueue by head
	
	public HFloppy() {
		super(FloppyFCB, Config.IO_LOG_FLOPPY);
		this.fcb = new FCB();
		
		// initialize FCB: we have 1x Shugart SA-455 as drive 0 and no more drives
		this.fcb.encodedDeviceTypes.set((short)0x4000); // 1x EncodedDiskDriveType.sa455DiskDrive(4) , 3x EncodedDiskDriveType.NoDiskDrive(0)
		
		this.fcb.dcb[0].deviceAttributes.type.set((short)19); // DeviceTypesExtras: sa455:	Device.Type = [FIRST[Floppy]+2];  -- Shugart SA-455 drive (with FIRST[Floppy] == 17)
		this.fcb.dcb[0].deviceAttributes.numberOfCylinders.set((short)40);
		this.fcb.dcb[0].deviceAttributes.maxSectorsPerTrack.set((short)16); // 10 sectors with 512 bytes, 16 sectors with 256 bytes
		this.fcb.dcb[0].deviceAttributes.formatLength.set((short)12288);
		this.fcb.dcb[0].deviceAttributes.ready.set(false);
		
		this.fcb.dcb[1].deviceAttributes.type.set((short)0); // Device.nullType
		this.fcb.dcb[2].deviceAttributes.type.set((short)0); // Device.nullType
		this.fcb.dcb[3].deviceAttributes.type.set((short)0); // Device.nullType
		
		// set state to "no floppy inserted"
		this.mesaEjectFloppy();
	}

	@Override
	public int getFcbRealAddress() {
		return this.fcb.getRealAddress();
	}

	@Override
	public short getFcbSegment() {
		return this.fcb.getIOPSegment();
	}

	@Override
	public boolean processNotify(short notifyMask) {
		if (notifyMask != this.fcb.workMask.byteMaskAndOffset.get()) {
			// not for us, let an other handler take care of this
			return false;
		}
		
		this.slogf("\n");
		this.logf("IOP::HFloppy.processNotify() - begin\n");
		
		// process the stopHandler-flag
		if (this.fcb.stopHandler.get()) {
			this.logf("IOP::HFloppy.processNotify() -> stopHandler\n");
			this.fcb.handlerIsStopped.set(true);
			this.logf("IOP::HFloppy.processNotify() - done\n\n");
			return true;
		} else if (this.fcb.handlerIsStopped.get()) {
			this.logf("IOP::HFloppy.processNotify() -> handler restarted\n");
			
			// update status in DCB
			boolean havingFloppy = (this.currFloppy != null);
			this.fcb.dcb[0].deviceAttributes.ready.set(havingFloppy);
			this.fcb.dcb[0].deviceAttributes.diskChange.set(havingFloppy);
			this.fcb.dcb[0].deviceAttributes.twoSided.set(havingFloppy);
			this.logf("===> floppy status: %s\n", Boolean.toString(havingFloppy));
			
		}
		this.fcb.handlerIsStopped.set(false);
		
		// process the iocb handling requests
		int iopIocbPtr = this.fcb.pilotQueue.queueHead.toLP();
		this.logf("IOP::HFloppy.processNotify()  -> fcb.pilotQueue.queueHead.toLP() = 0x%06X\n", iopIocbPtr);
		while (iopIocbPtr != 0) {
			this.workIocb.rebaseToVirtualAddress(iopIocbPtr);
			
			// possibly dump the iocb for debugging
			this.dumpWorkIocb("IOP::HFloppy.processNotify()  -> IOCB data:");
			
			// get operation data
			int genOp = this.workIocb.generalizedFDCOperation.get();
			int dataPtr = this.workIocb.dataAddress.toLP();
			boolean incrDataPtr = this.workIocb.incrementDataPointer.get();
			short intrMask = this.workIocb.actualClientCondition.maskValue.get();
			
			// get and work with the last fdc-operation as the only relevant one
			int fdcCommandIndex = Math.max(0, Math.min(this.workIocb.numberOfFDCCommands.get() - 1, this.workIocb.fdcCommands.length - 1)); // number is 1-based
			FdcCommandRecord fdcCommand = this.workIocb.fdcCommands[fdcCommandIndex];
			int op = fdcCommand.fdcCode.get();
			int driveNo = fdcCommand.CommandBytes_0_1.get() & 0xFF;
			int cyl = (fdcCommand.CommandBytes_2_3.get() >> 8) & 0xFF;
			int head = fdcCommand.CommandBytes_2_3.get() & 0xFF;
			int sect = (fdcCommand.CommandBytes_4_5.get() >> 8) & 0xFF;
			int sectorLengthEncoded = fdcCommand.CommandBytes_4_5.get() & 0xFF;
			int sectorWordLength = (sectorLengthEncoded == 0) ? 64 : (sectorLengthEncoded == 1) ? 128 : 256; // ignore other codes for floppies: 256 words = 512 bytes is max.
			int sectorsPerTrack = (fdcCommand.CommandBytes_6_7.get() >> 8) & 0xFF;
			
			this.slogf(
					"++ iocb.fdcCommands[%d]: op = %d, driveNo = %d, cyl/head/sect: %d/%d/%d, sectLength: code %d => %d words, sectors/track: %d\n",
					fdcCommandIndex, op, driveNo, cyl, head, sect, sectorLengthEncoded, sectorWordLength, sectorsPerTrack);
			
			// tell which fdcCommand was processed last
			this.workIocb.currentFDCCommand.set((short)(fdcCommandIndex + 1)); // 1-based
			fdcCommand.NumberOfCommandBytesWritten.set(fdcCommand.NumberOfCommandBytes.get()); // pretend we sent all commands bytes to the controller
			
			// interpret the dma-specs a bit to get the number of sectors to transfer
			int firstTrackTransferBytes = this.workIocb.firstTrack.TotalBytesToTransfer.get() & 0xFFFF;
			int middleTrackTransferBytes = this.workIocb.middleTrack.TotalBytesToTransfer.get() & 0xFFFF;
			int lastTrackTransferBytes = this.workIocb.lastTrack.TotalBytesToTransfer.get() & 0xFFFF;
			int totalTransferBytes = firstTrackTransferBytes + middleTrackTransferBytes + lastTrackTransferBytes;
			int totalTransferSectors = totalTransferBytes / (sectorWordLength * 2);
			this.slogf("++ iocb-dma-summary: totalTransterBytes = %d ( %d , %d , %d ) => %d sectors\n",
					totalTransferBytes,
					this.workIocb.firstTrack.TotalBytesToTransfer.get(), this.workIocb.middleTrack.TotalBytesToTransfer.get(), this.workIocb.lastTrack.TotalBytesToTransfer.get(),
					totalTransferSectors);
			
			// plausibility-checks of controller- with operation-values
			if (genOp != op) {
				this.slogf("#### warning: iocb.generalizedOperation(%d) != fdcCommand.op(%d)\n", genOp, op);
			}
			if (driveNo != this.workIocb.operation.device.get()) {
				this.slogf("#### warning: fdcCommand.driveNo(%d) != iocb.operation.device(%d)\n", driveNo, this.workIocb.operation.device.get());
			}
			if (driveNo != this.workIocb.daDriveNumber.get()) {
				this.slogf("#### warning: fdcCommand.driveNo(%d) != iocb.daDriveNumber(%d)\n", driveNo, this.workIocb.daDriveNumber.get());
			}
			if (totalTransferSectors != this.workIocb.operation.count.get()) {
				this.slogf("#### warning: fdcCommand->sectorCount(%d) != iocb.operation.count(%d)\n", totalTransferSectors, this.workIocb.operation.count.get());
			}
			
			if (this.workIocb.daDriveNumber.get() == 0) { // we only have drive=0, iocb.daDriveNumber seems more reliable than fcbCommand.commandbyte[2]...
				switch(genOp) {
				
				case ExtendedFDCcommandType.NullCommand: {
						// nothing to do, setting the operationState is apparently enough
						this.workIocb.operationState.set(OperationStateType.OperationCompleted);
					}
					break;
					
				case ExtendedFDCcommandType.ReadData: {
						// transfer data
						int targetPtr = dataPtr;
						int remainingSects = totalTransferSectors;
						int bytesTransferred = 0;
						int nextCyl = cyl;
						int nextHead = head;
						int nextSect = sect;
						while(remainingSects > 0) {
							// get the sector
							short[] sector = this.currFloppy.getSector(nextCyl, nextHead, nextSect);
							if (sector == null) {
								break; // abort data transfer if sector not found ...
							}
							
							// transfer sector content
							for (int i = 0; i < Math.min(sector.length, sectorWordLength); i++) {
								Mem.writeWord(targetPtr + i, sector[i]);
								bytesTransferred += 2;
							}
							targetPtr += sectorWordLength; // place next sector where invoker wanted it
							
							// move to next sector
							nextCyl = this.currFloppy.getNextCyl();
							nextHead = this.currFloppy.getNextHead();
							nextSect = this.currFloppy.getNextSect();
							
							remainingSects--;
						}
						
						// distribute byte transfers over the dma-specs
						int byteCount = Math.min(firstTrackTransferBytes, bytesTransferred);
						this.workIocb.firstTrack.TotalBytesActuallyTransfered.set((short)byteCount);
						bytesTransferred -= byteCount;
						byteCount = Math.min(middleTrackTransferBytes, bytesTransferred);
						this.workIocb.middleTrack.TotalBytesActuallyTransfered.set((short)byteCount);
						bytesTransferred -= byteCount;
						byteCount = Math.min(lastTrackTransferBytes, bytesTransferred);
						this.workIocb.lastTrack.TotalBytesActuallyTransfered.set((short)byteCount);
						
						// hack: adjust iocb.FinalDMACount (+ iocb.firstTrack.TotalBytesActuallyTransfered must be 0 (zero)!)
						int finalDmaCount = 0x00010000 - (this.workIocb.firstTrack.TotalBytesActuallyTransfered.get() & 0xFFFF);
						this.workIocb.FinalDMACount.set((short)(finalDmaCount & 0xFFFF));
						
						// produce result bytes of the fdcCommand
						fdcCommand.ResultBytes_0_1.set((short)0); // fdcStatusRegister0 = 0x00 , fdcStatusRegister1 = 0x00
						fdcCommand.ResultBytes_2_3.set((short)(nextCyl & 0x00FF)); // fdcStatusRegister2 = 0x00 , end-cyl
						fdcCommand.ResultBytes_4_5.set((short)((nextHead << 8) | (nextSect & 0x00FF)));
						fdcCommand.NumberOfResultBytesRead.set(fdcCommand.NumberOfResultBytes.get()); // pretend we read required result bytes from the controller
						
						// tell the operation was successful
						this.workIocb.operationState.set(OperationStateType.OperationCompleted);
					}
					break;
					
				default: {
						// TODO: possibly set more fields in iocb?
						this.workIocb.operationState.set(OperationStateType.OperationInvalid);
					}
				}
			} else {
				// TODO: possibly set more fields in iocb?
				this.workIocb.operationState.set(OperationStateType.OperationInvalid);
			}
			
			// set general data
			this.workIocb.currentTryCount.set(1); // all our operations succeed (or fail) on the first try
			this.workIocb.FDCHung.set(false);
			
			// show resulting iocb state
			this.slogf("\n");
			this.dumpWorkIocb("IOP::HFloppy.processNotify()  -> IOCB after processing:");
			this.slogf("\n");
			
			// raise interrupt to inform about outcome
			logf("IOP::HFloppy.processNotify() -> IOCB at 0x%06X processed, raising interrupt 0x%04X\n", iopIocbPtr, intrMask);
			Processes.requestMesaInterrupt(intrMask);
			
			// forward to next enqueued IOCB
			iopIocbPtr = this.workIocb.nextIOCB.toLP();
			this.logf("IOP::HFloppy.processNotify()  -> iocb.nextIOCB.toLP() = 0x%06X\n", iopIocbPtr);
		}
		
		// done: tell we handled the operation, no need to ask other handlers
		this.logf("IOP::HFloppy.processNotify() - done\n\n");
		return true;
	}
	
	private void dumpWorkIocb(String intro) {
		if (!this.logging) { return; }
		this.logf(intro + "\n");
		this.slogf("    - operation: %s\n", this.workIocb.operation.toString());
		this.slogf("    - generalizedFDCOperation........: %s\n", getExtendedFDCcommandTypeName(this.workIocb.generalizedFDCOperation.get()));
		this.slogf("    - dataAddress....................: 0x%06X\n", this.workIocb.dataAddress.toLP());
		this.slogf("    - actualClientCondition.maskValue: 0x%04X\n", this.workIocb.actualClientCondition.maskValue.get());
		this.slogf("    - daDriveNumber..................: %d\n", this.workIocb.daDriveNumber.get());
		this.slogf("    - incrementDataPointer...........: %s\n", Boolean.toString(this.workIocb.incrementDataPointer.get()));
		this.slogf("    - numberOfFDCCommands............: %d\n", this.workIocb.numberOfFDCCommands.get());
		this.slogf("    - currentFDCCommand..............: %d\n", this.workIocb.currentFDCCommand.get());
		this.dumpFdcCommandRecord("fdcCommands[0]", this.workIocb.fdcCommands0);
		this.dumpFdcCommandRecord("fdcCommands[1]", this.workIocb.fdcCommands1);
		this.dumpFdcCommandRecord("fdcCommands[2]", this.workIocb.fdcCommands2);
		this.dumpTrackDMAandCounterControl("firstTrack", this.workIocb.firstTrack);
		this.dumpTrackDMAandCounterControl("middleTrack", this.workIocb.middleTrack);
		this.dumpTrackDMAandCounterControl("lastTrack", this.workIocb.lastTrack);
	}
	
	private void dumpTrackDMAandCounterControl(String name, TrackDMAandCounterControl trackCtl) {
		this.slogf("    - %s ::\n", name);
		this.slogf("        -- TotalBytesToTransfer--------: %d\n", trackCtl.TotalBytesToTransfer.get() & 0xFFFF);
		this.slogf("        -- TotalBytesActuallyTransfered: %d\n", trackCtl.TotalBytesActuallyTransfered.get() & 0xFFFF);
		this.slogf("        -- CounterControlRegister------: 0x%04X\n", trackCtl.CounterControlRegister.w.get() & 0xFFFF);
		this.slogf("        -- FirstDMAtransferCount-------: %d\n", trackCtl.FirstDMAtransferCount.get() & 0xFFFF);
		this.slogf("        -- FirstDMAcontrolWord---------: 0x%04X\n", trackCtl.FirstDMAcontrolWord.w.get() & 0xFFFF);
		this.slogf("        -- NumberOfMiddleDMAtransfers--: %d\n", trackCtl.NumberOfMiddleDMAtransfers.get() & 0xFFFF);
		this.slogf("        -- MiddleDMAtransferCount------: %d\n", trackCtl.MiddleDMAtransferCount.get() & 0xFFFF);
		this.slogf("        -- MiddleDMAcontrolWord--------: 0x%04X\n", trackCtl.MiddleDMAcontrolWord.w.get() & 0xFFFF);
		this.slogf("        -- LastDMAtransferCount--------: %d\n", trackCtl.LastDMAtransferCount.get() & 0xFFFF);
		this.slogf("        -- LastDMAcontrolWord----------: 0x%04X\n", trackCtl.LastDMAcontrolWord.w.get() & 0xFFFF);
	}
	
	private void dumpFdcCommandRecord(String name, FdcCommandRecord cmd) {
		this.slogf("    - %s ::\n", name);
		this.slogf("        -- fdcCode : %s\n", getExtendedFDCcommandTypeName(cmd.fdcCode.get()));
		this.slogf("        -- dataTransferCode: %d\n", cmd.DataTransferCode.get());
		this.slogf("        -- anExtryByte: %02X\n", cmd.anExtraByte.get() & 0xFF);
		this.slogf("        -- mustWaitForInterrupt: %s\n", Boolean.toString(cmd.MustWaitForInterrupt.get()));
		this.slogf("        -- numberOfCommandBytes: %d\n", cmd.NumberOfCommandBytes.get() & 0xFF);
		this.slogf("        -- numberOfCommandBytesWritten: %d\n", cmd.NumberOfCommandBytesWritten.get() & 0xFF);
		this.slogf("        -- command bytes: 0x %04X %04X %04X %04X %04X\n", cmd.CommandBytes_0_1.get()&0xFFFF, cmd.CommandBytes_2_3.get()&0xFFFF, cmd.CommandBytes_4_5.get()&0xFFFF, cmd.CommandBytes_6_7.get()&0xFFFF, cmd.CommandBytes_8_9.get()&0xFFFF);
		this.slogf("        -- numberOfResultBytes: %d\n", cmd.NumberOfResultBytes.get() & 0xFF);
		this.slogf("        -- numberOfResultBytesWritten: %d\n", cmd.NumberOfResultBytesRead.get() & 0xFF);
		this.slogf("        -- result bytes: 0x %04X %04X %04X %04X\n", cmd.ResultBytes_0_1.get()&0xFFFF, cmd.ResultBytes_2_3.get()&0xFFFF, cmd.ResultBytes_4_5.get()&0xFFFF, cmd.ResultBytes_6_7.get()&0xFFFF);
	}

	@Override
	public void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue) {
		// check if it is for us
		if (lockMask != this.fcb.lockMask.get()) { return; }
		
		// log the lockmem operation
		this.logf("IOP::HFloppy.handleLockmem(rAddr = 0x%06X , memOp = %s , oldValue = 0x%04X , newValue = 0x%04X\n",
				realAddress, memOp.toString(), oldValue, newValue);
		
		// process the synchronized memory operation...
		// mostly logging to see if the expected memory change happen, as the "go" event for doing
		// real work is given through a NotifyIOP instruction
		int ra_queueSemaphore = this.fcb.queueSemaphore.getRealAddress();
		int ra_pilotQueue_queueNext = this.fcb.pilotQueue.queueNext.a15ToA0.ptr.getRealAddress();
		if (realAddress == ra_queueSemaphore) {
			// used for synchronizing access to pilotQueue
			// -> will be reset when the queue is completely updated
			this.logf("IOP::HFloppy.handleLockmem() -> fcb.queueSemaphore accessed by head\n");
		} else if(realAddress == (ra_pilotQueue_queueNext + 1)) {
			// accessing pilotQueue.queueNext 1st phase: upper word
			this.logf("IOP::HFloppy.handleLockmem() -> fcb.pilotQueue.queueNext upper word accessed by head\n");
		} else if (realAddress == ra_pilotQueue_queueNext) {
			// accessing pilotQueue.queueNext 2nd phase: lower word
			this.logf("IOP::HFloppy.handleLockmem() -> fcb.pilotQueue.queueNext lower word accessed by head\n");
			// this being the last step in updating pilotQueue.queueNext: reset the semaphore to release the queue
			this.fcb.queueSemaphore.set((short)0);
		} else {
			// what is being accessed ...??
			this.logf("IOP::HFloppy.handleLockmem() -> unrecognized access by head ... !!!\n");
		}
	}

	@Override
	public void handleLockqueue(int vAddr, int rAddr) {
		// not relevant for floppy handler
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// save current floppy if present and modified
		if (this.currFloppy != null && this.currFloppy.isChanged()) {
			this.currFloppy.save(errMsgTarget);
		}
		this.currFloppy = null;
	}
	
	/*
	 * ***** implementation of an 6085/daybreak floppy disk ("legacy" floppies only so far, read-only so far, poorly tested so far...)
	 */
	
	private Floppy nextFloppy = null;
	private boolean nextEjected = false;
	
	private Floppy currFloppy = null;
	
	public synchronized boolean insertFloppy(File f, boolean readonly) throws IOException {
		
		String fname = f.getName();
		String[] fnameParts = fname.split("\\.");
		boolean isImd = (fnameParts.length > 1) && ("imd".equalsIgnoreCase(fnameParts[fnameParts.length - 1]));
		boolean isDmk = (fnameParts.length > 1) && ("dmk".equalsIgnoreCase(fnameParts[fnameParts.length - 1]));
		
		if (isImd) {
			this.nextFloppy = new IMDFloppy(f);
		} else if (isDmk) {
			this.nextFloppy = new DMKFloppy(f);
		} else {
			// this.nextFloppy = new Floppy5dot25(f, readonly);
			throw new IOException("Only .imd or .dmk floppies currently supported");
		}
		this.nextEjected = false;
		return this.nextFloppy.isReadonly();
	}
	
	public synchronized void ejectFloppy() {
		this.nextFloppy = null;
		this.nextEjected = true;
	}
	
	private long noMesaFloppyInsertBefore = 0;
	
	private void mesaEjectFloppy() {
		this.currFloppy = null;
		this.nextEjected = false;
		this.noMesaFloppyInsertBefore = System.currentTimeMillis() + 500; // ensure that the mesa machine has a chance to seee that no floppy was (temporarily) inserted
		
		DeviceContextBlock dcb = this.fcb.dcb[0];
		dcb.driveBusy.set(false);
		dcb.pilotDiskChanged.set(false);
		dcb.doorOpen.set(true);
		dcb.statusRegister3.fault.set(false);
		dcb.statusRegister3.writeProtected.set(true);
		dcb.statusRegister3.ready.set(false);
		dcb.statusRegister3.track0.set(true);
		dcb.statusRegister3.twoSided.set(true);
		dcb.statusRegister3.theHeadAddress.set(0);
		// dcb.port80ControlWord ... only Head => Handler ??
		
		// System.out.println("==\n======= no floppy present at mesa level\n==");
	}
	
	private void mesaInsertFloppy() {
		if (this.noMesaFloppyInsertBefore > System.currentTimeMillis()) {
			return;
		}
		if (this.nextFloppy == null || this.currFloppy != null) {
			return;
		}
		
		this.currFloppy = this.nextFloppy;
		this.nextFloppy = null;
		
		DeviceContextBlock dcb = this.fcb.dcb[0];
		dcb.driveBusy.set(false);
		dcb.pilotDiskChanged.set(true);
		dcb.doorOpen.set(false);
		dcb.statusRegister3.fault.set(false);
		dcb.statusRegister3.writeProtected.set(this.currFloppy.isReadonly());
		dcb.statusRegister3.ready.set(true);
		dcb.statusRegister3.track0.set(true);
		dcb.statusRegister3.twoSided.set(true);
		dcb.statusRegister3.theHeadAddress.set(0);
		// dcb.port80ControlWord ... exclusively for Head => Handler ??
		
		// System.out.println("==\n======= new floppy INSERTED at mesa level\n==");
		// System.out.printf("== # cylinder: %d\n", this.currFloppy.getCylinders());
		// System.out.printf("== 1st track  - #sectors: %2d , word-length: %d\n", this.currFloppy.getCyl0Sectors(), this.currFloppy.getTrack0WordsPerSector());
		// System.out.printf("== 2nd track  - #sectors: %2d , word-length: %d\n", this.currFloppy.getCyl0Sectors(), this.currFloppy.getTrack1WordsPerSector());
		// System.out.printf("== data track - #sectors: %2d , word-length: %d\n", this.currFloppy.getDataSectors(), this.currFloppy.getDataWordsPerSector());
		// System.out.println("==");
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		if (this.nextFloppy != null || this.nextEjected) {
			// save current floppy if present and modified
			if (this.currFloppy != null && this.currFloppy.isChanged()) {
				this.currFloppy.save(null);
			}
			
			// eject the old floppy
			if (this.nextEjected) {
				this.mesaEjectFloppy();
			}			
			
			// load the new floppy
			this.mesaInsertFloppy();
		}
	}
	
	private abstract static class Floppy {
		
		// the floppy file
		protected final File f;

		// list of tracks, with track: list of sectors, with sector: array of short
		protected final List<List<short[]>> tracks = new ArrayList<>();
		
		// sector counts in 1st and other cylinders
		protected int cyl0Sectors;
		protected int dataSectors;
		
		// cylinders in floppy
		protected int cylinders;
		
		// sector length on tracks 0, 1 and others
		protected int track0WordsPerSector;
		protected int track1WordsPerSector;
		protected int dataWordsPerSector;
		
		protected Floppy(File f) {
			this.f = f;
		}
		
		public abstract boolean isReadonly();
		
		public abstract boolean isChanged();
		
		public abstract boolean save(StringBuilder errors);
		
		protected void logf(String template, Object... args) {
			if (Config.IO_LOG_FLOPPY) {
				//System.out.printf(template, args);
			}
		}

		public int getCyl0Sectors() {
			return cyl0Sectors;
		}

		public int getDataSectors() {
			return dataSectors;
		}

		public int getCylinders() {
			return cylinders;
		}

		public int getTrack0WordsPerSector() {
			return track0WordsPerSector;
		}

		public int getTrack1WordsPerSector() {
			return track1WordsPerSector;
		}

		public int getDataWordsPerSector() {
			return dataWordsPerSector;
		}
		
		private int nextCyl = 0;
		private int nextHead = 0;
		private int nextSect = 0;
		
		/* cyl & head: 0-based, sect: 1-based (as in Pilot-DiskAddress */
		public short[] getSector(int cyl, int head, int sect) {
			if (cyl < 0 || cyl >= this.cylinders) { return null; }
			if (head < 0 || head >= 2) { return null; }
			if (sect < 1) { return null; }
			if (cyl == 0) {
				if (sect >= this.cyl0Sectors) { return null; }
				this.nextCyl = cyl;
				this.nextHead = head;
				this.nextSect = sect + 1;
			} else {
				if (sect > this.dataSectors) { return null; }
				this.nextCyl = cyl;
				this.nextHead = head;
				this.nextSect = sect + 1;
				if (this.nextSect > this.dataSectors) {
					this.nextSect = 1;
					if (this.nextHead == 0) {
						this.nextHead = 1;
					} else {
						this.nextHead = 0;
						this.nextCyl++;
					}
				}
			}
			return this.tracks.get((cyl * 2) + head).get(sect - 1);
		}

		public int getNextCyl() {
			return nextCyl;
		}

		public int getNextHead() {
			return nextHead;
		}

		public int getNextSect() {
			return nextSect;
		}
		
	}
	
	private static class IMDFloppy extends Floppy {
		
		public IMDFloppy(File f) throws IOException {
			super(f);
			this.loadIMD(f);
		}

		@Override
		public boolean isReadonly() {
			return true;
		}
		
		@Override
		public boolean isChanged() {
			return false;
		}

		@Override
		public boolean save(StringBuilder errors) {
			if (errors != null) {
				errors.append("Writing IMD files currently not supported");
			}
			return false;
		}
		
		/*
		 * IMD specifics
		 */
		
		private static final int sectInfo2Bytesize[] = { 128 , 256 , 512 , 1024 , 2048 , 4096 , 8192 };
		
		private short readWord(BufferedInputStream bis) throws IOException {
			int h = bis.read();
			if (h < 0) { throw new IOException("Premature EOF"); }
			int l = bis.read();
			if (l < 0) { throw new IOException("Premature EOF"); }
			int w = ((h << 8) & 0xFF00) | (l & 0xFF);
			return (short)w;
		}
		
		// returns sector content
		private short[] readSector(BufferedInputStream bis, int sectorSize) throws IOException {
			int contentType = bis.read();
			if (contentType < 0) { throw new IOException("Premature EOF"); }
			
			short[] sector = new short[sectorSize];
			switch(contentType) {
			case 1:
			case 3:
			case 5:
			case 7: {
					for(int i = 0; i < sectorSize; i++) {
						sector[i] = readWord(bis);
					}
				}
				break;
				
			case 2:
			case 4:
			case 6:
			case 8: {
					int b = bis.read();
					int w = ((b << 8) & 0xFF) | (b & 0xFF);
					for(int i = 0; i < sectorSize; i++) {
						sector[i] = (short)w;
					}
				}
				break;
			
			default:
				for(int i = 0; i < sectorSize; i++) {
					sector[i] = 0;
				}
			}
			
			return sector;
		}
		
		// returns the track = list of sectors
		private List<short[]> readTrack(BufferedInputStream bis) throws IOException {
			// get track characteristics
			int mode = bis.read();
			int cylNo = bis.read();
			int headNo = bis.read();
			int numSects = bis.read();
			int sectInfo = bis.read();
			if (mode < 0 || cylNo < 0 || headNo < 0 || numSects < 0 || sectInfo < 0) {
				throw new IOException("Premature EOF");
			}
			
			// sanity checks
			if (numSects == 0) {
				return null;
			}
			if (sectInfo > sectInfo2Bytesize.length) {
				throw new IOException("Invalid IMD file, sectInfo not in [0..5]");
			}
			int sectorByteSize = sectInfo2Bytesize[sectInfo];
			int sectorWordSize = sectorByteSize / 2;
			
			// track sectors
			List<short[]> trackSectors = new ArrayList<>();
			
			logf("Track cyl %d head %d : numSects = %d sectorByteSize = %d\n", cylNo, headNo, numSects, sectorByteSize);
			
			// skip sector numbering map (Xerox floppies have linear ascending sector ordering, as seens so far...)
			for (int i = 0; i < numSects; i++) {
				if (bis.read() < 0) { throw new IOException("Premature EOF"); }
			}
			
			// skip sectorCylinder map if present
			if ((headNo & 0x80) != 0) {
				for (int i = 0; i < numSects; i++) {
					if (bis.read() < 0) { throw new IOException("Premature EOF"); }
				}
			}
			
			// skip sectorHead map if present
			if ((headNo & 0x40) != 0) {
				for (int i = 0; i < numSects; i++) {
					if (bis.read() < 0) { throw new IOException("Premature EOF"); }
				}
			}
			
			// read sectors and append to sector list
			for (int i = 0; i < numSects; i++) {
				trackSectors.add(this.readSector(bis, sectorWordSize));
			}
			
			// done for this track
			return trackSectors;
		}
		
		private void loadIMD(File f) throws IOException {
			if (!f.exists()) {
				logf("file not found\n");
				throw new IOException("Floppy file '" + f.getName() + "' not found");
			}
			
			logf("** start loading IMD floppy: %s\n", f.getName());
			
			try (FileInputStream fis = new FileInputStream(f);
				 BufferedInputStream bis = new BufferedInputStream(fis)) {
				// skip human readable IMD header
				StringBuilder sb = new StringBuilder();
				int b = bis.read();
				while (b >= 0 && b != 0x1A) { // ASCII EOF = end of readable header
					if (b == 0x0A) { // LF
						logf("header :: %s\n", sb.toString());
						sb.setLength(0);
					} else if (b != 0x0D) { // CR 
						sb.append((char)b);
					}
					b = bis.read();
				}
				if (sb.length() > 0) { logf("header :: %s\n", sb.toString()); }
				
				// read track 0
				List<short[]> track0 = this.readTrack(bis);
				this.tracks.add(track0);
				this.cyl0Sectors = track0.size();
				this.track0WordsPerSector = track0.get(0).length;
				
				// read track 1
				List<short[]> track1 = this.readTrack(bis);
				this.tracks.add(track1);
				int track1SectorCount = track1.size();
				if (track1SectorCount != this.cyl0Sectors) {
					logf("### ERROR sector count in track1 (%d) and track0 (%d) differ\n", track1SectorCount, this.cyl0Sectors);
					throw new IOException("Invalid IMD file: sector count in track0 and track1 differ");
				}
				this.track1WordsPerSector = track1.get(0).length;
				
				// read data tracks
				List<short[]> dataTrack = this.readTrack(bis);
				this.tracks.add(dataTrack);
				this.dataSectors = dataTrack.size();
				this.dataWordsPerSector = dataTrack.get(0).length;
				int dataTracks = 1;
				while(bis.available() > 0) {
					List<short[]> trackSectors = this.readTrack(bis);
					if (trackSectors == null) { break; }
					this.tracks.add(trackSectors);
					dataTracks++;
				}
				if ((dataTracks % 2) == 1) {
					throw new IOException("Invalid IMD file: invalid number of data tracks");
				}
				this.cylinders= (dataTracks / 2) + 1; // +1: cylinder 0
			}
			
			logf("** IMD characteristics: cyl0Sectors          = %d\n", cyl0Sectors);
			logf("** IMD characteristics: track0WordsPerSector = %d\n", track0WordsPerSector);
			logf("** IMD characteristics: track1WordsPerSector = %d\n", track1WordsPerSector);
			logf("** IMD characteristics: dataSectors          = %d\n", dataSectors);
			logf("** IMD characteristics: dataWordsPerSector   = %d\n", dataWordsPerSector);
			logf("** IMD characteristics: cylinders            = %d\n", cylinders);
			logf("\n");
		}
	}
	
	private static class DMKFloppy extends Floppy {
		
		public DMKFloppy(File f) throws IOException {
			super(f);
			this.loadDMK(f);
		}

		@Override
		public boolean isReadonly() {
			return true;
		}
		
		@Override
		public boolean isChanged() {
			return false;
		}

		@Override
		public boolean save(StringBuilder errors) {
			if (errors != null) {
				errors.append("Writing DMK files currently not supported");
			}
			return false;
		}
		
		/*
		 * DMK specifics
		 */
		
		private int readByte(BufferedInputStream bis) throws IOException {
			return bis.read() & 0xFF;
		}
		
		private int readWord(BufferedInputStream bis) throws IOException {
			return this.readByte(bis) | (this.readByte(bis) << 8);
		}
		
		private List<short[]> readTrack(int trackNo, BufferedInputStream bis, int rawTrackLength) throws IOException {
			List<short[]> trackSectors = new ArrayList<>();
			
			logf("\n-- begin track %d\n", trackNo);
			// read the max. 64 sector infos
			int[] offsets = new int[64];
			boolean[] isDD = new boolean[64];
			int sectCount = 0;
			logf("idams 0x :");
			for (int i = 0; i < offsets.length; i++) {
				int idam = this.readWord(bis);
				logf(" %04X", idam);
				isDD[i] = (idam & 0x8000) != 0; 
				offsets[i] = (idam & 0x3FFF) - 0x80;
				if (offsets[i] <= 0) { continue; } // not used sector
				sectCount++;
			}
			logf("\n");
			
			// read the sector contents
			int trackLength = rawTrackLength - 0x80;
			byte[] sectorData = new byte[trackLength];
			if (bis.read(sectorData) != sectorData.length) {
				throw new IOException("Short read on sector data");
			}
			
			int[] rawSectLengths = new int[sectCount];
			int lastOffset = trackLength;
			for (int i = sectCount - 1; i >= 0; i--) {
				rawSectLengths[i] = lastOffset - offsets[i];
				lastOffset = offsets[i];
			}
			
			if (trackNo == 0 || trackNo == 1) {
			
				logf("offsets:");
				for (int i = 0; i < sectCount; i++) {
					logf(" %4d", offsets[i]);
				}
				logf("\n");
				
				logf("slength:");
				for (int i = 0; i < sectCount; i++) {
					logf(" %4d", rawSectLengths[i]);
				}
				logf("\n");
				
				if (sectCount != 16 && sectCount != 26) {
					throw new IOException("Invalid Xerox legacy floppy image (unknown geometry)");
				}
	
//				logf("raw track data: ");
//				for (int idx = 0; idx < trackLength; idx++) {
//					if ((idx % 32) == 0) { logf("\n  "); }
//					logf(" %02X", sectorData[idx]);
//				}
//				logf("\n");
			
			}
			
			if (trackNo == 0 && sectorData[offsets[0]] == (byte)0xFE && sectorData[offsets[0]+1] == (byte)0xFE) {
				// xerox disks have single density 128 bytes in first track => all bytes are doubled in DMK
				// (ok, this is lazy, this info must be somehow in the metadata of the track or disk or ...)
				logf("... de-doubling content\n");
				for (int i = 0; i < trackLength/2; i++) {
					sectorData[i] = sectorData[i*2]; 
				}
				trackLength /= 2;
				for (int i = 0; i < sectCount; i++) {
					offsets[i] /= 2;
					rawSectLengths[i] /= 2;
				}
			}
			
			for (int i = 0; i < sectCount; i++) {
				logf("- sector[%2d] : offset = 0x%03X (%04d) , isDD: %5s, length = %d\n", i, offsets[i], offsets[i], (isDD[i]) ? "true" : "false", rawSectLengths[i]);
				logf("    ");
				
				int offset = offsets[i]; // start of raw data for this the sector
				
				for (int idx = 0; idx < 32; idx++) {
					logf(" %02X", sectorData[offset + idx]);
				}
				logf(" ...\n");
				
				int dataStart = offset;
				int signature = sectorData[dataStart++] & 0xFF;
				int cyl = sectorData[dataStart++] & 0xFF;
				int head = sectorData[dataStart++] & 0xFF;
				int sect = sectorData[dataStart++] & 0xFF;
				int sizeCode = sectorData[dataStart++] & 0xFF;
				dataStart += 2; // skip CRC
				
				// overread gap
				for (int gapIdx = 0; gapIdx < 50; gapIdx++) { // MAX_ID_GAP 50
					int b = sectorData[dataStart++] & 0xFF;
					if ((b >= 0xF8) && (b <= 0xFB)) {
						break;
					}
				}
				
				// we should now be at the real sector content start
				int size = 128 << sizeCode;
				logf("    sign 0x%02X  cyl %d  head %d  sect %d  sizeCode %d  size %d :\n",
						signature, cyl, head, sect, sizeCode, size);
//				logf("    ");
//				for (int idx = 0; idx < 32; idx++) {
//					logf(" %02X", sectorData[dataStart + idx]);
//				}
//				logf(" ...\n");
				
				// get the sector content
				short[] sectContent = new short[size / 2];
				for (int idx = 0; idx < size; idx += 2) {
					int b1 = sectorData[dataStart + idx] & 0xFF;
					int b2 = sectorData[dataStart + idx + 1] & 0xFF;
					int w = (b1 << 8) | b2;
					sectContent[idx / 2] = (short)w;
				}

				trackSectors.add(sectContent); // TODO: make sure the position is correct should the sector sequence not be ascendent
				
				logf("sector words: ");
				for (int idx = 0; idx < sectContent.length; idx++) {
					if ((idx % 16) == 0) { logf("\n  "); }
					logf(" %04X", sectContent[idx]);
				}
				logf("\n");
				
				// save the legacy metadata if necessary
				if (i == 0) {
					if (trackNo == 0) {        // special: cyl 0, head 0
						this.cyl0Sectors = sectCount;
						this.track0WordsPerSector = sectContent.length;
					} else if (trackNo == 1) { // special: cyl 0, head 1
						this.track1WordsPerSector = sectContent.length;
					} else if (trackNo == 2) { // data:    any track in cyl 1 .. ?
						this.dataSectors = sectCount;
						this.dataWordsPerSector = sectContent.length;
					}
				}
			}
			
			logf("-- end track %d\n", trackNo);
			return trackSectors;
		}		
		
		private void loadDMK(File f) throws IOException {
			if (!f.exists()) {
				logf("file '%s' not found\n", f.getAbsolutePath());
				throw new IOException("Floppy file '" + f.getName() + "' not found");
			}
			
			logf("** start loading DMK floppy: %s\n", f.getName());
			
			try (FileInputStream fis = new FileInputStream(f);
				 BufferedInputStream bis = new BufferedInputStream(fis)) {
				
				int roFlag = this.readByte(bis);
				int trackCount = this.readByte(bis);
				int trackLength = this.readWord(bis);
				int diskOptions = this.readByte(bis);
				
				// skip 7 bytes (5 bytes read so far)
				for (int i = 0; i < 7; i++) { this.readByte(bis); }
				
				int native1 = this.readWord(bis);
				int native2 = this.readWord(bis);
				
				int realTrackLen = trackLength - 0x80;
				
				// up to here we have read 16 bytes
				
				logf("DMK: r/o: 0x%02X , trackCount = %d , trackLength = %d (real: %d) , options = 0x%02X , native = 0x%04X%04X\n",
						roFlag, trackCount, trackLength, realTrackLen, diskOptions, native2, native1);
				
				for (int i = 0; i < (trackCount * 2); i++) {
					this.tracks.add(this.readTrack(i, bis, trackLength));
					if ((i % 2) == 0) { this.cylinders++; }
				}
			}
			
			logf("** DMK characteristics: cyl0Sectors          = %d\n", cyl0Sectors);
			logf("** DMK characteristics: track0WordsPerSector = %d\n", track0WordsPerSector);
			logf("** DMK characteristics: track1WordsPerSector = %d\n", track1WordsPerSector);
			logf("** DMK characteristics: dataSectors          = %d\n", dataSectors);
			logf("** DMK characteristics: dataWordsPerSector   = %d\n", dataWordsPerSector);
			logf("** DMK characteristics: cylinders            = %d\n", cylinders);
			logf("\n");
		}
	}
}