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
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.eLevelVKey;

/**
 * Agent for the keyboard of a Dwarf machine.
 * <p>
 * The purpose of this agent is to transfer the keyboard
 * events (already translated to {@code eLevelVKey}s) into
 * the bits in the FCB of this agent representing each key.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class KeyboardAgent extends Agent {

	/*
	 * KeyboardFCBType (7 words for keystates)
	 */

	private static final int FCB_SIZE = 7;
	
	/*
	 * key states
	 */
	private static final short ALL_KEYS_UP = (short)0xFFFF;

	private short[] uiKeys = new short[FCB_SIZE];
	private boolean uiKeysChanged = false;
	
	public KeyboardAgent(int fcbAddress) {
		super(AgentDevice.keyboardAgent, fcbAddress, FCB_SIZE);
		
		this.enableLogging(Config.IO_LOG_KEYBOARD);
		
		for (int i = 0; i < FCB_SIZE; i++) {
			this.uiKeys[i] = ALL_KEYS_UP;
		}
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// nothing to shutdown for this agent
	}

	@Override
	public void call() {
		logf("call() - irrelevant, why is this called???\n");
	}

	@Override
	protected void initializeFcb() {
		for (int i = 0; i < FCB_SIZE; i++) {
			this.setFcbWord(i, ALL_KEYS_UP);
		}
	}
	
	public synchronized void resetKeys() {
		for (int i = 0; i < FCB_SIZE; i++) {
			this.uiKeys[i] = ALL_KEYS_UP;
		}
		this.uiKeysChanged = true;
		this.logf("resetKeys()\n");
		Processes.requestDataRefresh();
	}

	public synchronized void handleKeyUsage(eLevelVKey key, boolean isPressed) {
		if (isPressed) {
			key.setPressed(this.uiKeys);
		} else {
			key.setReleased(this.uiKeys);
		}
		this.uiKeysChanged = true;
		this.logf("handleKeyUsage( key = %s, isPressed = %s )\n", key.toString(), (isPressed) ? "true" : "false");
		Processes.requestDataRefresh();
	}
	
	public synchronized void refreshMesaMemory() {
		if (this.uiKeysChanged) {
			this.logf("refreshMesaMemory() -> \n");
			for (int i = 0; i < FCB_SIZE; i++) {
				short keySetting = this.uiKeys[i];
//				logf("    | %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s |\n",
//						((keySetting & 0x8000) != 0) ? "x" : " ",
//						((keySetting & 0x4000) != 0) ? "x" : " ",
//						((keySetting & 0x2000) != 0) ? "x" : " ",
//						((keySetting & 0x1000) != 0) ? "x" : " ",
//						((keySetting & 0x0800) != 0) ? "x" : " ",
//						((keySetting & 0x0400) != 0) ? "x" : " ",
//						((keySetting & 0x0200) != 0) ? "x" : " ",
//						((keySetting & 0x0100) != 0) ? "x" : " ",
//						((keySetting & 0x0080) != 0) ? "x" : " ",
//						((keySetting & 0x0040) != 0) ? "x" : " ",
//						((keySetting & 0x0020) != 0) ? "x" : " ",
//						((keySetting & 0x0010) != 0) ? "x" : " ",
//						((keySetting & 0x0008) != 0) ? "x" : " ",
//						((keySetting & 0x0004) != 0) ? "x" : " ",
//						((keySetting & 0x0002) != 0) ? "x" : " ",
//						((keySetting & 0x0001) != 0) ? "x" : " "
//						);
				this.setFcbWord(i, keySetting);
			}
			this.uiKeysChanged = false;
		}
	}
	
}
