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

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;

/**
 * Abstract base class for all agents defining the common public interface
 * and providing the common functionality.
 * <p>
 * A derived class usually defines a set of constants for the structure of the
 * FCB ({@code fcb_*}), commands ({@code Command_*}), status codes ({@code Status_*})
 * etc., as necessary for communicating with Pilot.
 * </p>
 * <p>
 * <b>Important fine point</b>: When an agent asynchronously receives data from the device
 * it represents (e.g. from the UI or from the network), this data must be buffered
 * and may not be written directly to the mesa engines memory space, as the necessary 
 * synchronizations would excessively slow down the mesa engine.
 * <br>
 * An agent can freely access mesa memory when executing the {@code call()} method (thus
 * servicing a CALLAGENT instruction) and when executing the {@code refreshMesaMemory()} method,
 * which is called at more or less regular intervals by the mesa engine for exactly the purpose
 * of synchronizing the mesa memory with the external data changes accumulated so far.  
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public abstract class Agent {
	
	// the agent type (needed for logging only)
	protected final AgentDevice agentType;
	
	// the own FCB address
	protected final int fcbAddress; // virtual memory long pointer
	
	// the size of the FCB
	protected final int fcbSize; // in words
	
	// should this agent log the own actions?
	protected boolean logging = false;
	
	/**
	 * Enable or disable logging for this agent.
	 * @param enabled the new logging flag.
	 */
	public void enableLogging(boolean enabled) {
		this.logging = enabled;
	}
	
	/**
	 * Base constructor with minimal required parameters.
	 * 
	 * @param agentType the own type for the agent (for logging).
	 * @param fcbAddress the base address of the own FCB.
	 * @param fcbSize the size of the own FCB.
	 */
	protected Agent(AgentDevice agentType, int fcbAddress, int fcbSize) {
		this.agentType = agentType;
		this.fcbAddress = fcbAddress;
		this.fcbSize = fcbSize;
		
		this.logf("ctor - fcbAddress = 0x%08X , fcbSize = %d , initializing FCB\n", fcbAddress, fcbSize);
		this.initializeFcb();
	}
	
	/**
	 * @return the type of this agent.
	 */
	public AgentDevice getAgentType() {
		return this.agentType;
	}
	
	/**
	 * @return the address of this agents FCB 
	 */
	public int getFcbAddress() {
		return this.fcbAddress;
	}
	
	/**
	 * @return the FCB size for this agent.
	 */
	public int getFcbSize() {
		return this.fcbSize;
	}
	
	/**
	 * Logging for this agent if enabled, issuing a line prefix identifying the agent type.
	 * 
	 * @param template {@code printf} template for the line. 
	 * @param args arguments for the template.
	 */
	protected void logf(String template, Object... args) {
		if (!this.logging) { return; }
		System.out.printf("Agent " + agentType + ": " + template, args);
	}
	
	/**
	 * Logging for this agent if enabled, but without a line prefix.
	 * 
	 * @param template {@code printf} template for the line. 
	 * @param args arguments for the template.
	 */
	protected void slogf(String template, Object... args) {
		if (!this.logging) { return; }
		System.out.printf(template, args);
	}
	
	/*
	 * general public interface of an Agent
	 */
	
	/**
	 * Fill the agents FCB with the initial data for the agent configuration.
	 */
	protected abstract void initializeFcb();
	
	/**
	 * Execute the agent operation(s) as instructed in the FCB of the agent.
	 */
	public abstract void call();
	
	/**
	 * Shutdown the agent, possibly saving back all buffered data to the
	 * external media hosting the agents content.
	 * 
	 * @param errMsgTarget for collecting warnings and error messages
	 *   during shutdown.
	 */
	public abstract void shutdown(StringBuilder errMsgTarget);
	
	/**
	 * Copy all buffered new external data into mesa memory space.  
	 */
	public abstract void refreshMesaMemory();
	
	/*
	 * common internal functionality provided to agents
	 */
	
	/**
	 * Read a word from the agents FCB.
	 * 
	 * @param offset position of the word in the FCB.
	 * @return the FCB word at {@code offset}.
	 */
	protected short getFcbWord(int offset) {
		if (offset < 0 || offset >= this.fcbSize) {
			Cpu.ERROR("Agent.getFcbWord :: offset out of range: " + offset);
		}
		return Mem.readWord(this.fcbAddress + offset);
	}
	
	/**
	 * Read a double-word from the agents FCB.
	 * 
	 * @param offset position of the double-word in the FCB.
	 * @return the FCB double-word at {@code offset}.
	 */
	protected int getFcbDblWord(int offset) {
		if (offset < 0 || (offset + 1) >= this.fcbSize) {
			Cpu.ERROR("Agent.getFcbWord :: offset out of range: " + offset);
		}
		return Mem.readDblWord(this.fcbAddress + offset);
	}
	
	/**
	 * Write a word (given as {@code short}) into the agents FCB.
	 * 
	 * @param offset position of the word in the FCB.
	 * @param word the data to be written.
	 */
	protected void setFcbWord(int offset, short word) {
		if (offset < 0 || offset >= this.fcbSize) {
			Cpu.ERROR("Agent.getFcbWord :: offset out of range: " + offset);
		}
		Mem.writeWord(this.fcbAddress + offset, word);
	}
	
	/**
	 * Write a word (given as {@code int}) into the agents FCB.
	 * 
	 * @param offset position of the word in the FCB.
	 * @param word the data to be written.
	 */
	protected void setFcbWord(int offset, int word) {
		if (offset < 0 || offset >= this.fcbSize) {
			Cpu.ERROR("Agent.getFcbWord :: offset out of range: " + offset);
		}
		Mem.writeWord(this.fcbAddress + offset, (short)(word & 0xFFFF));
	}
	
	/**
	 * Write a double-word into the agents FCB.
	 * 
	 * @param offset position of the double-word in the FCB.
	 * @param dblWord the data to be written.
	 */
	protected void setFcbDblWord(int offset, int dblWord) {
		if (offset < 0 || (offset + 1) >= this.fcbSize) {
			Cpu.ERROR("Agent.getFcbWord :: offset out of range: " + offset);
		}
		Mem.writeDblWord(this.fcbAddress + offset, dblWord);
	}
	
}
