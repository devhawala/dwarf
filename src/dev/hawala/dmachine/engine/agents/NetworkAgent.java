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

import java.util.LinkedList;
import java.util.Queue;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;

/**
 * Agent for the network interface of a Dwarf machine,
 * 
 * Network access is supported to the Dodo NetHub or
 * as fallback to an internal time service, depending on
 * the configuration of the network agent.  
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017,2018)
 */
public class NetworkAgent extends Agent {
	
	/*
	 * EthernetFCBType
	 */
	private static final int fcb_lp_receiveIOCB = 0;
	private static final int fcb_lp_transmitIOCB = 2;
	private static final int fcb_w_receiveInterruptSelector = 4;
	private static final int fcb_w_transmitInterruptSelector = 5;
	private static final int fcb_w_stopAgent = 6;
	private static final int fcb_w_receiveStopped = 7;
	private static final int fcb_w_transmitStopped = 8;
	private static final int fcb_w_hearSelf = 9;
	private static final int fcb_w_processorID0 = 10;
	private static final int fcb_w_processorID1 = 11;
	private static final int fcb_w_processorID2 = 12;
	private static final int fcb_w_packetsMissed = 13;
	private static final int fcb_w_agentBlockSize = 14;
	private static final int FCB_SIZE = 15;
	
	// EthernetIOCBType (8 words)
	private static final int iocb_lp_bufferAddress = 0;
	private static final int iocb_w_bufferLength = 2;
	private static final int iocb_w_actualLength = 3;
	private static final int iocb_w_dequeuedPacketTypeStatus = 4; // dequeued(1), packetType(1..7), status(8..15)
	private static final int iocb_w_retries = 5;
	private static final int iocb_lp_nextIocb = 6;
	
	// bits for iocb_w_dequeuedPacketTypeStatus
	private static final int Q_queued   = 0x00000000;
	private static final int Q_dequeued = 0x00008000;
	private static final int T_receive  = 0x00000000;
	private static final int T_transmit = 0x00000100;
	private static final int S_inProgress              =   1;
	private static final int S_completedOK             =   2;
	private static final int S_tooManyCollisions       =   4;
	private static final int S_badCRC                  =   8;
	private static final int S_alignmentError          =  16;
	private static final int S_packetTooLong           =  32;
	private static final int S_badCRDAndAlignmentError = 128;
	
	// other constants for network
	private static final int MaxPacketSize = 1536; // max. ethernet packet size, XNS uses max. 576 bytes = 546 payload + 30 header
	
	// configuration parameters
	private static String hubHostname = "";
	private static int hubPort = 0;
	private static int localTimeOffsetMinutes = 0;
	
	// the network device the agent is connected to
	private iNetDeviceInterface netIf = null;
	
	// packet receiving status
	private boolean receiveStopped = true; // is receiving packets stopped and are all incoming packets to be dropped?
	
	// list of IOCB addresses ready to receive packets
	private final Queue<Integer> receiveIocbs = new LinkedList<>();
	
	// temp buffer for word <-> byte conversion of packets
	private final byte[] packetBuffer = new byte[2048];
	
	/**
	 * Configure the (next created instance of the) agent with connection data
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
	
//	private static class InterruptThrottler implements Runnable {
//		
//		public static final int NO_INTERRUPTS = -123456;
//		
//		private final int waitInterval; // milliseconds
//		
//		private final List<Runnable> pendingInterruptRaisers = new ArrayList<>();
//		private Runnable nextRaiser = null;
//		private long nextTs = never();
//		
//		public InterruptThrottler(int interval) {
//			this.waitInterval = interval;
//		}
//		
//		private static long never() {
//			return System.currentTimeMillis() + (28 * 86400000L); // now + 4 weeks 
//		}
//		
//		public Runnable getNextRaiser() {
//			if (this.waitInterval <= 0) {
//				return null;
//			}
//			synchronized(this) {
//				if (System.currentTimeMillis() < this.nextTs) {
//					return null;
//				}
//				Runnable result = this.nextRaiser;
//				this.nextRaiser = null;
//				this.nextTs = never();
//				return result;
//			}
//		}
//		
//		public void addRaiser(Runnable raiser) {
//			if (this.waitInterval == NO_INTERRUPTS) {
//				return;
//			}
//			if (this.waitInterval <= 0) {
//				raiser.run();
//				return;
//			}
//			synchronized(this) {
//				this.pendingInterruptRaisers.add(raiser);
//			}
//		}
//		
//		public void reset() {
//			synchronized(this) {
//				this.pendingInterruptRaisers.clear();
//				this.nextRaiser = null;
//				this.nextTs = never();
//			}
//		}
//		
//		@Override
//		public void run() {
//			if (this.waitInterval <= 0) {
//				return;
//			}
//			try {
//				synchronized(this) {
//					while(true) {
//						this.wait(waitInterval);
//						if (this.nextRaiser != null) {
//							Processes.requestDataRefresh();
//						} else if (!this.pendingInterruptRaisers.isEmpty()) {
//							this.nextRaiser = this.pendingInterruptRaisers.remove(0);
//							this.nextTs = System.currentTimeMillis() + waitInterval;
//						}
//					}
//				}
//			} catch (InterruptedException e) {
//				return;
//			}
//		} 
//	}
	
//	private final InterruptThrottler interruptThrottler = new InterruptThrottler(0);
//	private Thread throttlerThread = null;
	
	public NetworkAgent(int fcbAddress) {
		super(AgentDevice.networkAgent, fcbAddress, FCB_SIZE);
		this.enableLogging(Config.IO_LOG_NETWORK);
		
		if (hubHostname != null && !hubHostname.isEmpty()
			&& hubPort > 0 && hubPort < 0xFFFF) {
			this.netIf = new NetworkHubInterface(hubHostname, hubPort);
		} else {
			this.netIf = new NetworkInternalTimeService(localTimeOffsetMinutes);
		}
		this.netIf.setNewPacketNotifier(() -> {
			logf("\n+++ requesting datarefresh for new packet (at: %s , insns = %d)\n", 
					getNanoMs(), Cpu.insns);
			Processes.requestDataRefresh();
		});
		
//		this.throttlerThread = new Thread(this.interruptThrottler);
//		this.throttlerThread.setName("InterruptThrottler");
//		this.throttlerThread.setDaemon(true);
//		this.throttlerThread.start();
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		if (this.netIf != null) {
			this.netIf.shutdown();
			this.netIf = null;
		}
//		if (this.throttlerThread != null) {
//			this.throttlerThread.interrupt();
//			this.throttlerThread = null;
//		}
	}
	
	@Override
	public void refreshMesaMemory() {
		boolean logIntro = true;
		
		// no network => nothing to do...
		if (this.netIf == null) { return; }
		
		// raise next interrupt if available
//		Runnable intrRaiser = this.interruptThrottler.getNextRaiser();
//		if (intrRaiser != null) {
//			logIntro = this.logTimeIntro();
//			logf("refreshMesaMemory()\n");
//			
//			intrRaiser.run();
//		}
		
		// did Pilot stop packet transmission?
		if (this.receiveStopped) {
			// drain incoming packets
			while(this.netIf.dequeuePacket(this.packetBuffer, this.packetBuffer.length) > 0) {
				// ignore packet content
			}
		}
		
		// receive ingone packets into waiting iocbs
		boolean didInterrupt = false;
		while(!this.receiveIocbs.isEmpty()) {
			int packetByteCount = this.netIf.dequeuePacket(this.packetBuffer, this.packetBuffer.length);
			if (packetByteCount < 1) { return; }
			
			if (logIntro) {
				logIntro = this.logTimeIntro();
				logf("begin refreshMesaMemory()\n");
			}
			
			int recvIocb = this.dequeueReceiveIocb();
			
			int bufferAddress = Mem.readDblWord(recvIocb + iocb_lp_bufferAddress);
			int bufferByteLength = Mem.readWord(recvIocb + iocb_w_bufferLength);
			
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
			Mem.writeWord(recvIocb + iocb_w_actualLength, (short)(wlen * 2));
			
			int status = (trfBytes == packetByteCount) ? S_completedOK : S_packetTooLong;
			int packetTypeBits = Mem.readWord(recvIocb + iocb_w_dequeuedPacketTypeStatus) & 0x0000FF00;
			Mem.writeWord(recvIocb + iocb_w_dequeuedPacketTypeStatus, (short)(packetTypeBits | status));
			
			dumpIocb(recvIocb, true);
			slogf("\n");
			
			this.packetsReceived++;
			logf("     transfered %d words (trfBytes %d, bufferLen %d) to Mesa memory at 0x%08X => status = %d\n", wlen, trfBytes, bufferByteLength, bufferAddress, status);
			
//			logf("registering interrupt raiser for recvIocb 0x%08X to throttler\n", recvIocb);
//			String registerTs = getNanoMs();
//			this.interruptThrottler.addRaiser(() -> {
//				int packetTypeBits = Mem.readWord(recvIocb + iocb_w_dequeuedPacketTypeStatus) & 0x0000FF00;
//				Mem.writeWord(recvIocb + iocb_w_dequeuedPacketTypeStatus, (short)(packetTypeBits | status));
//				
//				short recvIntrMask = this.getFcbWord(fcb_w_receiveInterruptSelector);
//				Processes.requestMesaInterrupt(recvIntrMask);
//				logf("+++++  at %s (registered: %s) :: requested MesaInterrupt(0x%04X) for recvIocb 0x%08X\n",
//						getNanoMs(), registerTs, recvIntrMask, recvIocb);
//			});
			
			if (!didInterrupt) {
				short recvIntrMask = this.getFcbWord(fcb_w_receiveInterruptSelector);
				Processes.requestMesaInterrupt(recvIntrMask);
				didInterrupt = true;
				logf("     requested MesaInterrupt(0x%04X)\n", recvIntrMask);
//				Cpu.out.printf("####################  requested MesaInterrupt(0x%04X)\n", recvIntrMask);
			}
		}
		if (!logIntro) {
			logf("done refreshMesaMemory()\n");
		}
	}
	
	private int packetsSent = 0;
	private int packetsReceived = 0;
	
	public int getPacketsSentCount() { return this.packetsSent; }
	
	public int getPacketsReceivedCount() { return this.packetsReceived; }
	
	@Override
	public void call() {
		this.logTimeIntro();
		boolean stop = (this.getFcbWord(fcb_w_stopAgent) != PrincOpsDefs.FALSE);
		if (stop) {
			logf("call() - stop transmissions\n");
			
			// stop transmissions
			this.setFcbWord(fcb_w_receiveStopped, PrincOpsDefs.TRUE);
			this.setFcbWord(fcb_w_transmitStopped, PrincOpsDefs.TRUE);
			this.receiveStopped = true;
			
			// drop pending packets in network interface, iocb queue and interrupt queue
			// drain incoming packets
			if (this.netIf != null) {
				while(this.netIf.dequeuePacket(this.packetBuffer, this.packetBuffer.length) > 0) {
					// ignore packets dropped
				}
			}
			this.receiveIocbs.clear();
//			this.interruptThrottler.reset();
			logf("call() - end\n");
			
			return; // nothing to else do if stopping transmissions
		} else {
			// (re)tart transmissions
			this.setFcbWord(fcb_w_receiveStopped, PrincOpsDefs.FALSE);
			this.setFcbWord(fcb_w_transmitStopped, PrincOpsDefs.FALSE);
			if (this.receiveStopped) {
				this.receiveIocbs.clear();
				logf("call() - (re)starting transmissions\n");
			}
			this.receiveStopped = false;
		}
		
		short receiveInterruptSelector = this.getFcbWord(fcb_w_receiveInterruptSelector);
		short transmitInterruptSelector = this.getFcbWord(fcb_w_transmitInterruptSelector);
		boolean doTransmitInterrupt = false;
		
		boolean hearSelf = (this.getFcbWord(fcb_w_hearSelf) != PrincOpsDefs.FALSE);
		int recvIocb = this.getFcbDblWord(fcb_lp_receiveIOCB);
		int sendIocb = this.getFcbDblWord(fcb_lp_transmitIOCB);
		
		logf("call() - recvIocb = 0x%08X , sendIocb = 0x%08X , stopAgent = %s , hearSelf = %s\n",
				recvIocb, sendIocb, (stop) ? "true" : "false", (hearSelf) ? "true" : "false");
		logf("         recvIntr = 0x%04X , xmitIntr = 0x%04X\n",
				receiveInterruptSelector & 0xFFFF, transmitInterruptSelector & 0xFFFF);
		if (stop) { return; }
		
		int recvCnt = 0;
		while(recvIocb != 0) {
			int bufferAddress = Mem.readDblWord(recvIocb + iocb_lp_bufferAddress);
			int bufferLength = Mem.readWord(recvIocb + iocb_w_bufferLength);
			logf("         recvIocb[%d] :: recvIocb = 0x%08X , bufferAddress = 0x%08X , bufferLength = %d\n",
					recvCnt, recvIocb, bufferAddress, bufferLength);
			
			this.enqueueReceiveIocb(recvIocb);
			
			recvCnt++;
			recvIocb = Mem.readDblWord(recvIocb + iocb_lp_nextIocb);
		}
		
		int sendCnt = 0;
		while(sendIocb != 0) {
			int bufferAddress = Mem.readDblWord(sendIocb + iocb_lp_bufferAddress);
			int bufferLength = Mem.readWord(sendIocb + iocb_w_bufferLength);
			int actualLength = Mem.readWord(sendIocb + iocb_w_actualLength);
			
			logf("         sendIocb[%d] :: sendIocb = 0x%08X , bufferAddress = 0x%08X , bufferLength = %d , actualLength = %d\n",
					sendCnt, sendIocb, bufferAddress, bufferLength, actualLength);
			
			doTransmitInterrupt = true; // we have something to handle for transmission, so inform about the outcome
			
			final int status;
			if (bufferAddress == 0 || bufferLength < NetworkHubInterface.MIN_NET_PACKET_LEN) {
				status = S_badCRC;
			} else if (bufferLength > Math.min(NetworkHubInterface.MAX_NET_PACKET_LEN, MaxPacketSize)) {
				status = S_packetTooLong;
			} else if (this.netIf == null) {
				status = S_badCRC; // S_tooManyCollisions ??
			} else {
				int bpos = 0;
				for (int i = 0; i < (bufferLength + 1)/2; i++) {
					short w = Mem.readWord(bufferAddress + i);
					this.packetBuffer[bpos++] = (byte)(w >>> 8);
					this.packetBuffer[bpos++] = (byte)(w & 0xFF);
				}
				int trfLength = this.netIf.enqueuePacket(this.packetBuffer, bufferLength, hearSelf);
				status = (trfLength == bufferLength) ? S_completedOK : S_packetTooLong;
				packetsSent++;
				Mem.writeWord(sendIocb + iocb_w_actualLength, (short)trfLength);
				Mem.writeWord(sendIocb + iocb_w_retries, (short)0); // the packet was sent on the 1st try
			}
			int oldDequeuedPacketTypeBits = Mem.readWord(sendIocb + iocb_w_dequeuedPacketTypeStatus) & 0x0000FF00;
			Mem.writeWord(sendIocb + iocb_w_dequeuedPacketTypeStatus, (short)(oldDequeuedPacketTypeBits | status));
			
//			logf("registering interrupt raiser for sendIocb 0x%08X to throttler\n", sendIocb);
//			int iocbStatus = status;
//			int intrIocb = sendIocb;
//			String registerTs = getNanoMs();
//			this.interruptThrottler.addRaiser(() -> {
//				int oldDequeuedPacketTypeBits = Mem.readWord(intrIocb + iocb_w_dequeuedPacketTypeStatus) & 0x0000FF00;
//				Mem.writeWord(intrIocb + iocb_w_dequeuedPacketTypeStatus, (short)(oldDequeuedPacketTypeBits | iocbStatus));
//
//				short xmitIntrMask = this.getFcbWord(fcb_w_transmitInterruptSelector);
//				Processes.requestMesaInterrupt(transmitInterruptSelector);
//				logf("+++++ at %s (registered: %s) :: requested MesaInterrupt(0x%04X) for sendIocb 0x%04X\n",
//						getNanoMs(), registerTs, xmitIntrMask, intrIocb);
//			});
			
			dumpIocb(sendIocb);
			
			if (status == S_completedOK) {
				logf("          => packet [%d] transmitted, length: %d\n", packetsSent, bufferLength);
			} else {
				logf("          => packet not transmitted or transmittable\n");
			}
			
			sendCnt++;
			sendIocb = Mem.readDblWord(sendIocb + iocb_lp_nextIocb);
		}
		
		if (doTransmitInterrupt) {
			Processes.requestMesaInterrupt(transmitInterruptSelector);
			logf("     requested MesaInterrupt(0x%04X)\n", transmitInterruptSelector);
		}
		logf("call() - end\n");
	}

	@Override
	protected void initializeFcb() {
		this.setFcbDblWord(fcb_lp_receiveIOCB, 0);
		this.setFcbDblWord(fcb_lp_transmitIOCB, 0);
		this.setFcbWord(fcb_w_receiveInterruptSelector, 0);
		this.setFcbWord(fcb_w_transmitInterruptSelector, 0);
		this.setFcbWord(fcb_w_stopAgent, PrincOpsDefs.FALSE);
		this.setFcbWord(fcb_w_receiveStopped, PrincOpsDefs.TRUE);
		this.setFcbWord(fcb_w_transmitStopped, PrincOpsDefs.TRUE);
		this.setFcbWord(fcb_w_hearSelf, PrincOpsDefs.FALSE);
		this.setFcbWord(fcb_w_processorID0, Cpu.getPIDword(1));
		this.setFcbWord(fcb_w_processorID1, Cpu.getPIDword(2));
		this.setFcbWord(fcb_w_processorID2, Cpu.getPIDword(3));
		this.setFcbWord(fcb_w_packetsMissed, 0);
		this.setFcbWord(fcb_w_agentBlockSize, 0); // no agent specific space needed in IOCBs ... ?
	}
	

	
	private void enqueueReceiveIocb(int iocb) {
		if (iocb == 0) { return; }
		
		if (!this.receiveIocbs.isEmpty()) {
			int bufferAddress = Mem.readDblWord(iocb + iocb_lp_bufferAddress);
			boolean doRemove = false;
			for (int queued : this.receiveIocbs) {
				int queuedAddress = Mem.readDblWord(queued + iocb_lp_bufferAddress);
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
		
		int packetTypeBits = Mem.readWord(iocb + iocb_w_dequeuedPacketTypeStatus) & 0x0000FF00;
		Mem.writeWord(iocb + iocb_w_dequeuedPacketTypeStatus, (short)(packetTypeBits | S_inProgress));
		
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
	
	private static String getNanoMs() {
		long nanoTs = System.nanoTime();
		return String.format("%9d.%06d ms", nanoTs / 1000000, nanoTs % 1000000);
	}
	
	// returns if log must be done
	private boolean logTimeIntro() {
		slogf("\n\n--\n-- at %s (insns: %d)\n--\n", getNanoMs(), Cpu.insns);
		return false;
	}	
	
	private void dumpIocb(int iocb) {
		dumpIocb(iocb, false);
	}
	
	private void dumpIocb(int iocb, boolean dumpBuffer) {
		int bAddr = Mem.readDblWord(iocb);
		int bLen = Mem.readWord(iocb + 2);
		int actLen = Mem.readWord(iocb + 3);
		int qtStatus = Mem.readWord(iocb + 4);
		int retries = Mem.readWord(iocb + 5);
		int nextIocb = Mem.readDblWord(iocb + 6);
		String dequeued = ((qtStatus & Q_dequeued) != 0) ? "dequeue" : "queued";
		String type = ((qtStatus & T_transmit) != 0) ? "transmit" : "receive";
		String status = ((qtStatus & S_inProgress) != 0) ? " inProgress" : "";
		status += ((qtStatus & S_completedOK) != 0) ? " completedOK" : "";
		status += ((qtStatus & S_tooManyCollisions) != 0) ? " tooManyCollisions" : "";
		status += ((qtStatus & S_badCRC) != 0) ? " badCRC" : "";
		status += ((qtStatus & S_alignmentError) != 0) ? " alignmentError" : "";
		status += ((qtStatus & S_packetTooLong) != 0) ? " packetTooLong" : "";
		status += ((qtStatus & 64) != 0) ? " d64" : "";
		status += ((qtStatus & S_badCRDAndAlignmentError) != 0) ? " badCRDAndAlignmentError" : "";
		
		slogf("-> IOCB @ 0x%08X [ bAddr = 0x%08X , bLen = %4d , actLen = %4d , qts = 0x%04X , retries = %4d , nextIocb = 0x%08X ; ( %s %s%s ) ]\n",
			iocb, bAddr, bLen, actLen, qtStatus, retries, nextIocb, dequeued, type, status);
		if (dumpBuffer) {
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

}
