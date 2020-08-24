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
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkField;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkWord;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.agents.NetworkHubInterface;
import dev.hawala.dmachine.engine.agents.NetworkInternalTimeService;
import dev.hawala.dmachine.engine.agents.iNetDeviceInterface;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.ClientCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.NotifyMask;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.OpieAddress;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.QueueBlock;
import dev.hawala.dmachine.engine.iop6085.IORegion.BoolField;
import dev.hawala.dmachine.engine.iop6085.IORegion.Field;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;
import dev.hawala.dmachine.engine.iop6085.IORegion.IOStruct;
import dev.hawala.dmachine.engine.iop6085.IORegion.Word;

/**
 * IOP device handler for the network interface of a Daybreak/6085 machine.
 * <p>
 * The network interface is in fact the Nethub-interface for Dodo or a local
 * internal time service, both implemented with the Guam agents.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class HEthernet extends DeviceHandler {
	
	/*
	 * Function Context Block
	 */
	
	private static final String NetworkFCB = "NetworkFCB";

	private interface MesaClientState {
		public static final short off = 0;
		public static final short on = 1;
	}
	
	private static class EHF_SystemControlBlock {
		
		public final Word w0;
		/**/public final Field stat;
		/**/public final Field zeroA;
		/**/public final Field cus;
		/**/public final Field zeroB;
		/**/public final Field rus;
		/**/public final Field zeroC;
		public final Word w1;
		/**/public final Field ack;
		/**/public final Field unusedA;
		/**/public final Field cuc;
		/**/public final BoolField reset;
		/**/public final Field ruc;
		/**/public final Field unusedB;
		public final Word cblOffset; // SCBBase RELATIVE POINTER TO CommandBlock
		public final Word rfaOffset; // SCBBase RELATIVE POINTER TO ReceiveFrameDescriptor
		public final Word crcErrs;
		public final Word alnErrs;
		public final Word rscErrs;
		public final Word ovrnErrs;
		
		public EHF_SystemControlBlock(String name) {
			this.w0 = mkWord(name, "word0");
			/**/this.stat = mkField("stat", this.w0, 0xF000);
			/**/this.zeroA = mkField("zeroA", this.w0, 0x0800);
			/**/this.cus = mkField("cus", this.w0, 0x0700);
			/**/this.zeroB = mkField("zeroB", this.w0, 0x0080);
			/**/this.rus = mkField("rus", this.w0, 0x0070);
			/**/this.zeroC = mkField("zeroC", this.w0, 0x000F);
			this.w1 = mkWord(name, "word1");
			/**/this.ack = mkField("ack", this.w1, 0xF000);
			/**/this.unusedA = mkField("unusedA", this.w1, 0x0800);
			/**/this.cuc = mkField("cuc", this.w1, 0x0700);
			/**/this.reset = mkBoolField("", this.w1, 0x0080);
			/**/this.ruc = mkField("ruc", this.w1, 0x0070);
			/**/this.unusedB = mkField("unusedB", this.w1, 0x000F);
			this.cblOffset = mkWord(name, "cblOffset");
			this.rfaOffset = mkWord(name, "rfaOffset");
			this.crcErrs = mkWord(name, "crcErrs");
			this.alnErrs = mkWord(name, "alnErrs");
			this.rscErrs = mkWord(name, "rscErrs");
			this.ovrnErrs = mkWord(name, "ovrnErrs");
		}
	}
	
	private static class NonMesaContext {
		
		// fake non mesa context by reserving 160 words (length of NonMesaContext)
		// and see if Pilot accesses it in any way
		public final Word[] nmc;
		
		public NonMesaContext(String name) {
			this.nmc = new Word[160];
			for (int i = 0; i < this.nmc.length; i++) {
				this.nmc[i] = mkWord(name, "word[" + i + "]");
			}
		}
		
	}
	
	private static class FCB implements IORAddress {
		private final int startAddress;
		
		public final QueueBlock mesaOutQueue;
		public final QueueBlock mesaInQueue;
		public final Word mesaClientStateRequest; // ==> MesaClientState
		
		public final EHF_SystemControlBlock scb;
		public final NotifyMask etherOutWorkMask;
		public final NotifyMask etherInWorkMask;
		public final Word etherLockMask;
		public final Word mesaInClientState; // ==> MesaClientState
		public final Word mesaOutClientState; // ==> MesaClientState
		
		public final Word iopEtherOutQueSemaphore;
		public final Word mesaEtherOutQueSemaphore;
		public final Word iopEtherInQueSemaphore;
		public final Word mesaEtherInQueSemaphore;
		
		public final NonMesaContext nonMesaContext;
		
		private FCB() {
			this.startAddress = IORegion.syncToSegment() + IORegion.IOR_BASE;
			
			this.mesaOutQueue = new QueueBlock(NetworkFCB, "mesaOutQueue");
			this.mesaInQueue = new QueueBlock(NetworkFCB, "mesaInQueue");
			this.mesaClientStateRequest = mkWord(NetworkFCB, "mesaClientStateRequest");
			
			this.scb = new EHF_SystemControlBlock(NetworkFCB + ".scb");
			this.etherOutWorkMask = new NotifyMask(NetworkFCB, "etherOutWorkMask");
			this.etherInWorkMask = new NotifyMask(NetworkFCB, "etherInWorkMask");
			this.etherLockMask = mkWord(NetworkFCB, "etherLockMask");
			this.mesaInClientState = mkWord(NetworkFCB, "mesaInClientState");
			this.mesaOutClientState = mkWord(NetworkFCB, "mesaOutClientState");
			
			this.iopEtherOutQueSemaphore = mkWord(NetworkFCB, "iopEtherOutQueSemaphore");
			this.mesaEtherOutQueSemaphore = mkWord(NetworkFCB, "mesaEtherOutQueSemaphore");
			this.iopEtherInQueSemaphore = mkWord(NetworkFCB, "iopEtherInQueSemaphore");
			this.mesaEtherInQueSemaphore = mkWord(NetworkFCB, "mesaEtherInQueSemaphore");
			
			this.nonMesaContext = new NonMesaContext(NetworkFCB + ".nonMesaContext");
			
			// initialize masks for communication with mesahead
			this.etherOutWorkMask.byteMaskAndOffset.set(mkMask());
			this.etherInWorkMask.byteMaskAndOffset.set(mkMask());
			this.etherLockMask.set(mkMask());
		}

		@Override
		public String getName() {
			return NetworkFCB;
		}

		@Override
		public int getRealAddress() {
			return this.startAddress;
		}
		
	}
	
	/*
	 * Input Output Control Block
	 */
	
	private static final String NetworkIOCB = "NetworkIOCB";
	
	private interface OpType {
		public final short command = 0;
		public final short output = 1;
		public final short reset = 2;
		public final short startRU = 3;
		public final short input = 15;
	}
	
	private static class IOCB extends IOStruct {
		
		public final OpieAddress next;
		public final ClientCondition clientCondition;
		public final Word i586Status; // uninterpreted for now, see if Pilot accesses it...
		public final Word w6;
		/**/public final BoolField status_done;
		/**/public final BoolField status_handled;
		/**/public final BoolField status_okay;
		/**/public final BoolField status_frameTooLong;
		/**/public final BoolField status_interruptTimeout;
		/**/public final Field status_unused;
		/**/public final BoolField status_isDequeued;
		/**/public final Field opType; // { command(0), output(1), reset(2), startRU(3), input(15) }
		/****/public final BoolField op_io_dontProcess;
		/****/public final OpieAddress op_io_address; // ... this is word7...
		/****/public final Word op_io_length; // in bytes (input: max. bytes receivable, output: bytes to send)
		/****/public final Word op_io_count; // in bytes (input: bytes received)
		// CommandSelect has max. 7 words, starting at word 7 (overlapping with the 4 words [op_io_address..op_io_count])
		// => total IOCB length == 14
		// => 3 words for CommandSelect still missing
		// no idea *when* / *why* the command variant is ised, so for now: use dummies until that's got clearer..
		/****/public final Word op_command_select_word11;
		/****/public final Word op_command_select_word12;
		/****/public final Word op_command_select_word13;
		
		public IOCB(int base) {
			super(base, NetworkIOCB);
			
			this.next = new OpieAddress(this, "next");
			this.clientCondition = new ClientCondition(this, "clientCondition");
			this.i586Status = mkWord("i586Status");
			this.w6 = mkWord("word6");
			/**/this.status_done = mkBoolField("status.done", this.w6, 0x8000);
			/**/this.status_handled = mkBoolField("status.handled", this.w6, 0x4000);
			/**/this.status_okay = mkBoolField("status.okay", this.w6, 0x2000);
			/**/this.status_frameTooLong = mkBoolField("status.frameTooLong", this.w6, 0x1000);
			/**/this.status_interruptTimeout = mkBoolField("status.interruptTimeout", this.w6, 0x0800);
			/**/this.status_unused = mkField("status.unused", this.w6, 0x0600);
			/**/this.status_isDequeued = mkBoolField("status.isDequeued", this.w6, 0x0100);
			/**/this.opType = mkField("op.type", this.w6, 0x00F0);
			/****/this.op_io_dontProcess = mkBoolField("op.io.dontProcess", this.w6, 0x0008);
			/****/this.op_io_address = new OpieAddress(this, "op.io.address");
			/****/this.op_io_length = mkByteSwappedWord("op.io.length"); // not defined as such but (always?) byte-swapped by head
			/****/this.op_io_count = mkByteSwappedWord("op.io.count"); // not defined as such (always?) byte-swapped by head
			// see above for first 4 words of Commandselect overlapping starting from op_io_address
			/****/this.op_command_select_word11 = mkWord("op.command.select.word11");
			/****/this.op_command_select_word12 = mkWord("op.command.select.word12");
			/****/this.op_command_select_word13 = mkWord("op.command.select.word13");
			
			this.endStruct();
		}
		
		public void dump(PrintStream ps, String prefix) {
			int aNext = this.next.toLP();
			short intrMask = this.clientCondition.maskValue.get();
			String status = this.status_done.is() ? " done" : "";
			status += this.status_handled.is() ? " handled" : "";
			status += this.status_okay.is() ? " ok" : "";
			status += this.status_frameTooLong.is() ? " frameTooLong" : "";
			status += this.status_interruptTimeout.is() ?  "interruptTimeout" : "";
			status += this.status_isDequeued.is() ? " dequeued" : " ";
			int opTypeVal = this.opType.get(); // { command(0), output(1), reset(2), startRU(3), input(15) }
			String opType = (opTypeVal == 0)
				? "command" : (opTypeVal == 1)
				? "output" : (opTypeVal == 2)
				? "reset" : (opTypeVal == 3)
				? "startRU" : (opTypeVal == 15)
				? "input"
				: "invalid(" + opTypeVal + ")";
			ps.printf(
				"%s-> IOCB @ 0x%08X [%s next = 0x%08X , intrMask = 0x%04X , status =%s , opType = %s%s , bAddr = 0x%08X , length = %d , count = %d ]\n",
				prefix,
				this.getRealAddress(),
				this.op_io_dontProcess.is() ? " DONT_PROCESS" : "",
				aNext,
				intrMask,
				status,
				opType,
				this.op_io_dontProcess.is() ? " dontProcess" : "",
				this.op_io_address.toLP(),
				this.op_io_length.get(),
				this.op_io_count.get()
				);
		}
		
	}
	
	
	/*
	 * statistical data
	 */
	
	private int packetsSent = 0;
	private int packetsReceived = 0;

	public int getPacketsSentCount() { return this.packetsSent; }
	
	public int getPacketsReceivedCount() { return this.packetsReceived; }
			
	/*
	 * implementation
	 */
	
	// central component: Function Control Block
	private final FCB fcb;
	
	// other constants for network
	private static final int MaxPacketSize = 1536; // max. ethernet packet size, XNS uses max. 576 bytes = 546 payload + 30 header
	
	// configuration parameters
	private static String hubHostname = "";
	private static int hubPort = 0;
	private static int localTimeOffsetMinutes = 0;
	
	/**
	 * Configure the (next created instance of the) handler with connection data
	 * to the NetHub or as backup the internal (local) time service parameters. 
	 * 
	 * @param hostname
	 * 		the name of the NetHub host
	 * 		(or {@code null} or empty string for no NetHub connection)
	 * @param port
	 * 		the port where the NetHub is listening 
	 * 		(or {@code 0} (zero) for no NetHub connection)
	 * @param fallbackLocalTimeOffsetMinutes
	 * 		if no NetHub is used: difference between local time and GMT in
	 * 		minutes, with positive values being to the east and negative
	 * 		to the west (e.g. Germany is +60 without DST and +120 with DST
	 * 		whereas Alaska should be -560 without DST resp -480 with DST).
	 */
	public static void setHubParameters(String hostname, int port, int fallbackLocalTimeOffsetMinutes) {
		hubHostname = hostname;
		hubPort = port;
		localTimeOffsetMinutes = fallbackLocalTimeOffsetMinutes;
	}
	
	// the network device the handler is connected to
	private iNetDeviceInterface netIf = null;
	
	// packet receiving status
	private boolean receiveStopped = true; // is receiving packets stopped and are all incoming packets to be dropped?
	
	// some status infos
	private boolean hearSelf = false;
	
	// the (relocatable) working IOCBs
	private final IOCB workIocb = new IOCB(0);
	private final IOCB tmpIocb = new IOCB(0);
	
	// list of IOCB addresses ready to receive packets
	private final Queue<Integer> receiveIocbs = new LinkedList<>();
	
	// temp buffer for word <-> byte conversion of packets
	private final byte[] packetBuffer = new byte[2048];
	
	public HEthernet() {
		super(NetworkFCB, Config.IO_LOG_NETWORK);
		this.fcb = new FCB();
		
		if (hubHostname != null && !hubHostname.isEmpty()
			&& hubPort > 0 && hubPort < 0xFFFF) {
			this.netIf = new NetworkHubInterface(hubHostname, hubPort);
		} else {
			this.netIf = new NetworkInternalTimeService(localTimeOffsetMinutes);
		}
		this.netIf.setNewPacketNotifier(() -> {
			logf("\n+++ requesting datarefresh for new packet (at: %s , insns = %d)\n", getNanoMs(), Cpu.insns);
			Processes.requestDataRefresh();
		});
	}

	@Override
	public int getFcbRealAddress() {
		return this.fcb.getRealAddress();
	}

	@Override
	public short getFcbSegment() {
		return this.fcb.getIOPSegment();
	}
	
	private static final short STATUS_TRANSMIT_NONE = (short)0x8000; // completion
	private static final short STATUS_TRANSMIT_COLLISIONS = (short)0x902F; // completion, aborted, tooManyCollisions, collisions=15 
	private static final short STATUS_TRANSMIT_OTHER = (short)0x9010; // completion, aborted, unusedB 
	
	private static int wordSwapBytes(int val) {
		val &= 0xFFFF;
		int res = (val << 8) | (val >>> 8);
		return res & 0xFFFF;
	}

	@Override
	public boolean processNotify(short notifyMask) {
		if (notifyMask != this.fcb.etherInWorkMask.byteMaskAndOffset.get()
			&& notifyMask != this.fcb.etherOutWorkMask.byteMaskAndOffset.get()) {
			// not for us, let an other handler take care of this
			return false;
		}
		
		this.logf(
			"IOP::HEthernet.processNotify() -- working for ether%sWorkMask at 0x%08X+0x%04X [insn# %d ]\n",
			(notifyMask == this.fcb.etherInWorkMask.byteMaskAndOffset.get()) ? "In" : "Out", Cpu.CB, Cpu.savedPC, Cpu.insns);
		
		// check for stopping transmissions
		if (this.fcb.mesaClientStateRequest.get() == MesaClientState.off) {
			logf("IOP::HEthernet.processNotify() -> stop transmissions\n");
			
			this.fcb.mesaInClientState.set(MesaClientState.off);
			this.fcb.mesaOutClientState.set(MesaClientState.off);
			this.receiveStopped = true;
			
			// drop pending packets in network interface, iocb queue and interrupt queue
			// drain ingone packets currently queued
			if (this.netIf != null) {
				while(this.netIf.dequeuePacket(this.packetBuffer, this.packetBuffer.length) > 0) {
					// ignore packets dropped
				}
			}
			this.receiveIocbs.clear();
			logf("IOP::HEthernet.processNotify() -> end\n");
			return true;
		}
		
		// (re)start transmissions
		this.fcb.mesaInClientState.set(MesaClientState.on);
		this.fcb.mesaOutClientState.set(MesaClientState.on);
		if (this.receiveStopped) {
			this.receiveIocbs.clear();
			logf("IOP::HEthernet.processNotify() -> (re)starting transmissions\n");
		}
		this.receiveStopped = false;
		
		// handle new input buffer enqueued to receive ingoing packets (IOCBs are in fcb.etherInWorkMask) 
		if (notifyMask == this.fcb.etherInWorkMask.byteMaskAndOffset.get()) {
			int recvCnt = 0;
			boolean hasNewInputBuffer = false;
			int iocbAddr = this.fcb.mesaInQueue.queueHead.toLP();
			logf("IOP::HEthernet.processNotify() -> fcb.mesaInQueue.queueHead as LP: 0x%06X\n", iocbAddr);
			while (iocbAddr != 0) {
				this.workIocb.rebaseToVirtualAddress(iocbAddr);
				if (this.workIocb.op_io_dontProcess.is()) {
					logf("         recvIocb[%d] -> recvIocb = 0x%08X => dontProcess !\n", recvCnt, iocbAddr);
					recvCnt++;
					continue;
				}
				int opType = this.workIocb.opType.get();
				if (opType == OpType.input) {
					int bufferAddress = this.workIocb.op_io_address.toLP();
					int bufferLength = this.workIocb.op_io_length.get();
					logf("         recvIocb[%d] -> recvIocb = 0x%08X , bufferAddress = 0x%08X , bufferLength = %d\n",
							recvCnt, iocbAddr, bufferAddress, bufferLength);
					
					this.enqueueReceiveIocb(iocbAddr);
					recvCnt++;
					hasNewInputBuffer = true;
				}
				
				iocbAddr = this.workIocb.next.toLP();
			}
			if (hasNewInputBuffer) {
				refreshMesaMemory(); // transfer received packets already waiting (will be a no-op if none waiting)
			}
			logf("IOP::HEthernet.processNotify() -> end\n");
			return true;
		}
		
		// here all IOCBs to process are in fcb.etherInWorkMask, but these can do different operations
		short interruptsToRaise = 0;
		int sendCnt = 0;
		int iocbAddr = this.fcb.mesaOutQueue.queueHead.toLP();
		logf("IOP::HEthernet.processNotify() -> fcb.mesaOutQueue.queueHead as LP: 0x%06X\n", iocbAddr);
		while (iocbAddr != 0) {
			this.workIocb.rebaseToVirtualAddress(iocbAddr);
			if (this.logging) { this.workIocb.dump(System.out, "         sendIocb[" + sendCnt + "] "); }
			if (this.workIocb.op_io_dontProcess.is()) {
				logf("         sendIocb[%d] -> op.io.dontProcess == true => ignoring IOCB\n", sendCnt);
				sendCnt++;
				iocbAddr = this.workIocb.next.toLP();
				continue;
			}
			int opType = this.workIocb.opType.get(); // { command(0), output(1), reset(2), startRU(3), input(15) }
			switch(opType) {
				
				case OpType.output: {
					int bufferAddress = this.workIocb.op_io_address.toLP(); // TODO: handle non-VM result (?page, ?IOP-address)
					int bufferLength = this.workIocb.op_io_length.get();

					this.workIocb.i586Status.set(STATUS_TRANSMIT_NONE);
					
					if (bufferAddress == 0 || bufferLength < NetworkHubInterface.MIN_NET_PACKET_LEN) {
						logf("         sendIocb[%d] -> null buffer or too short\n", sendCnt);
						this.workIocb.i586Status.set(STATUS_TRANSMIT_OTHER);
						this.workIocb.status_okay.set(false);
					} else if (bufferLength > Math.min(NetworkHubInterface.MAX_NET_PACKET_LEN, MaxPacketSize)) {
						logf("         sendIocb[%d] -> too long\n", sendCnt);
						this.workIocb.status_frameTooLong.set(true);
						this.workIocb.status_okay.set(false);
					} else if (this.netIf == null) {
						logf("         sendIocb[%d] -> netIf is null\n", sendCnt);
						this.workIocb.i586Status.set(STATUS_TRANSMIT_COLLISIONS);
						this.workIocb.status_okay.set(false);
					} else {
						int bpos = 0;
						for (int i = 0; i < (bufferLength + 1)/2; i++) {
							short w = Mem.readWord(bufferAddress + i);
							this.packetBuffer[bpos++] = (byte)(w >>> 8);
							this.packetBuffer[bpos++] = (byte)(w & 0xFF);
						}
						int trfLength = this.netIf.enqueuePacket(this.packetBuffer, bufferLength, hearSelf);
						if (trfLength == bufferLength) {
							this.workIocb.status_frameTooLong.set(false);
							this.workIocb.status_okay.set(true);
							logf("         sendIocb[%d] -> successfully enqueued to netIf\n", sendCnt);
						} else {
							logf("         sendIocb[%d] -> enqueued to netIf but trfLength = %d\n", sendCnt, trfLength);
							this.workIocb.status_frameTooLong.set(true);
							this.workIocb.status_okay.set(false);
						}
						this.workIocb.op_io_count.set((short)trfLength);
						
						interruptsToRaise |= this.workIocb.clientCondition.maskValue.get();
						
						packetsSent++;
					}
					this.workIocb.status_done.set(true);
					
					break;
				}
				
				case OpType.reset: {
					// simulate a reset by stopping all (see above)
					this.fcb.mesaInClientState.set(MesaClientState.off);
					this.fcb.mesaOutClientState.set(MesaClientState.off);
					this.receiveStopped = true;
					
					// drop pending packets in network interface, iocb queue and interrupt queue
					// drain incoming packets
					if (this.netIf != null) {
						while(this.netIf.dequeuePacket(this.packetBuffer, this.packetBuffer.length) > 0) {
							// ignore packets dropped
						}
					}
					this.receiveIocbs.clear();
					
					this.workIocb.status_okay.set(true);
					logf("         sendIocb[%d] -> resetted handler\n", sendCnt);
					break;
				}
				
				case OpType.command: {
					// TODO: implement the actions...
					
					// for now we "assume" that anything will be configured to our defaults ...
					this.workIocb.status_okay.set(true);
					logf("         sendIocb[%d] -> command assumed ok (but ignored)\n", sendCnt);
					break;
				}
				
				case OpType.startRU: {
					this.receiveStopped = false;
					this.workIocb.status_okay.set(true);
					logf("         sendIocb[%d] -> (re)started receive unit\n", sendCnt);
					break;
				}
				
				default: // unknown opType (or input enqueued in the wrong queue) ??
					logf("         sendIocb[%d] -> invalid opType %d\n", sendCnt, opType);
					this.workIocb.status_okay.set(false);
					
			}
			this.workIocb.status_done.set(true);
			this.workIocb.status_handled.set(true);
			
			sendCnt++;
			iocbAddr = this.workIocb.next.toLP();
		}
		
		// enqueue "packet(s) sent" interrupt
		if (interruptsToRaise != 0) {
			Processes.requestMesaInterrupt(interruptsToRaise);
			logf("     requested MesaInterrupt(0x%04X)\n", interruptsToRaise);
		}
		
		// done
		logf("IOP::HEthernet.processNotify() -> end\n");
		return true;
	}

	@Override
	public void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue) {
		// check if it is for us
		if (lockMask != this.fcb.etherLockMask.get()) { return; }
		
		// log the lockmem operation
		this.logf("IOP::HEthernet.handleLockmem( rAddr = 0x%06X , memOp = %s , oldValue = 0x%04X , newValue = 0x%04X )\n",
				realAddress, memOp.toString(), oldValue, newValue);
		
		// handle handshake for setting one of the 2 semaphore words for one of the 2 queues 
		IORAddress location = IORegion.resolveRealAddress(realAddress);
		if (location == this.fcb.iopEtherInQueSemaphore) {
			// phase 1 of locking access to the mesaInQueue
			this.logf("IOP::HEthernet.handleLockmem() -> accessed fcb.iopEtherInQueSemaphore\n"); 
		} else if (location == this.fcb.mesaEtherInQueSemaphore) {
			// phase 2 of locking access to the mesaInQueue
			this.logf("IOP::HEthernet.handleLockmem() -> accessed fcb.mesaEtherInQueSemaphore\n");
			this.fcb.iopEtherInQueSemaphore.set((short)0);
		} else if (location == this.fcb.iopEtherOutQueSemaphore) {
			// phase 1 of locking access to the mesaOutQueue
			this.logf("IOP::HEthernet.handleLockmem() -> accessed fcb.iopEtherOutQueSemaphore\n"); 
		} else if (location == this.fcb.mesaEtherOutQueSemaphore) {
			// phase 2 of locking access to the mesaOutQueue
			this.logf("IOP::HEthernet.handleLockmem() -> accessed fcb.mesaEtherOutQueSemaphore\n");
			this.fcb.iopEtherOutQueSemaphore.set((short)0);
		}
		
	}

	@Override
	public void handleLockqueue(int vAddr, int rAddr) {
		// possibly used by mesa-head for synchronizing access to queue-heads
		// pointless here, as queue manipulation occurs only:
		// - during LOCKMEM or NOTIFYIOP instructions
		// - between instructions during refreshMesaMemory()
		this.logf("IOP::HEthernet.handleLockqueue( vAddr = 0x%06X , rAddr = 0x%06X )", vAddr, rAddr);
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		boolean logIntro = true;
		
		// no network => nothing to do...
		if (this.netIf == null) { return; }
		
		// did Pilot stop packet transmission?
		if (this.receiveStopped) {
			// drain incoming packets
			while(this.netIf.dequeuePacket(this.packetBuffer, this.packetBuffer.length) > 0) {
				// ignore packet content
			}
		}
		
		// move ingone packets into waiting receive iocbs
		boolean didInterrupt = false;
		while(!this.receiveIocbs.isEmpty()) {
			int packetByteCount = this.netIf.dequeuePacket(this.packetBuffer, this.packetBuffer.length);
			if (packetByteCount < 1) { return; }
			
			if (logIntro) {
				logIntro = this.logTimeIntro();
				logf("begin refreshMesaMemory()\n");
			}
			
			int recvIocb = this.dequeueReceiveIocb();
			this.workIocb.rebaseToVirtualAddress(recvIocb);
			
			int bufferAddress = this.workIocb.op_io_address.toLP(); // TODO: handle non-VM address-type (?page, ?IOP-address)
			int bufferByteLength = this.workIocb.op_io_length.get();
			
			int trfBytes = Math.min(packetByteCount, bufferByteLength);
			int wlen = 0;
			logf("\n -- raw packet data\n");
			for(int i = 0; i < trfBytes; i += 2) {
				if ((i % 32) == 0) { slogf("\n 0x%03X : ", i); }
				int b1 = ((this.packetBuffer[i] & 0x00FF) << 8);
				int b2 = this.packetBuffer[i+1] & 0x00FF;
				short w = (short)(b1 | b2);
				slogf(" %04X", w);
				Mem.writeWord(bufferAddress + wlen, w);
				wlen++;
			}
			slogf("\n\n");
			this.workIocb.op_io_count.set((short)(wlen * 2));
			
			final String status;
			if (trfBytes == packetByteCount) {
				this.workIocb.status_okay.set(true);
				status = "okay";
			} else {
				this.workIocb.status_okay.set(false);
				this.workIocb.status_frameTooLong.set(true);
				status = "frameTooLong";
			}
			this.workIocb.status_done.set(true);
			this.workIocb.status_handled.set(true);
			
			dumpIocb(recvIocb, true);
			slogf("\n");
			
			this.packetsReceived++;
			logf("     transfered %d words (trfBytes %d, bufferLen %d) to Mesa memory at 0x%08X => status = %s\n", wlen, trfBytes, bufferByteLength, bufferAddress, status);
			
			if (!didInterrupt) {
				short recvIntrMask = this.workIocb.clientCondition.maskValue.get();
				Processes.requestMesaInterrupt(recvIntrMask);
				didInterrupt = true;
				logf("     requested MesaInterrupt(0x%04X)\n", recvIntrMask);
			}
		}
		if (!logIntro) {
			logf("done refreshMesaMemory()\n");
		}
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		if (this.netIf != null) {
			this.netIf.shutdown();
			this.netIf = null;
		}
	}
	
	private void enqueueReceiveIocb(int iocb) {
		if (iocb == 0) { return; }
		
		this.workIocb.rebaseToVirtualAddress(iocb);
		
		if (!this.receiveIocbs.isEmpty()) {
			int bufferAddress = this.workIocb.op_io_address.toLP();
			boolean doRemove = false;
			for (int queued : this.receiveIocbs) {
				this.tmpIocb.rebaseToVirtualAddress(queued);
				int queuedAddress = this.tmpIocb.op_io_address.toLP();
				if (queuedAddress == bufferAddress) {
					doRemove = true;
					break;
				}
			}
			if (doRemove) {
				logf("     ** removed iocb 0x%08X with same buffer address\n", iocb);
				this.receiveIocbs.remove(Integer.valueOf(iocb));
			}
		}
		
		this.workIocb.status_done.set(false);
		this.workIocb.status_handled.set(false);
		
		this.receiveIocbs.add(iocb);
		
		logf("     enqueued iocb 0x%08X\n", iocb);
		dumpReceiveIocbs();
	}
	
	private int dequeueReceiveIocb() {
		if (this.receiveIocbs.isEmpty()) { return 0; }
		
		int currIocb = this.receiveIocbs.remove();
		
		logf("     dequeued iocb 0x%08X\n", currIocb);
		dumpIocb(currIocb);
		dumpReceiveIocbs();
		
		return currIocb;
	}
	
	private void dumpIocb(int iocbAddr) {
		this.dumpIocb(iocbAddr, false);
	}
	
	private void dumpIocb(int iocbAddr, boolean dumpBuffer) {
		if (!this.logging) { return; }
		this.tmpIocb.rebaseToVirtualAddress(iocbAddr);
		this.tmpIocb.dump(System.out, "");
		
		if (dumpBuffer) {
			int bAddr = this.tmpIocb.op_io_address.toLP();
			int actLen = this.tmpIocb.op_io_count.get();
			int wlen = (actLen + 1) / 2; 
			for(int i = 0; i < wlen; i++) {
				if ((i % 16) == 0) { slogf("\n 0x%03X : ", i); }
				int w = Mem.readWord(bAddr + i) & 0xFFFF;
				slogf(" %04X", w);
			}
			slogf("\n");
		}
	}
	
	private void dumpReceiveIocbs() {
		logf("     receive iocb queue now:\n");
		for (int iocb : receiveIocbs) {
			dumpIocb(iocb);
		}
		logf("     -----------------------\n");
	}
	
	private static String getNanoMs() {
		long nanoTs = System.nanoTime();
		return String.format("%9d.%06d ms", nanoTs / 1000000, nanoTs % 1000000);
	}
	
	// returns if log must be done
	private boolean logTimeIntro() {
		slogf("\n\n--\n-- at %s (insns: %d)\n--\n", getNanoMs(), Cpu.insns);
		return false;
	}
	
	/*
	 * loading boot files over the network using the simpleRequest/simpleData boot protocol
	 * (only the germ, as (initial or mesa) microcodes are irrelevant and the boot file proper
	 * is loaded by the germ using the spp boot protocol) 
	 */ 
	
	private static final byte[] bootBuffer = new byte[1024];
	
	private static void putWord(int wordAt, short w) {
		int byteAt = wordAt * 2;
		if (byteAt < 0 || byteAt > bootBuffer.length) { return; }
		bootBuffer[byteAt] = (byte)((w >> 8) & 0xFF);
		bootBuffer[byteAt + 1] = (byte)(w & 0xFF);
	}
	
	private static int getWord(int wordAt) {
		int byteAt = wordAt * 2;
		if (byteAt < 0 || byteAt > (bootBuffer.length - 1)) { return 0; }
		int b0 = (bootBuffer[byteAt] & 0xFF);
		int b1 = (bootBuffer[byteAt + 1] & 0xFF);
		return ((b0 << 8) | b1);
	}
	
	private static void sendPacket(iNetDeviceInterface zeNet, int wordLength) {
		zeNet.enqueuePacket(bootBuffer, 2 * wordLength, false);
	}
	
	private static int /* length in words */ recvPacket(iNetDeviceInterface zeNet) {
		int byteLength = zeNet.dequeuePacket(bootBuffer, bootBuffer.length);
		return (byteLength + 1) / 2;
	}
	
	public static boolean loadFileFromBootService(
			int mac0, int mac1, int mac2,
			long bfn,
			List<short[]> germ) {
		// get us a network interface
		if (hubHostname == null || hubHostname.isEmpty() || hubPort < 1 || hubPort > 0xFFFF) {
			return false;
		}
		iNetDeviceInterface zeNet = new NetworkHubInterface(hubHostname, hubPort);
		final short localSocket = 1234;
		
		// where to collect the packets
		short[][] pages = new short[1024][]; // array of 1024 packets/pages
		int lastPage = -1; // highest index used in pages
		boolean ok = true;
		
		// send the request after building the packet component-wise (idp-payload: 4 words => idp-length = 19 words)
		int bfn0 = (int)((bfn >> 32) & 0xFFFF);
		int bfn1 = (int)((bfn >> 16) & 0xFFFF);
		int bfn2 = (int)(bfn & 0xFFFF);
		
		// eth: dst
		putWord(0, (short)0xFFFF);
		putWord(1, (short)0xFFFF);
		putWord(2, (short)0xFFFF);
		
		// eth: src
		putWord(3, (short)mac0);
		putWord(4, (short)mac1);
		putWord(5, (short)mac2);
		
		// eth: type
		putWord(6, (short)0x0600);
		
		// idp: ckSum
		putWord(7, (short)0xFFFF); // no checksum
		
		// idp: length
		putWord(8, (short)38); // payload length (19 words -> 38 bytes)
		
		// idp: transport control & packet type
		putWord(9, (short)9); // hop count = 0 & packet type = BOOT_SERVER_PACKET
		
		// idp: destination endpoint: copy the source destination of the ingone packet
		putWord(10, (short)0); // local network
		putWord(11, (short)0);
		putWord(12, (short)0xFFFF); // broadcast
		putWord(13, (short)0xFFFF);
		putWord(14, (short)0xFFFF);
		putWord(15, (short)10); // BOOT socket
		
		// idp: source endpoint: put "our" address with the "local" net and "our" socket
		putWord(16, (short)0); // local network
		putWord(17, (short)0);
		putWord(18, (short)mac0); // our machine id
		putWord(19, (short)mac1);
		putWord(20, (short)mac2);
		putWord(21, (short)localSocket); // our socket
		
		// boot: etherBootPacketType simpleRequest
		putWord(22, (short)1);
		
		// boot: bootFileNumber
		putWord(23, (short)bfn0);
		putWord(24, (short)bfn1);
		putWord(25, (short)bfn2);
		
		// and send it
		sendPacket(zeNet, 30); // real ethernet length is 26 words, but minimal length is 60 bytes
		
		boolean done = false;
		while (!done && ok) {
			// wait a bit and check for next packet
			try { Thread.sleep(2); } catch (InterruptedException e) { break; }
			int wLen = recvPacket(zeNet);
			if (wLen == 0) {
				continue;
			}
			
			// is it for us? (i.e.: our mac-address, our socket, etherBootPacketType simpleData and requested bootfile)
			if (getWord(12) == mac0
					&& getWord(13) == mac1
					&& getWord(14) == mac2
					&& getWord(15) == localSocket
					&& getWord(22) == 2
					&& getWord(23) == bfn0
					&& getWord(24) == bfn1
					&& getWord(25) == bfn2) {
				int payloadWords = (getWord(8) + 1) / 2;
				int pageNo = getWord(26) - 1;
				int pageWordCount = payloadWords - 20; // payload starts at word offset 7, page content starts at word offset 27
				
				if (pageNo >= 0 && pageNo < pages.length && pageWordCount == 256) {
					short[] page = new short[256];
					for (int i = 0; i < 256; i++) {
						page[i] = (short)getWord(27 + i);
					}
					pages[pageNo] = page;
					if (pageNo > lastPage) {
						lastPage = pageNo;
					}
				} else if (pageWordCount != 0) {
					ok = false;
				}
				
				done = (pageWordCount <= 0);
			}
		}
		
		// transfer the pages into the result
		for (int i = 0; ok && i <= lastPage; i++) {
			short[] page = pages[i];
			if (page == null) { // this page was losst in transfer...
				germ.clear();
				ok = false;
			} else {
				germ.add(page);
			}
		}
		
		// done
		zeNet.shutdown();
		return ok;
	}
	
}