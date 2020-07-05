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

/**
 * Common interface and functionality of the Daybreak/6085 device handlers.
 * 
 *@author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public abstract class DeviceHandler {
	
	// generation of bit masks for identifying a single device
	// for notifications by the mesa engine
	private static int nextNotifyMaskLow = 1;
	private static int nextNotifyMaskHigh = 1;
	protected static short mkMask() {
		int res = (nextNotifyMaskHigh << 8) | nextNotifyMaskLow;
		nextNotifyMaskLow <<= 1;
		if (nextNotifyMaskLow > 0x0080) {
			nextNotifyMaskLow = 1;
			nextNotifyMaskHigh++;
		}
		return (short)res;
	}
	
	// handler name for logging
	private final String handlerName;
	
	// should this agent log the own actions?
	protected boolean logging = false;
	
	// constructor
	protected DeviceHandler(String handlerName, boolean loggingEnabled) {
		this.handlerName = handlerName;
		this.logging = loggingEnabled;
	}
	
	/**
	 * Enable or disable logging for this device-handler.
	 * @param enabled the new logging flag.
	 */
	public void enableLogging(boolean enabled) {
		this.logging = enabled;
	}
	
	/**
	 * Logging for this device-handler if enabled, issuing a line prefix identifying the agent type.
	 * 
	 * @param template {@code printf} template for the line. 
	 * @param args arguments for the template.
	 */
	protected void logf(String template, Object... args) {
		if (!this.logging) { return; }
		System.out.printf("DevHandler " + this.handlerName + ": " + template, args);
	}
	
	/**
	 * Logging for this device-handler if enabled, but without a line prefix.
	 * 
	 * @param template {@code printf} template for the line. 
	 * @param args arguments for the template.
	 */
	protected void slogf(String template, Object... args) {
		if (!this.logging) { return; }
		System.out.printf(template, args);
	}
	
	/**
	 * @return the real-memory address of the FCB
	 */
	public abstract int getFcbRealAddress();
	
	/**
	 * @return the segment of the FCB in the IOP memory address space
	 */
	public abstract short getFcbSegment();

	/**
	 * Check if the {@code notifyMask} identifies this device handler and if so process
	 * the request(s) currently present in the FCB of the device handler
	 * <p>
	 * When a NotifyIOP instruction is executed, this method will be called for all device
	 * handlers up to the first handler returning {@code true}.  
	 * </p>
	 * 
	 * @return {@code true} if this device handler was identified by the {@code notifyMask}
	 * 		(independently of the outcome (successful or not) if processing happened).
	 */
	public abstract boolean processNotify(short notifyMask);
	
	/**
	 * Operation codes for LOCKMEM instruction
	 */
	public enum MemOperation { add(0), and(1), or(2), xchg(3), overwriteIfNil(4);
		public final int code;
		private MemOperation(int code) { this.code = code; }
	}
	
	/**
	 * Handle synchronized access to memory among the mesa machine and the IOP.
	 * 
	 * @param lockMask
	 * @param realAddress
	 * @param memOp
	 * @param oldValue
	 * @param newValue
	 */
	public abstract void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue);
	
	/**
	 * Optionally cleanup at the end of the LOCKMEM processing.
	 * 
	 * @param lockMask
	 * @param realAddress
	 */
	public void cleanupAfterLockmem(short lockMask, int realAddress) {
		// default: do nothing
	}
	
	public abstract void handleLockqueue(int vAddr, int rAddr);
	
	/**
	 * Copy all buffered new external data into mesa memory space.  
	 */
	public abstract void refreshMesaMemory();
	
	/**
	 * Stop usage of the device and save buffers or the devices state if necessary.
	 * 
	 * @param errMsgTarget target for messages during shutdown, indicating that there were errors
	 */
	public abstract void shutdown(StringBuilder errMsgTarget);
	
}