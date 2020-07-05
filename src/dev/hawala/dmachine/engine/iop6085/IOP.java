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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Opcodes;
import dev.hawala.dmachine.engine.Opcodes.OpImpl;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.eLevelVKey;
import dev.hawala.dmachine.engine.iMesaMachineDataAccessor;
import dev.hawala.dmachine.engine.iUiDataConsumer;
import dev.hawala.dmachine.engine.iop6085.DeviceHandler.MemOperation;
import dev.hawala.dmachine.engine.iop6085.HDisk.VerifyLabelOp;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.IORTable;

/**
 * Emulation of the Daybreak/6085 Input/Output Processor (IOP) board, setting
 * up the device handlers for the supported resp. required devices and dispatching
 * requests from the mesa-machine implementation or through machine-specific instructions
 * to the respective handler(s).
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class IOP extends Mem /* inherit from class Mem for directly accessing the real memory word array ! */ {

	/*
	 * the table of device handlers used by the mesa engine, holding the
	 * FCBs (Function Control Blocks) of the handlers, where each handler
	 * (device) has a predefined index position.
	 * 
	 */
	private static final IORTable iorTable = new IORTable();
	
	/*
	 *  all handlers for devices that this IOP supports, as generic
	 *  list and then as type-specific instances 
	 */
	private static List<DeviceHandler> devHandlers = new ArrayList<>();
	
	private static HBeep hBeep;
	private static HDisplay hDisplay;
	private static HKeyboardMouse hKeyMo;
	private static HDisk hDisk;
	private static HFloppy hFloppy;
	private static HEthernet hEthernet;
	private static HTTY hTty;
	private static HProcessor hProcessor;
	
	/**
	 * setup the IOP and create all necessary device handlers
	 */
	public static void initialize(VerifyLabelOp labelOpOnRead, VerifyLabelOp labelOpOnWrite, VerifyLabelOp labelOpOnVerify, boolean logLabelProblems) {
		
		// install IOP6085 specific instructions
		Opcodes.implantEscOverride(0x87, "zESC.BYTESWAP",  escBYTESWAP);
		Opcodes.implantEscOverride(0x89, "zESC.NOTIFYIOP", escNOTIFYIOP);
		Opcodes.implantEscOverride(0x88, "zESC.LOCKMEM",   escLOCKMEM);
		Opcodes.implantEscOverride(0x86, "zESC.LOCKQUEUE", escLOCKQUEUE);
		
		// register memory updater for transferring data between agent and mesa memory
		Processes.setMesaMemoryUpdater(IOP::processPendingMesaMemoryUpdates);
		
		// register statistics provider
		Processes.setStatisticsProvider(new IOPStatisticsProvider());
		
		// and now the handlers...
		short fcbSegment;
		
		// device handler for beep
		hBeep = new HBeep();
		fcbSegment = hBeep.getFcbSegment();
		iorTable.segments[IOPTypes.HandlerID_beep].ioRegionSegment.set(fcbSegment);
		devHandlers.add(hBeep);
		
		// device handler for keyboard & mouse
		hKeyMo = new HKeyboardMouse();
		fcbSegment = hKeyMo.getFcbSegment();
		iorTable.segments[IOPTypes.HandlerID_keyboardAndMouse].ioRegionSegment.set(fcbSegment);
		devHandlers.add(hKeyMo);
		
		// device handler for display
		hDisplay = new HDisplay(hKeyMo);
		fcbSegment = hDisplay.getFcbSegment();
		iorTable.segments[IOPTypes.HandlerID_display].ioRegionSegment.set(fcbSegment);
		devHandlers.add(hDisplay);
		
		// device handler for processor
		hProcessor  = new HProcessor();
		fcbSegment = hProcessor.getFcbSegment();
		iorTable.segments[IOPTypes.HandlerID_processor].ioRegionSegment.set(fcbSegment);
		devHandlers.add(hProcessor);
		
		// device handler for hard disk(s)
		hDisk = new HDisk(labelOpOnRead, labelOpOnWrite, labelOpOnVerify, logLabelProblems);
		fcbSegment = hDisk.getFcbSegment();
		iorTable.segments[IOPTypes.HandlerID_disk].ioRegionSegment.set(fcbSegment);
		devHandlers.add(hDisk);
		
		// device handler for floppy disk(s)
		hFloppy = new HFloppy();
		fcbSegment = hFloppy.getFcbSegment();
		iorTable.segments[IOPTypes.HandlerID_floppy].ioRegionSegment.set(fcbSegment);
		devHandlers.add(hFloppy);
		
		// device handler for network
		hEthernet = new HEthernet();
		fcbSegment = hEthernet.getFcbSegment();
		iorTable.segments[IOPTypes.HandlerID_ethernet].ioRegionSegment.set(fcbSegment);
		devHandlers.add(hEthernet);
		
		// device handler for tty
		hTty = new HTTY();
		fcbSegment = hTty.getFcbSegment();
		iorTable.segments[IOPTypes.HandlerID_tty].ioRegionSegment.set(fcbSegment);
		devHandlers.add(hTty);
		
	}
	
	/**
	 * Request a write back of all buffered data on all device handlers and
	 * finalize the handlers.
	 * 
	 * @param errMsgTarget for collecting warnings and error messages
	 *   from the handlers during shutdown.
	 */
	public static void shutdown(StringBuilder errMsgTarget) {
		for (DeviceHandler handler : devHandlers) {
			handler.shutdown(errMsgTarget);
		}
	}
	
	/**
	 * Transfer all cached data changes by devices into mesa memory space.  
	 */
	private static void processPendingMesaMemoryUpdates() {
		for (DeviceHandler handler : devHandlers) {
			handler.refreshMesaMemory();
		}
	}
	
	/*
	 * implementation for special 6085 i/o related instructions 
	 */
	
	/**
	 *  BYTESWAP - byte swap in a word
	 */
	private static final OpImpl escBYTESWAP = () -> {
//		System.out.printf("## ESC x87 .. BYTESWAP at 0x%08X+0x%04X [insn# %d]\n", Cpu.CB, Cpu.savedPC, Cpu.insns);
		int val = Cpu.pop() & 0xFFFF;
		int res = (val << 8) | (val >>> 8);
		Cpu.push(res);
	};
	
	/**
	 *  NOTIFYIOP - invocation of a specific device, identified by the mask in the FCB
	 */
	private static final OpImpl escNOTIFYIOP = () -> {
		short notifyMask = Cpu.pop();
		for (DeviceHandler handler : devHandlers) {
			if (handler.processNotify(notifyMask)) {
				return;
			}
		}
	};
	
	/**
	 * LOCKMEM - synchronized/interlocked access to a memory location in the IO-region
	 */
	private static final OpImpl escLOCKMEM = () -> {
		int mask = Cpu.pop() & 0xFFFF;
		int value = Cpu.pop() & 0xFFFF;
		int ioRegionOffset = Cpu.pop() & 0xFFFF;
		int operation = Cpu.pop() & 0xFFFF;
		
		int realLP = IORegion.IOR_BASE + ioRegionOffset;
		int oldValue = mem[realLP] & 0xFFFF;
		final int newValue;
		final MemOperation memOp;
		if (operation == MemOperation.add.code) {
			newValue = oldValue + value;
			memOp = MemOperation.add;
		} else if (operation == MemOperation.and.code) {
			newValue = oldValue & value;
			memOp = MemOperation.and;
		} else if (operation == MemOperation.or.code) {
			newValue = oldValue | value;
			memOp = MemOperation.or;
		} else if (operation == MemOperation.overwriteIfNil.code) {
			newValue = (oldValue != 0) ? oldValue : value;
			memOp = MemOperation.overwriteIfNil;
		} else if (operation == MemOperation.xchg.code) {
			newValue = value;
			memOp = MemOperation.xchg;
		} else {
			throw new IllegalArgumentException("operation is not a valid MemOperation");
		}
		
		for (DeviceHandler handler : devHandlers) {
			handler.handleLockmem((short)mask, realLP, memOp, (short)oldValue, (short)newValue);
		}
		
		short newShortVal = (short)(newValue & 0xFFFF);
		mem[realLP] = newShortVal;
		
//		IORAddress location = IORegion.resolveRealAddress(realLP);
//		System.out.printf("IOP::escLOCKMEM() -> value at rAddr: 0x%04X (%s)\n", mem[realLP], location.getName());
		
		for (DeviceHandler handler : devHandlers) {
			handler.cleanupAfterLockmem((short)mask, realLP);
		}
		
		Cpu.push(oldValue); // the instruction appears to return the oldValue
	};
	
	/**
	 * LOCKQUEUE - lock a queue (until the queue's device is notified?)
	 */
	private static final OpImpl escLOCKQUEUE = () -> {
		int lpQueueVAddr = Cpu.popLong();
		for (DeviceHandler handler : devHandlers) {
			handler.handleLockqueue(lpQueueVAddr, Mem.getRealAddress(lpQueueVAddr, true));
		}
	};
	
	/*
	 * interface between the UI implementation and the UI related devices
	 */
	
	private static class UiCallbacks implements iUiDataConsumer {

		@Override
		public void acceptKeyboardKey(eLevelVKey key, boolean isPressed) {
			if (hKeyMo == null) { return; }
			hKeyMo.handleKeyUsage(key, isPressed);
		}

		@Override
		public void resetKeys() {
			if (hKeyMo == null) { return; }
			hKeyMo.resetKeys();
		}

		@Override
		public void acceptMouseKey(int key, boolean isPressed) {
			if (key == 1) {
				this.acceptKeyboardKey(eLevelVKey.Point, isPressed);
			} else if (key == 2) {
				this.acceptKeyboardKey(eLevelVKey.Menu, isPressed);
			} else if (key == 3) {
				this.acceptKeyboardKey(eLevelVKey.Adjust, isPressed);
			}
		}

		@Override
		public void acceptMousePosition(int x, int y) {
			if (hDisplay == null) { return; }
			hDisplay.recordMouseMoved(x, y);
		}

		@Override
		public void registerPointerBitmapAcceptor(PointerBitmapAcceptor acpt) {
			if (hDisplay == null) { return; }
			hDisplay.setPointerBitmapAcceptor(acpt);
		}

		@Override
		public Supplier<int[]> registerUiDataRefresher(iMesaMachineDataAccessor refresher) {
			Processes.registerUiRefreshCallback(refresher);
			return null;
		}
		
	}
	
	private static UiCallbacks uiCallbacks = null;
	
	public static iUiDataConsumer getUiCallbacks() {
		if (uiCallbacks == null) {
			uiCallbacks = new UiCallbacks();
		}
		return uiCallbacks;
	}
	
	/*
	 * access to statistical data for the I/O devices
	 */
	
	private static class IOPStatisticsProvider implements Processes.StatisticsProvider {
	
		public int getDiskReads() {
			if (hDisk == null) { return 0; }
			return hDisk.getReads();
		}
		
		public int getDiskWrites() {
			if (hDisk == null) { return 0; }
			return hDisk.getWrites();
		}
		
		public int getFloppyReads() {
			if (hFloppy == null) { return 0; }
			return hFloppy.getReads();
		}
		
		public int getFloppyWrites() {
			if (hFloppy == null) { return 0; }
			return hFloppy.getWrites();
		}
		
		public int getNetworkpacketsSent() {
			if (hEthernet == null) { return 0; }
			return hEthernet.getPacketsSentCount();
		}
		
		public int getNetworkpacketsReceived() {
			if (hEthernet == null) { return 0; }
			return hEthernet.getPacketsReceivedCount();
		}
	}
	
	/*
	 * floppy handling operations
	 */
	
	/**
	 * Insert a floppy image into the virtual floppy drive.
	 * If there is currently a floppy loaded, it is first "ejected" normally.
	 * 
	 * @param f the file to use as floppy image
	 * @param readonly if {@code true}, write operations are rejected.
	 * @return {@code true} if the floppy is effectively readonly, i.e. if
	 * {@code readonly} was given as {@code false} but {@code true} is returned,
	 * file passed is readonly and cannot b written.
	 * @throws IOException in case of problems with the virtual floppy file 
	 */
	public static boolean insertFloppy(File f, boolean readonly) throws IOException {
		return hFloppy.insertFloppy(f, readonly);
	}
	
	/**
	 * Remove a floppy image from the virtual floppy drive. As modifications to the
	 * floppy are all buffered in memory, the floppy is written back to the (real) disk
	 * only when eject is called (explicitely or implicitely during {@code insertFloppy})
	 * or when {@code shutdown} is called.
	 */
	public static void ejectFloppy() {
		hFloppy.ejectFloppy();
	}
	
}