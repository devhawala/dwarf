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

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;

/**
 * Agent for the network interface of a Dwarf machine,
 * 
 * Network access is currently not supported, this agent mainly
 * logs and discards packets that the mesa engine wants to send, 
 * confirming to the mesa engine that the packets were successfully
 * transmitted. No packets can be received, as there is no connection
 * to any network. 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class NetworkAgent extends Agent {
	
	/*
	 * EthernetFCBType
	 */
	private static final int fcb_lp_receiveIOCB = 0;
	private static final int fcb_lp_transmitIOCB = 2;
	private static final int fcb_w_receiveInterruptSelector = 5;
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
	
	public NetworkAgent(int fcbAddress) {
		super(AgentDevice.networkAgent, fcbAddress, FCB_SIZE);
		this.enableLogging(Config.AGENTS_LOG_NETWORK);
		// TODO: connect to network
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// TODO: disconnect from network
	}
	
	@Override
	public void refreshMesaMemory() {
		// TODO: copy packets received so far to mesa memory and raise interrupt (redesign?) 
	}
	
	private int packetsSent = 0;
	
	private int packetsReceived = 0;
	
	public int getPacketsSentCount() { return this.packetsSent; }
	
	public int getPacketsReceivedCount() { return this.packetsReceived; }

	@Override
	public void call() {
		boolean stop = (this.getFcbWord(fcb_w_stopAgent) != PrincOpsDefs.FALSE);
		if (stop) {
			// stop transmissions
			this.setFcbWord(fcb_w_receiveStopped, PrincOpsDefs.TRUE);
			this.setFcbWord(fcb_w_transmitStopped, PrincOpsDefs.TRUE);
			return; // nothing to else do if stopping transmissions
		} else {
			// start transmissions
			this.setFcbWord(fcb_w_receiveStopped, PrincOpsDefs.FALSE);
			this.setFcbWord(fcb_w_transmitStopped, PrincOpsDefs.FALSE);
		}
		
		short transmitInterruptSelector = this.getFcbWord(fcb_w_transmitInterruptSelector);
		boolean doTransmitInterrupt = false;
		
		boolean hearSelf = (this.getFcbWord(fcb_w_hearSelf) != PrincOpsDefs.FALSE);
		int recvIocb = this.getFcbDblWord(fcb_lp_receiveIOCB);
		int sendIocb = this.getFcbDblWord(fcb_lp_transmitIOCB);
		
		long nanoTs = System.nanoTime();
		logf("\n\n--\n-- at %9d.%06d\n--\n", nanoTs / 1000000, nanoTs % 1000000);
		
		logf("call() - recvIocb = 0x%08X , sendIocb = 0x%08X , stopAgent = %s , hearSelf = %s\n",
				recvIocb, sendIocb, (stop) ? "true" : "false", (hearSelf) ? "true" : "false");
		if (stop) { return; }
		
		// TODO *really* process transmission IOCBs
		
		int recvCnt = 0;
		while(recvIocb != 0) {
			int bufferAddress = Mem.readDblWord(recvIocb + iocb_lp_bufferAddress);
			int bufferLength = Mem.readWord(recvIocb + iocb_w_bufferLength);
			int oldDequeuedPacketTypeBits = Mem.readWord(recvIocb + iocb_w_dequeuedPacketTypeStatus) & 0x0000FF00;
			logf("         recvIocb[%d] :: recvIocb = 0x%08X , bufferAddress = 0x%08X , bufferLength = %d\n",
					recvCnt, recvIocb, bufferAddress, bufferLength);
			
			Mem.writeWord(recvIocb + iocb_w_dequeuedPacketTypeStatus, (short)(oldDequeuedPacketTypeBits | S_inProgress));
			
			recvCnt++;
			recvIocb = Mem.readDblWord(recvIocb + iocb_lp_nextIocb);
		}
		
		int sendCnt = 0;
		while(sendIocb != 0) {
			int bufferAddress = Mem.readDblWord(sendIocb + iocb_lp_bufferAddress);
			int bufferLength = Mem.readWord(sendIocb + iocb_w_bufferLength);
			int actualLength = Mem.readWord(sendIocb + iocb_w_actualLength);
			int oldDequeuedPacketTypeBits = Mem.readWord(sendIocb + iocb_w_dequeuedPacketTypeStatus) & 0x0000FF00;
			
			logf("         sendIocb[%d] :: sendIocb = 0x%08X , bufferAddress = 0x%08X , bufferLength = %d , actualLength = %d\n",
					sendCnt, sendIocb, bufferAddress, bufferLength, actualLength);
			
			doTransmitInterrupt = true; // we have something to handle for transmission, so inform about the outcome
			
			int status = S_inProgress;
			if (bufferAddress == 0 || bufferLength < 1) {
				status = S_badCRC;
			} else if (bufferLength > MaxPacketSize) {
				status = S_packetTooLong;
//			} else if (transmission failed) {
//				status = S_badCRC; // S_tooManyCollisions ??
			} else {
				status = S_completedOK;
				packetsSent++;
				Mem.writeWord(sendIocb + iocb_w_actualLength, (short)bufferLength);
				Mem.writeWord(sendIocb + iocb_w_retries, (short)0); // the packet was sent on the 1st try 
			}
			Mem.writeWord(sendIocb + iocb_w_dequeuedPacketTypeStatus, (short)(oldDequeuedPacketTypeBits | status));
			
			if (status == S_completedOK) {
//				logf("          => packet content:");
//				for (int i = 0; i < (bufferLength + 1)/2; i++) {
//					if ((i % 16) == 0) {
//						slogf("\n              0x%03X :", i);
//					}
//					slogf(" %04X", Mem.readWord(bufferAddress + i));
//				}
//				slogf("\n");
				dumpPacket(bufferAddress, bufferLength);
			} else {
				logf("          => packet not transmittable\n");
			}
			
			// TODO: if hearSelf => enqueue this packet into receive queue
			
			sendCnt++;
			sendIocb = Mem.readDblWord(sendIocb + iocb_lp_nextIocb);
		}
		
		if (doTransmitInterrupt) {
			Processes.requestMesaInterrupt(transmitInterruptSelector);
		}
	}
	
	private void dumpPacket(int buffer, int length) {
		logf("\n          => raw packet content:");
		for (int i = 0; i < (length + 1)/2; i++) {
			if ((i % 16) == 0) {
				slogf("\n              0x%03X :", i);
			}
			slogf(" %04X", Mem.readWord(buffer + i));
		}
		slogf("\n");
		
		if (length < 7) { return; }
		
		logf("\n          => ethernet packet header\n");
		dumpNetAddress("dst-addr", Mem.readWord(buffer + 0), Mem.readWord(buffer + 1), Mem.readWord(buffer + 2));
		dumpNetAddress("src-addr", Mem.readWord(buffer + 3), Mem.readWord(buffer + 4), Mem.readWord(buffer + 5));
		short etherType = Mem.readWord(buffer + 6);
		slogf("              ethType  : 0x%04X (%s)\n", etherType, (etherType == 0x0600) ? "xns" : "other" );
		
		if (etherType != 0x0600) { return; }
		
		logf("\n          => xns packet header\n");
		int pByteLen = Mem.readWord(buffer + 8) & 0xFFFF;
		int pWordLen = (pByteLen + 1) /2;
		slogf("              ckSum   : 0x%04X\n", Mem.readWord(buffer + 7));
		slogf("              length  : %d bytes => %d words\n", pByteLen, pWordLen);
		short ctlType = Mem.readWord(buffer + 9);
		slogf("              transCtl: %d\n", ctlType >>> 8);
		int ptype = ctlType & 0xFF;
		String typeName = "?";
		switch(ptype) {
		case 1: typeName = "Rip"; break;
		case 2: typeName = "Echo"; break;
		case 3: typeName = "Error"; break;
		case 4: typeName = "PEX"; break;
		case 5: typeName = "SPP"; break;
		case 9: typeName = "BootServerPacket"; break;
		case 12: typeName = "PUP"; break;
		default: typeName = "unknown";
		}
		slogf("              pktType : %d = %s\n", ptype, typeName);
		dumpXnsEndpoint("destination",
				Mem.readWord(buffer + 10),
				Mem.readWord(buffer + 11),
				Mem.readWord(buffer + 12),
				Mem.readWord(buffer + 13),
				Mem.readWord(buffer + 14),
				Mem.readWord(buffer + 15));
		dumpXnsEndpoint("source",
				Mem.readWord(buffer + 16),
				Mem.readWord(buffer + 17),
				Mem.readWord(buffer + 18),
				Mem.readWord(buffer + 19),
				Mem.readWord(buffer + 20),
				Mem.readWord(buffer + 21));
		
		if (ptype == 4) { // PEX
			logf("\n          => PEX header\n");
			slogf("              identif.  : %04X - %04X\n", Mem.readWord(buffer + 22), Mem.readWord(buffer + 23));
			int ctype = Mem.readWord(buffer + 24);
			String clientType;
			switch(ctype) {
			case 0: clientType = "unspecified"; break;
			case 1: clientType = "time"; break;
			case 2: clientType = "clearinghouse"; break;
			case 8: clientType = "teledebug"; break;
			default: clientType = "??";
			}
			slogf("              clientType: %d = %s\n", ctype, clientType);
			dumpXnsBody(typeName, buffer, pByteLen, 3);
		} else {
			dumpXnsBody(typeName, buffer, pByteLen, 0);
		}
		
	}
	
	private void dumpNetAddress(String prefix, short w0, short w1, short w2) {
		slogf("              %s : %02X-%02X-%02X-%02X-%02X-%02X\n",
			prefix, (w0 >>> 8) & 0xFF, w0 & 0xFF, (w1 >>> 8) & 0xFF, w1 & 0xFF, (w2 >>> 8) & 0xFF, w2 & 0xFF);
	}
	
	private void dumpXnsEndpoint(String prefix, short w0, short w1, short w2, short w3, short w4, short w5) {
		logf("\n          => xns %s\n", prefix);
		slogf("              network : %04X-%04X\n", w0, w1);
		dumpNetAddress("host   ", w2, w3, w4);
		String socket = null;
		switch(w5) {
		case 1: socket = "routing"; break;
		case 2: socket = "echo"; break;
		case 3: socket = "error"; break;
		case 4: socket = "envoy"; break;
		case 5: socket = "courier"; break;
		case 7: socket = "clearinghouse_old"; break;
		case 8: socket = "time"; break;
		case 10: socket = "boot"; break;
		case 19: socket = "diag"; break;
		case 20: socket = "clearinghouse -- Broadcast for servers / Clearinghouse"; break;
		case 21: socket = "auth -- Broadcast for servers / Authentication"; break;
		case 22: socket = "mail"; break;
		case 23: socket = "net_exec"; break;
		case 24: socket = "ws_info"; break;
		case 28: socket = "binding"; break;
		case 35: socket = "germ"; break;
		case 48: socket = "teledebug"; break;
		}
		slogf("              socket  : %04X%s%s\n", w5, (socket == null) ? "" : " - ", (socket == null) ? "" : socket);
	}
	
	private void dumpXnsBody(String prefix, int buffer, int byteLength, int xnsSkip) {
		buffer += 22 + xnsSkip;
		byteLength -= (15 + xnsSkip) * 2;
		logf("\n          => xns %s payload ( bytes: %d => words: %d )", prefix, byteLength, (byteLength + 1) / 2);
		short w = 0;
		int b = 0;
		for (int i = 0; i < byteLength; i++) {
			if ((i % 2) == 0) {
				w = Mem.readWord(buffer + (i / 2));
				b = (w >> 8) & 0xFF;
			} else {
				b = w & 0xFF;
			}
			if ((i % 16) == 0) {
				slogf("\n              0x%03X :", i);
			}
			slogf(" %02X", b);
		}
		slogf("\n");
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

}
