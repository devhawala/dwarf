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

/**
 * Agent for the "beep" device (not implemented, Dwarf is "noiseless").
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class BeepAgent extends Agent {

	/*
	 * BeepFCBType
	 */
	private static final int fcb_w_frequency = 0;
	private static final int FCB_SIZE = 1;
	
	public BeepAgent(int fcbAddress) {
		super(AgentDevice.beepAgent, fcbAddress, FCB_SIZE);
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// nothing to shutdown for this agent
	}
	
	@Override
	public void refreshMesaMemory() {
		// nothing to transfer to mesa memory for this agent
	}

	@Override
	public void call() {
		int frequency = this.getFcbWord(fcb_w_frequency);
		this.logf("frequency = %d\n", frequency);
	}

	@Override
	protected void initializeFcb() {
		this.setFcbWord(fcb_w_frequency, 0);
	}
	
}
