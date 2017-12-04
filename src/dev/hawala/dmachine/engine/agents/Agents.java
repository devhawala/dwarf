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

import java.io.File;
import java.io.IOException;

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.eLevelVKey;
import dev.hawala.dmachine.engine.iMesaMachineDataAccessor;
import dev.hawala.dmachine.engine.iUiDataConsumer;

/**
 * Management class for all agents as central dispatch instance
 * for all agent related operations.
 * <br>
 * The {@code Agents} static class initializes the agents to correctly
 * setup the FCB area at virtual memory ioArea start, accessed by the mesa
 * engine (mapping this specific area to real memory address 0x00000000) to
 * read the current state of agents resp. to enlink there the IOCBs for agent
 * requests before executing the CALLAGENT instruction, which invokes the
 * {@code Agents.callAgent()} method. Conversely, the devices can be closed
 * and buffered data saved by deinializing the agents.
 * <br>
 * The UI can access the necessary callbacks for transferring UI-changes
 * to the mesa engine by calling the {@code getUiCallbacks()} method.
 * Further methods allow to change the current "inserted" diskette in the
 * simulated floppy drive.
 * <br>
 * Furthermore access to statistical data of specific agents is provided.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Agents {
	
	// all agents
	private static final Agent[] agent = new Agent[AgentDevice.values().length];
	
	// specific agents directly accessible for published functionality   
	private static DiskAgent diskAgent;
	private static FloppyAgent floppyAgent;
	private static NetworkAgent networkAgent;
	private static DisplayAgent displayAgent;
	private static MouseAgent mouseAgent;
	private static KeyboardAgent keyboardAgent;
	
	// the callbacks allowing the UI to transmit data modifications to the mesa engine  
	private static class UiCallbacks implements iUiDataConsumer {

		@Override
		public void acceptKeyboardKey(eLevelVKey key, boolean isPressed) {
			if (keyboardAgent == null) { return; }
			keyboardAgent.handleKeyUsage(key, isPressed);
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
			if (mouseAgent == null) { return; }
			mouseAgent.recordMouseMoved(x, y);
		}

		@Override
		public void registerPointerBitmapAcceptor(PointerBitmapAcceptor acpt) {
			if (mouseAgent == null) { return; }
			mouseAgent.setPointerBitmapAcceptor(acpt);
		}
		
		@Override
		public void registerUiDataRefresher(iMesaMachineDataAccessor refresher) {
			Processes.registerUiRefreshCallback(refresher);
		}
	}
	
	/**
	 * Retrieve the agent callbacks for the UI,
	 *  
	 * @return the callbacks instance
	 */
	public static iUiDataConsumer getUiCallbacks() {
		return new UiCallbacks();
	}

	/**
	 * Initialize the agents and setup the FCB area of the mesa engine.
	 */
	public static void initialize() {
		// relevant (virtual) addresses in ioRegion:
		// -> first COUNT[AgentDevice] LONG POINTERs to the FCB of each agent
		// -> then the  FCB areas of the devices
		// first: address of current entry in the LONG POINTER array 
		int currFcbPtr = Mem.ioArea;
		// second: address of current FCB
		int currFcbArea = roundUp(currFcbPtr + (AgentDevice.values().length * 2));
		
		// nullAgent at index 0
		int idx = 0;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new NullAgent(currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// diskAgent at index 1
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		diskAgent = new DiskAgent(currFcbArea);
		agent[idx] = diskAgent;
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// floppyAgent at index 2
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		floppyAgent = new FloppyAgent(currFcbArea);
		agent[idx] = floppyAgent;
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// networkAgent at index 3
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		networkAgent = new NetworkAgent(currFcbArea);
		agent[idx] = networkAgent;
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// parallelAgent at index 4
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new ParallelAgent(currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// keyboardAgent at index 5
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		keyboardAgent = new KeyboardAgent(currFcbArea);
		agent[idx] = keyboardAgent;
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// beepAgent at index 6
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new BeepAgent(currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// mouseAgent at index 7
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		mouseAgent = new MouseAgent(currFcbArea);
		agent[idx] = mouseAgent;
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// processorAgent at index 8
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new ProcessorAgent(currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// streamAgent at index 9
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new StreamAgent(currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// serialAgent at index 10
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new SerialAgent(currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// ttyAgent at index 11
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new TtyAgent(currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// displayAgent at index 12
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		displayAgent = new DisplayAgent(currFcbArea, mouseAgent);
		agent[idx] = displayAgent;
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// reserved3Agent at index 13
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new ReservedAgent(AgentDevice.reserved3Agent, currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// reserved2Agent at index 14
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new ReservedAgent(AgentDevice.reserved2Agent, currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		// reserved1Agent at index 15
		idx++;
		Mem.writeDblWord(currFcbPtr, currFcbArea);
		agent[idx] = new ReservedAgent(AgentDevice.reserved1Agent, currFcbArea);
		currFcbPtr += 2;
		currFcbArea = roundUp(currFcbArea + agent[idx].getFcbSize());
		
		
		// sanity check: verify that each agent is at the expected index position
		for (int i = 0; i < AgentDevice.values().length; i++) {
			if (agent[i] != null) {
				if (agent[i].getAgentType().getIndex() != i) {
					Cpu.ERROR("Agents.initialize :: wrong agent at index " + i + " -> " + agent[i].getAgentType());;
				}
			}
		}
		
		// reset the FCB pointer for agents not present in Dwarf
		idx = AgentDevice.nullAgent.getIndex();
		agent[idx] = null;
		Mem.writeDblWord(Mem.ioArea + (2 * idx), 0);
		idx = AgentDevice.parallelAgent.getIndex();
		agent[idx] = null;
		Mem.writeDblWord(Mem.ioArea + (2 * idx), 0);
		idx = AgentDevice.serialAgent.getIndex();
		agent[idx] = null;
		Mem.writeDblWord(Mem.ioArea + (2 * idx), 0);
		idx = AgentDevice.ttyAgent.getIndex();
		agent[idx] = null;
		Mem.writeDblWord(Mem.ioArea + (2 * idx), 0);
		idx = AgentDevice.reserved3Agent.getIndex();
		agent[idx] = null;
		Mem.writeDblWord(Mem.ioArea + (2 * idx), 0);
		idx = AgentDevice.reserved2Agent.getIndex();
		agent[idx] = null;
		Mem.writeDblWord(Mem.ioArea + (2 * idx), 0);
		idx = AgentDevice.reserved1Agent.getIndex();
		agent[idx] = null;
		Mem.writeDblWord(Mem.ioArea + (2 * idx), 0);
	}
	
	// ensure that an FCB starts at a double word address
	private static int roundUp(int ptr) {
		if ((ptr & 1) != 0) { return ptr + 1; }
		return ptr;
	}
	
	/**
	 * Dispatch a CALLAGENT instruction to the correct agent. 
	 * 
	 * @param agentIndex the agent number popped from the
	 * 	  evaluation stack
	 */
	public static void callAgent(int agentIndex) {
		if (agentIndex < 0 || agentIndex >= agent.length) {
			Cpu.ERROR("CALLAGENT :: invalid agentIndex " + agentIndex);
			return; // we won't get here
		}
		if (agent[agentIndex] == null) {
			Cpu.ERROR("CALLAGENT :: agent not available at agentIndex " + agentIndex);
			return; // we won't get here
		}
		agent[agentIndex].call();
	}
	
	/**
	 * Transfer all cached data changes into mesa memory space.  
	 */
	public static void processPendingMesaMemoryUpdates() {
		for (int i = 0; i < AgentDevice.values().length; i++) {
			if (agent[i] != null) {
				agent[i].refreshMesaMemory();
			}
		}
	}
	
	/**
	 * Request a write back of all buffered data on all agents and
	 * finalize the agents.
	 * 
	 * @param errMsgTarget for collecting warnings and error messages
	 *   from the agents during shutdown.
	 */
	public static void shutdown(StringBuilder errMsgTarget) {
		for (int i = 0; i < agent.length; i++) {
			Agent a = agent[i];
			if (a != null) { a.shutdown(errMsgTarget); }
		}
	}
	
	/*
	 * access to statistical data
	 */
	
	public static int getDiskReads() {
		if (diskAgent == null) { return 0; }
		return diskAgent.getReads();
	}
	
	public static int getDiskWrites() {
		if (diskAgent == null) { return 0; }
		return diskAgent.getWrites();
	}
	
	public static int getFloppyReads() {
		if (floppyAgent == null) { return 0; }
		return floppyAgent.getReads();
	}
	
	public static int getFloppyWrites() {
		if (floppyAgent == null) { return 0; }
		return floppyAgent.getWrites();
	}
	
	public static int getNetworkpacketsSent() {
		if (networkAgent == null) { return 0; }
		return networkAgent.getPacketsSentCount();
	}
	
	public static int getNetworkpacketsReceived() {
		if (networkAgent == null) { return 0; }
		return networkAgent.getPacketsReceivedCount();
	}
	
	/*
	 * floppy operations
	 */
	
	/**
	 * Insert a floppy immage into the virtual floppy drive.
	 * The file must have the raw size of a 3,5" floppy (i.e. 1440k == 1474560 bytes).
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
		return floppyAgent.insertFloppy(f, readonly);
	}
	
	/**
	 * Remove a floppy image from the virtual floppy drive. As modifications to the
	 * floppy are all buffered in memory, the floppy is written back to the (real) disk
	 * only when eject is called (explicitely or implicitely during {@code insertFloppy})
	 * or when {@code shutdown} is called.
	 */
	public static void ejectFloppy() {
		floppyAgent.ejectFloppy();
	}
	
	// for debugging purposes
	public static void dumpIoArea() {
		System.out.printf("Agent::InitializeAgent() :: begin\n");

		System.out.printf("  FCB addresses at IO-region start\n");
		for(int i = 0; i < agent.length; i++) {
			int agFcbPtr = Mem.ioArea + (i * 2);
			System.out.printf("     agent[%02d] -> FCB at 0x%08X : 0x%04X 0x%04X\n",
					i, agFcbPtr, Mem.readWord(agFcbPtr), Mem.readWord(agFcbPtr + 1));
		}

		for(int i = 0; i < agent.length; i++) {
			Agent a = agent[i];
			if (a == null) {
				System.out.printf("Agent::InitializeAgent() :: agent[%d] not present\n", i);
				continue;
			}
			int sz = a.getFcbSize();
			int ad = a.getFcbAddress();
			System.out.printf("Agent::InitializeAgent() :: agent[%d] .. FCBSize = %03d .. at 0x%08X .. name = '%s'\n",
					i, sz, ad, a.getAgentType().name());
			if (sz == 0) { continue; }
			System.out.printf("   FCB content:");
			for (int x = 0; x < sz; x++) {
				if ((x % 8) == 0) { System.out.printf("\n     "); }
				System.out.printf(" 0x%04X", Mem.readWord(ad + x));
			}
			System.out.printf("\n");
		}

		System.out.printf("Agent::InitializeAgent() -----------------------\n\n");
	}
}
