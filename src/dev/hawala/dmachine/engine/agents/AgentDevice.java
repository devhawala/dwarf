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
 * Definition of all agent types in the PrincOps Guam (? Pilot 15.3 ?) architecture.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public enum AgentDevice {
	nullAgent(0),
	diskAgent(1),
	floppyAgent(2),
	networkAgent(3),
	parallelAgent(4),
	keyboardAgent(5),
	beepAgent(6),
	mouseAgent(7),
	processorAgent(8),
	streamAgent(9),
	serialAgent(10),
	ttyAgent(11),
	displayAgent(12),
	reserved3Agent(13),
	reserved2Agent(14),
	reserved1Agent(15);
	
	private final int index;
	
	private AgentDevice(int idx) {
		this.index = idx;
	}
	
	/**
	 * @return the index position reserved for this agent
	 *   in the LONG POINTER TO FCB array at the ioArea start.
	 */
	public int getIndex() {
		return this.index;
	}
}