/*
Copyright (c) 2018, Dr. Hans-Walter Latz
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

/**
 * Implementation of an intenal time service in case no network access is
 * configured.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2018)
 */
public class NetworkInternalTimeService implements iNetDeviceInterface {
	
	private PacketActor notifier = null;
	private short[] timeResponse = null;
	
	private final short direction; // 0 = west, 1 = east
	private final short offsetHours;
	private final short offsetMinutes;
	
	/**
	 * Initialize the internal time service with the time zone information
	 * (without DST, meaning if DST is active, the {@code gmtOffsetMinutes}
	 * value must be adjusted accordingly).
	 * 
	 * @param gmtOffsetMinutes difference between local time and GMT in
	 * 		minutes, with positive values being to the east and negative
	 * 		to the west (e.g. Germany is +60 without DST and +120 with DST
	 * 		whereas Alaska is -560 without DST resp -480 with DST).  
	 */
	public NetworkInternalTimeService(int gmtOffsetMinutes) {
		if (gmtOffsetMinutes >= 0) {
			this.direction = 1;
		} else {
			this.direction = 0;
			gmtOffsetMinutes = -gmtOffsetMinutes;
		}
		gmtOffsetMinutes = gmtOffsetMinutes % 720;
		this.offsetHours = (short)(gmtOffsetMinutes / 60);
		this.offsetMinutes = (short)(gmtOffsetMinutes % 60);
	}

	@Override
	public void shutdown() {
		// nothing to shutdown...
	}

	@Override
	public synchronized void setNewPacketNotifier(PacketActor notifier) {
		this.notifier = notifier;
	}

	@Override
	public int enqueuePacket(byte[] srcBuffer, int byteCount, boolean feedback) {		
		if (   this.readWord(srcBuffer, 0) != -1
			|| this.readWord(srcBuffer, 1) != -1
			|| this.readWord(srcBuffer, 2) != -1
			|| this.readWord(srcBuffer, 6) != 0x0600) { // not broadcast or not xns
			return byteCount;
		}
		
		if (srcBuffer.length < 54
			|| this.readWord(srcBuffer, 15) != 0x0008
			|| this.readWord(srcBuffer, 9) != 0x0004) { // wrong length, target port not time or not PEX
			return byteCount;
		}
		
		if (   this.readWord(srcBuffer, 24) != 0x0001
			|| this.readWord(srcBuffer, 25) != 0x0002
			|| this.readWord(srcBuffer, 26) != 0x0001) { // not time packet type, wrong version, not request
			return byteCount;
		}
		
		// create time request response
		
		// the raw packet
		short[] b = new short[37];
		
		// address components
		short myNet0 = 0x0004;
		short myNet1 = 0x0001;
		short myMac0 = 0x1000;
		short myMac1 = 0x1A33;
		short myMac2 = 0x3333;
		short mySocket = 8;
		short mac0 = this.readWord(srcBuffer, 3);
		short mac1 = this.readWord(srcBuffer, 4);
		short mac2 = this.readWord(srcBuffer, 5);
		
		// time data
		long unixTimeMillis = System.currentTimeMillis();
		int  milliSecs = (int)(unixTimeMillis % 1000);
		long unixTimeSecs = unixTimeMillis / 1000;
		int mesaSecs = (int)((unixTimeSecs + (731 * 86400) + 2114294400) & 0x00000000FFFFFFFFL);
		short mesaSecs0 = (short)(mesaSecs >>> 16);
		short mesaSecs1 = (short)(mesaSecs & 0xFFFF);
		
		// build the packet component-wise
		
		// eth: dst
		b[0] = mac0;
		b[1] = mac1;
		b[2] = mac2;
		
		// eth: src
		b[3] = myMac0;
		b[4] = myMac1;
		b[5] = myMac2;
		
		// eth: type
		b[6] = 0x0600;
		
		// xns: ckSum
		b[7] = (short)0xFFFF; // no checksum
		
		// xns: length
		b[8] = 60; // payload length
		
		// xns: transport control & packet type
		b[9] = 4; // hop count = 0 & packet type = PEX
		
		// xns: destination endpoint: copy the source destination of the ingone packet
		b[10] = this.readWord(srcBuffer, 16);
		b[11] = this.readWord(srcBuffer, 17);
		b[12] = this.readWord(srcBuffer, 18);
		b[13] = this.readWord(srcBuffer, 19);
		b[14] = this.readWord(srcBuffer, 20);
		b[15] = this.readWord(srcBuffer, 21);
		
		// xns: source endpoint: put "our" address with the "local" net and "our" socket
		b[16] = myNet0;
		b[17] = myNet1;
		b[18] = myMac0;
		b[19] = myMac1;
		b[20] = myMac2;
		b[21] = mySocket;
		
		// pex: identification from request
		b[22] = this.readWord(srcBuffer, 22);
		b[23] = this.readWord(srcBuffer, 23);
		
		// pex: client type
		b[24] = 1; // clientType "time"
		
		// payload: time response
		b[25] = 2;                  // version(0): WORD -- TimeVersion = 2
		b[26] = 2;                  // tsBody(1): SELECT type(1): PacketType FROM -- timeResponse = 2
		b[27] = mesaSecs0;          // time(2): WireLong -- computed time
		b[28] = mesaSecs1;
		b[29] = this.direction;     // zoneS(4): System.WestEast -- east
		b[30] = this.offsetHours;   // zoneH(5): [0..177B] -- +1 hour
		b[31] = this.offsetMinutes; // zoneM(6): [0..377B] -- +0 minutes
		b[32] = 0;                  // beginDST(7): WORD -- no dst (temp)
		b[33] = 0;                  // endDST(8): WORD -- no dst (temp)
		b[34] = 1;                  // errorAccurate(9): BOOLEAN -- true
		b[35] = 0;                  // absoluteError(10): WireLong]
		b[36] = (short)((milliSecs > 500) ? 1000 - milliSecs : milliSecs); // no direction ?? (plus or minus)?
		
		// enqueue for "receiving" the the service response
		this.setPacket(b);
		
		// done
		return byteCount;
	}

	@Override
	public int dequeuePacket(byte[] trgBuffer, int maxLength) {
		short[] packet = this.getPacket();
		if (packet == null || trgBuffer == null || maxLength < 2) {
			return 0;
		}
		this.setPacket(null); // the only one was received...
		
		int trfWords = Math.min(maxLength / 2, packet.length);
		int bpos = 0;
		for (int i = 0; i < trfWords; i++) {
			trgBuffer[bpos++] = (byte)((packet[i] >> 8) & 0xFF);
			trgBuffer[bpos++] = (byte)(packet[i] & 0xFF);
		}
		
		return trfWords * 2;
	}
	
	/*
	 * internals
	 */
	
	private short readWord(byte[] b, int wpos) {
		int bpos = wpos * 2;
		if (bpos < 0 || (bpos + 1) >= b.length) {
			return 0;
		}
		int hi = b[bpos++] & 0xFF;
		int lo = b[bpos] & 0xFF;
		short res = (short)(((hi << 8) | lo) & 0xFFFF);
		return res;
	}
	
	private synchronized short[] getPacket() {
		return this.timeResponse;
	}
	
	private synchronized void setPacket(short[] p) {
		this.timeResponse = p;

		if (this.notifier != null) {
			try {
				this.notifier.handleSinglePacket();
			} catch (InterruptedException e) {
				// ignored
			}
		}
	}

}
