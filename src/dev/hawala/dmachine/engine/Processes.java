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

package dev.hawala.dmachine.engine;

import static dev.hawala.dmachine.engine.Mem.readField;
import static dev.hawala.dmachine.engine.Mem.writeField;

import dev.hawala.dmachine.engine.Cpu.MesaAbort;
import dev.hawala.dmachine.engine.Xfer.XferType;
import dev.hawala.dmachine.engine.agents.Agents;

/**
 * Basic data structures and common functions for handling processes
 * in the mesa engine as defined in PrincOps chapter "10 Processes".
 * <br>
 * This class provides the functionality required for implementing the
 * processes related instructions including the process scheduler
 * described in the PrincOps.
 * <br>
 * Additionally, it provides the bidirectional synchronization between
 * the Dwarf mesa engine and the Dwarf UI:
 * <ul>
 * <li>
 * UI =&gt; mesa engine<br>
 * the UI calls methods on the agents to inform about key-press changes,
 * mouse movements or the like; the agent then enqueues a special interrupt,
 * which is honored by the mesa engine as callback to agents between 2 instructions,
 * ensuring by this that no other changes occur in main memory while agents copy
 * UI data into the engines virtual memory.
 * </li>
 * <li>
 * mesa engine =&gt; UI<br>
 * at more or less regular intervals, the mesa engine calls back the UI through
 * a registered callback object during the regular timeout scan, again ensuring
 * that this synchonisation occurs between 2 instructions.
 * </li>
 * </ul>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Processes {	
	
	/*
	 * 10.1 Data Structures
	 */
	
	/* Queue, Condition, Monitor, PsbFlags, PsbLink
	 * 
	 * As the members of the data structures are defined at bit-boundaries inside a single
	 * word, member access is defined in terms either as FieldSpecs (intra-word field
	 * access for member longer than 1 bit) or bit masks (for single bit members).
	 * For all member, methods are provided to retrieve or update the value of one
	 * member in a word, with the updating methods returning the new word holding the
	 * new member value and the other members left with their old value.
	 * The complete set of accessor methods is provided for each data structure. 
	 */
	
	// common position / length of the PsbIndex in types: Queue, Condition, Monitor, PsbFlags, PsbLink 
	private static final int Common_psbIndex_Field = 0x39; 
	
	/*
	 *  Queue: word, tail:PsbIndex in bits 3..12
	 *  QueueHandler = LONG POINTER TO Queue
	 */
	public static short getQueue_tail(short q) { return readField(q, Common_psbIndex_Field); }
	public static short setQueue_tail(short q, int idx) { return writeField(q, Common_psbIndex_Field, (short)idx); }
	
	/*
	 *  Condition: word, tail:PsbIndex in bits 3..12, abortable: bit 14, wakeup: bit 15
	 */
	private static final int Condition_abortable_Mask = 0x0002;
	private static final int Condition_wakeup_Mask = 0x0001;
	
	public static short getCondition_tail(short c) { return readField(c, Common_psbIndex_Field); }
	public static short setCondition_tail(short c, int idx) { return writeField(c, Common_psbIndex_Field, (short)idx); }
	
	public static boolean isConditionAbortable(short w) { return ((w & Condition_abortable_Mask) != 0); }
	public static short setConditionAbortable(short w) { return (short)(w | Condition_abortable_Mask); }
	public static short unsetConditionAbortable(short w) { return (short)(w & (~Condition_abortable_Mask)); }
	
	public static boolean isConditionWakeup(short w) { return ((w & Condition_wakeup_Mask) != 0); }
	public static short setConditionWakeup(short w) { return (short)(w | Condition_wakeup_Mask); }
	public static short unsetConditionWakeup(short w) { return (short)(w & (~Condition_wakeup_Mask)); }
	
	/*
	 *  Monitor: word, tail:PsbIndex in bits 3..12, available: bits 13..14, locked: bit 15
	 */
	private static final int Monitor_locked_Mask = 0x0001;
	
	public static short getMonitor_tail(short m) { return readField(m, Common_psbIndex_Field); }
	public static short setMonitor_tail(short m, int idx) { return writeField(m, Common_psbIndex_Field, (short)idx); }
	
	public static boolean isMonitorLocked(short w) { return ((w & Monitor_locked_Mask) != 0); }
	public static short setMonitorLocked(short w) { return (short)(w | Monitor_locked_Mask); }
	public static short unsetMonitorLocked(short w) { return (short)(w & (~Monitor_locked_Mask)); }
	
	/*
	 *  PsbFlags: word, cleanup:PsbIndex in bits 3..12, waiting: bit 14, abort: bit 15
	 */
	private static final int PsbFlags_waiting_Mask = 0x0002;
	private static final int PsbFlags_abort_Mask = 0x0001;
	
	public static short getPsbFlags_cleanup(short f) { return readField(f, Common_psbIndex_Field); }
	public static short setPsbFlags_cleanup(short f, int idx) { return writeField(f, Common_psbIndex_Field, (short)idx); }
	
	public static boolean isPsbFlagsWaiting(short w) { return ((w & PsbFlags_waiting_Mask) != 0); }
	public static short setPsbFlagsWaiting(short w) { return (short)(w | PsbFlags_waiting_Mask); }
	public static short unsetPsbFlagsWaiting(short w) { return (short)(w & (~PsbFlags_waiting_Mask)); }
	
	public static boolean isPsbFlagsAbort(short w) { return ((w & PsbFlags_abort_Mask) != 0); }
	public static short setPsbFlagsAbort(short w) { return (short)(w | PsbFlags_abort_Mask); }
	public static short unsetPsbFlagsAbort(short w) { return (short)(w & (~PsbFlags_abort_Mask)); }
	
	/*
	 *  PsbLink: word, priority: bits 0..2, next:PsbIndex in bits 3..12, failed: bit 13, permanent: bit 14, preempted: bit 15
	 */
	private static final int PsbLink_priority_Field = 0x02;
	private static final int PsbLink_failed_Mask = 0x0004;
	private static final int PsbLink_permanent_Mask = 0x0002;
	private static final int PsbLink_preempted_Mask = 0x0001;
	
	public static short getPsbLink_priority(short l) { return readField(l, PsbLink_priority_Field); }
	public static short setPsbLink_priority(short l, int prio) { return writeField(l, PsbLink_priority_Field, (short)prio); }
	
	public static short getPsbLink_next(short l) { return readField(l, Common_psbIndex_Field); }
	public static short setPsbLink_next(short l, int idx) { return writeField(l, Common_psbIndex_Field, (short)idx); }
	
	public static boolean isPsbLinkFailed(short w) { return ((w & PsbLink_failed_Mask) != 0); }
	public static short setPsbLinkFailed(short w) { return (short)(w | PsbLink_failed_Mask); }
	public static short unsetPsbLinkFailed(short w) { return (short)(w & (~PsbLink_failed_Mask)); }
	
	public static boolean isPsbLinkPermanent(short w) { return ((w & PsbLink_permanent_Mask) != 0); }
	public static short setPsbLinkPermanent(short w) { return (short)(w | PsbLink_permanent_Mask); }
	public static short unsetPsbLinkPermanent(short w) { return (short)(w & (~PsbLink_permanent_Mask)); }
	
	public static boolean isPsbLinkPreempted(short w) { return ((w & PsbLink_preempted_Mask) != 0); }
	public static short setPsbLinkPreempted(short w) { return (short)(w | PsbLink_preempted_Mask); }
	public static short unsetPsbLinkPreempted(short w) { return (short)(w & (~PsbLink_preempted_Mask)); }
	
	/*
	 *  ProcessStateBlock member offsets
	 */
	public static final int ProcessStateBlock_link = 0;      // PsbLink
	public static final int ProcessStateBlock_flags = 1;     // PsbFlags
	public static final int ProcessStateBlock_context = 2;   // POINTER (to frame or state-vector)
	public static final int ProcessStateBlock_timeout = 3;   // Ticks
	public static final int ProcessStateBlock_mds = 4;       // CARDINAL (high order 16 bits of MDS register)
	public static final int ProcessStateBlock_available = 5; // UNSPECIFIED (unused)
	public static final int ProcessStateBlock_sticky = 6;    // LONG UNSPECIFIED (for floating point ops)
	public static final int ProcessStateBlock_Size = 8;
	
	// StateAllocationTable: TYPE = ARRAY Priority OF POINTER TO StateVector
	public static final int StateAllocationTable_Size = 8; // 8 words: 8 priorities => 8 POINTERs
	
	// InterruptVector: TYPE = ARRAY InterruptLevel OF InterruptItem
	// InterruptLevel: TYPE = [0 .. WordSize)
	// InterruptItem: TYPE = MACHINE DEPENDENT RECORD [condition (0): Condition, available (1): UNSPECIFIED]
	private static final int InterruptLevels = 16;
	private static final int InterruptItem_condition = 0;
	private static final int InterruptItem_available = 1;
	public static final int InterruptItem_Size = 2;
	public static final int InterruptVector_Size = InterruptItem_Size * InterruptLevels;
	
	// FaultVector: TYPE = ARRAY Faultlndex OF FaultQueue
	// Faultlndex: TYPE = [O..8);
	// FaultQueue: TYPE = MACHINE DEPENDENT RECORD [queue (0): Queue, condition (1): Condition];
	private static final int FaultQueue_Size = 2;
	private static final int FaultVector_queue = 0;
	private static final int FaultVector_condition = 1;
	public static final int FaultVector_Size = 16; // 8 indices x 2 words
	
	// header area in the ProcessDataArea overlaid over the first entries
	// these are LONG POINTERs
	// (the addresses require that Cpu.PDA is defined as constant (static final)
	public static final int PDA_LP_header_ready = Cpu.PDA;
	public static final int PDA_LP_header_count = Cpu.PDA + 1;
	public static final int PDA_LP_header_state = Cpu.PDA + 8;
	public static final int PDA_LP_header_interrupt = Cpu.PDA + 8 + StateAllocationTable_Size;
	public static final int PDA_LP_header_fault = Cpu.PDA + 8 + StateAllocationTable_Size + InterruptVector_Size;
	public static final int ProcessDataArea_header_Size = 
			  1   // ready: Queue (1 word)
			+ 1   // count: CARDINAL
			+ 1   // unused: UNSPECIFIED
			+ 5   // available: ARRAY [0..5) OF UNSPECIFIED
			+ StateAllocationTable_Size
			+ InterruptVector_Size
			+ FaultVector_Size; // this should be 64 words in total == 8 PDA entries out of 1024
	
	// PsbIndex ; 0..1023
	public static final int PsbIndex_Max = 1024;
	public static final int PsbNull = 0;
	public static final int PsbStart = (ProcessDataArea_header_Size + ProcessStateBlock_Size - 1) / ProcessStateBlock_Size;

	public static /* LONG POINTER */ int lengthenPdaPtr(/* POINTER */ int ptr ) {
		return Cpu.PDA + (ptr & 0xFFFF);
	}
	
	public static /* POINTER */ short offsetPda(/* LONG POINTER */ int ptr) {
		if (((ptr - Cpu.PDA) & 0xFFFF0000) != 0) { Cpu.ERROR("offsetPda :: highHalf(ptr - Cpu.PDA) != 0"); }
		return (short)((ptr - Cpu.PDA) & 0x0000FFFF);
	}
	
	public static /* word */ short readPdaWord(/* POINTER */ int ptr) {
		return Mem.readWord(lengthenPdaPtr(ptr));
	}
	
	public static void writePdaWord(/* POINTER */ int ptr, short word) {
		Mem.writeWord(lengthenPdaPtr(ptr),  word);
	}
	
	public static int psbHandle(int index) {
		return (index & 0x03FF) * ProcessStateBlock_Size; // limit to 0..1023
	}
	
	public static short psbIndex(int handle) {
		return (short)((handle & 0x0000FFFF) / ProcessStateBlock_Size); // TODO: !! this may deliver >= 1024 ??
	}
	
//	// get PsbIndex from Queue | PsbLink | Condition | Monitor 
//	private static int psbIndexOf(short item) {
//		return readField(item, Queue_tail);
//	}
	
	private static short readPSBword(int index, int offset) {
		int ptr = Cpu.PDA + ((index & 0x0000FFFF) * ProcessStateBlock_Size) + offset;
		return Mem.readWord(ptr);
	}
	
	private static void writePSBword(int index, int offset, short word) {
		int ptr = Cpu.PDA + ((index & 0x0000FFFF) * ProcessStateBlock_Size) + offset;
		Mem.writeWord(ptr, word);
	}
	
	/*
	 * access utils for members of the PSB at index
	 */
	
	public static short fetchPSB_link(int index) {
		return readPSBword(index, ProcessStateBlock_link);
	}
	
	public static void storePSB_link(int index, short value) {
		writePSBword(index, ProcessStateBlock_link, value);
	}
	
	public static short fetchPSB_flags(int index) {
		return readPSBword(index, ProcessStateBlock_flags);
	}
	
	public static void storePSB_flags(int index, short value) {
		writePSBword(index, ProcessStateBlock_flags, value);
	}
	
	public static short fetchPSB_context(int index) {
		return readPSBword(index, ProcessStateBlock_context);
	}
	
	public static void storePSB_context(int index, short value) {
		writePSBword(index, ProcessStateBlock_context, value);
	}
	
	public static short fetchPSB_timeout(int index) {
		return readPSBword(index, ProcessStateBlock_timeout);
	}
	
	public static void storePSB_timeout(int index, short value) {
		writePSBword(index, ProcessStateBlock_timeout, value);
	}
	
	public static short fetchPSB_mds(int index) {
		return readPSBword(index, ProcessStateBlock_mds);
	}
	
	public static void storePSB_mds(int index, short value) {
		writePSBword(index, ProcessStateBlock_mds, value);
	}
	
	/*
	 * process support routines
	 */
	
	/*
	 * 10.2.1 Monitor Entry
	 */
	
	public static void enterFailed(/* LONG POINTER TO Monitor */ int m) {
		short link = fetchPSB_link(Cpu.PSB);
		link = setPsbLinkFailed(link);
		storePSB_link(Cpu.PSB, link);
		requeue(PDA_LP_header_ready, m, Cpu.PSB);
		reschedule(false);
	}
	
	/*
	 * 10.2.2 Monitor Exit
	 */
	
	public static boolean exit(/* LONG POINTER TO Monitor */ int m) {
		short mon = Mem.readWord(m);
		if (!isMonitorLocked(mon)) { Cpu.ERROR("exit :: exiting non-locked monitor"); }
		mon = unsetMonitorLocked(mon);
		Mem.writeWord(m, mon);
		
		short mon_tail = getMonitor_tail(mon);
		boolean requeue = (mon_tail != PsbNull);
		if (requeue) {
			short link = fetchPSB_link(mon_tail);
			requeue(m, PDA_LP_header_ready, getPsbLink_next(link));
		}
		return requeue;
	}
	
	/*
	 * 10.2.5 Notify and Broadcast
	 */
	
	public static void wakeHead(/* LONG POINTER TO Condition */ int c) {
		short cond = Mem.readWord(c);
		short link = fetchPSB_link(getCondition_tail(cond));
		short link_next = getPsbLink_next(link);
		short flags = fetchPSB_flags(link_next);
		flags = unsetPsbFlagsWaiting(flags);
		storePSB_flags(link_next, flags);
		storePSB_timeout(link_next, (short)0);
		requeue(c, PDA_LP_header_ready, link_next);
	}
	
	/*
	 * 10.3.1 Queuing Procedures
	 */
	
	public static void requeue(int src, int dst, short psb) {
		if (psb == PsbNull) { Cpu.ERROR("requeue :: psb == psnBull"); }
		dequeue(src, psb);
		enqueue(dst, psb);
	}
	
	private static void dequeue(int src, short psb) {
		short queue = 0;
		int que = src;
		if (que != 0) { queue = Mem.readWord(que); }
		
		short prev = PsbNull; // PsbIndex
		short link = fetchPSB_link(psb);
		if (getPsbLink_next(link) != psb) {
			short temp; // PsbLink
			prev = (que == 0) ? psb : getQueue_tail(queue);
			while(true) {
				temp = fetchPSB_link(prev);
				if (getPsbLink_next(temp) == psb) { break; }
				prev = getPsbLink_next(temp);
			}
			temp = setPsbLink_next(temp, getPsbLink_next(link));
			storePSB_link(prev, temp);
		}
		
		if (que == 0) {
			short flags = fetchPSB_flags(psb);
			flags = setPsbFlags_cleanup(flags, getPsbLink_next(link));
			storePSB_flags(psb, flags);
		} else if (getQueue_tail(queue) == psb) {
			queue = setQueue_tail(queue, prev);
			Mem.writeWord(que, queue);
		}
	}
	
	private static void enqueue(int dst, short psb) {
		int que = dst;
		short queue = Mem.readWord(que);
		short link = fetchPSB_link(psb);
		
		if (getQueue_tail(queue) == PsbNull) {
			link = setPsbLink_next(link, psb);
			storePSB_link(psb, link);
			queue = setQueue_tail(queue, psb);
			Mem.writeWord(que, queue);
		} else {
			short prev = getQueue_tail(queue);
			short currentlink = fetchPSB_link(prev);
			if (getPsbLink_priority(currentlink) >= getPsbLink_priority(link)) {
				queue = setQueue_tail(queue, psb);
				Mem.writeWord(que,  queue);
			} else {
				while(true) {
					short nextlink = fetchPSB_link(getPsbLink_next(currentlink));
					if (getPsbLink_priority(link) > getPsbLink_priority(nextlink)) { break; }
					prev = getPsbLink_next(currentlink);
					currentlink = nextlink;
				}
			}
			link = setPsbLink_next(link, getPsbLink_next(currentlink));
			storePSB_link(psb, link);
			currentlink = setPsbLink_next(currentlink, psb);
			storePSB_link(prev, currentlink);
		}
	}
	
	/*
	 * 10.3.2 Cleanup Links
	 */
	
	public static void cleanupCondition(/* LONG POINTER TO Condition */ int c) {
		short cond = Mem.readWord(c);
		short psb = (short)getCondition_tail(cond);
		if (psb != PsbNull) {
			short flags = fetchPSB_flags(psb);
			if (getPsbFlags_cleanup(flags) != PsbNull) {
				while(true) {
					if (getPsbFlags_cleanup(flags) == psb) {
						cond = unsetConditionWakeup(cond);
						cond = setCondition_tail(cond, PsbNull);
						Mem.writeWord(c, cond);
						return;
					}
					psb = getPsbFlags_cleanup(flags);
					flags = fetchPSB_flags(psb);
					if (getPsbFlags_cleanup(flags) == PsbNull) { break; }
				}
				short head = psb;
				while(true) {
					short link = fetchPSB_link(psb);
					if (getPsbLink_next(link) == head) { break; }
					psb = getPsbLink_next(link);
				}
				cond = setCondition_tail(cond, psb);
				Mem.writeWord(c, cond);
			}
		}
	}
	
	/*
	 * 10.4.1 Scheduler
	 */
	
	public static void reschedule(boolean preemption) {
		if (Cpu.running) {
			saveProcess(preemption);
		}
		
		short queue = Mem.readWord(PDA_LP_header_ready);
		short queueTail = getQueue_tail(queue);
		if (queueTail == PsbNull) {
			rescheduleBusyWait();
			return;
		}
		
		short link = fetchPSB_link(queueTail);
		short psb;
		while(true) {
			psb = getPsbLink_next(link);
			link = fetchPSB_link(psb);
			if (isPsbLinkPermanent(link) || isPsbLinkPreempted(link) || !emptyState(getPsbLink_priority(link))) {
				break;
			}
			if (psb == queueTail) {
				rescheduleBusyWait();
				return;
			}
		}
		
		Cpu.PSB = psb;
		Cpu.savedPC = 0;
		Cpu.PC = 0;
		Cpu.LF = loadProcess();
		Cpu.running = true;
		Xfer.impl.xfer(Cpu.LF, 0, XferType.xprocessSwitch, false);
	}
	
	private static void rescheduleBusyWait() {
		if (!interruptsEnabled()) { Cpu.rescheduleError(); }
		Cpu.running = false;
	}
	
	private static void saveProcess(boolean preemption) {
		// start of: BEGIN ENABLE Abort => ERROR;
		try {
			short link = fetchPSB_link(Cpu.PSB);
			boolean link_permanent = isPsbLinkPermanent(link);
			if (Cpu.validContext()) {
				Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
			}
			if (preemption) {
				link = setPsbLinkPreempted(link);
				int state = (!link_permanent)
						? allocState(getPsbLink_priority(link))
						: lengthenPdaPtr(fetchPSB_context(Cpu.PSB));
				Cpu.saveStack(state);
				Mem.writeWord(state + Cpu.StateVector_frame, (short)(Cpu.LF & 0xFFFF));
				if (!link_permanent) {
					storePSB_context(Cpu.PSB, offsetPda(state));
				}
			} else {
				link = unsetPsbLinkPreempted(link);
				if (!link_permanent) {
					storePSB_context(Cpu.PSB, (short)(Cpu.LF & 0xFFFF));
				} else {
					int state = lengthenPdaPtr(fetchPSB_context(Cpu.PSB));
					Mem.writeWord(state + Cpu.StateVector_frame, (short)(Cpu.LF & 0xFFFF));
				}
			}
			storePSB_link(Cpu.PSB, link);
		} catch(MesaAbort mf) {
			// end of: BEGIN ENABLE Abort => ERROR;
			Cpu.ERROR("saveProcess :: received Abort-exception");
		}
	}
	
	private static int /* LF */ loadProcess() {
		// start of: BEGIN ENABLE Abort => ERROR;
		try {
			short link = fetchPSB_link(Cpu.PSB);
			boolean link_permanent = isPsbLinkPermanent(link);
			short frame = fetchPSB_context(Cpu.PSB);
			if (isPsbLinkPreempted(link)) {
				int state = lengthenPdaPtr(frame);
				Cpu.loadStack(state);
				frame = Mem.readWord(state + Cpu.StateVector_frame);
				if (!link_permanent) {
					freeState(getPsbLink_priority(link), state);
				}
			} else {
				if (isPsbLinkFailed(link)) {
					Cpu.push(PrincOpsDefs.FALSE);
					link = unsetPsbLinkFailed(link);
					storePSB_link(Cpu.PSB, link);
				}
				if (isPsbLinkPermanent(link)) {
					int state = lengthenPdaPtr(frame);
					frame = Mem.readWord(state + Cpu.StateVector_frame);
				}
			}
			int mds = fetchPSB_mds(Cpu.PSB);
			Cpu.MDS = mds << PrincOpsDefs.WORD_BITS;
			return frame;
		} catch(MesaAbort mf) {
			// end of: BEGIN ENABLE Abort => ERROR;
			Cpu.ERROR("loadProcess :: received Abort-exception");
			return -1; // keep the compiler happy (Cpu.ERROR does not return, but the compiler does not know that...)
		}
	}
	
	/*
	 * 10.4.2.2 State Vector Allocation
	 */
	
	private static boolean emptyState(int pri) {
		short state = Mem.readWord(PDA_LP_header_state + (pri & 0x0007));
		return (state == 0);
	}
	
	private static /* StateHandle */ int allocState(int pri) {
		int statesCell = PDA_LP_header_state + (pri & 0x0007);
		short offset = Mem.readWord(statesCell);
		if (offset == 0) { Cpu.ERROR("allocState :: offset == 0 for priority " + pri); }
		int state = lengthenPdaPtr(offset);
		Mem.writeWord(statesCell, Mem.readWord(state));
		return state;
	}
	
	private static void freeState(int pri, /* StateHandle */ int state) {
		int statesCell = PDA_LP_header_state + (pri & 0x0007);
		Mem.writeWord(state, Mem.readWord(statesCell));
		Mem.writeWord(statesCell, offsetPda(state));
	}
	
	/*
	 * 10.4.3 Faults
	 */
	
	public static int faultOne(int fi, short parameter) {
		int psb = fault(fi);
		short state = fetchPSB_context(psb);
		writePdaWord(state + Cpu.StateVector_data, parameter);
		throw new MesaAbort();
	}
	
	public static int faultTwo(int fi, int parameter) {
		int psb = fault(fi);
		short state = fetchPSB_context(psb);
		writePdaWord(state + Cpu.StateVector_data, (short)(parameter & 0x0000FFFF));
		writePdaWord(state + Cpu.StateVector_data + 1, (short)(parameter >>> PrincOpsDefs.WORD_BITS));
		throw new MesaAbort();
	}
	
	private static /* PsbIndex */ int fault(int fi) {
		short faulted = Cpu.PSB;
		int lpFaultIndexCell = PDA_LP_header_fault + (fi * FaultQueue_Size);
		requeue(PDA_LP_header_ready, lpFaultIndexCell + FaultVector_queue, faulted);
		notifyWakeup(lpFaultIndexCell + FaultVector_condition);
		Cpu.PC = Cpu.savedPC;
		Cpu.SP = Cpu.savedSP;
		reschedule(true);
		return faulted;
	}
	
	/*
	 * Extension to PrincOps: section "4.1 Interpreter" defines that instructions
	 * are executed only if the cpu state is running, but continuing to check
	 * for interrupts and timeouts. This creates a very tight loop in the interpreter
	 * while most time there are no interrupts or timeouts to honor, therefore
	 * needlessly consuming (real hardware) CPU for nothing.
	 * For this reason, a real idling mechanism is used in Dwarfs mesa engine, putting
	 * the (Java) interpreter thread to sleep for a limited time. The PrincOps requirements
	 * (interrupt responsiveness and maximal timeout scan intervals) are met by restarting
	 * the interpreter (i.e. end sleeping) when an interrupt is enqueued and limiting the
	 * sleep time to a time frame below the required timeout scan interval.       
	 */
	
	private static final int NOT_RUNNING_SLEEP_MSECS = 2;
	
	private static final Object lock = new Object();
	
	/**
	 * Hold execution for a limited time, restarting execution when an
	 * interrupt is enqueued.
	 */
	public static void idle() {
		synchronized(lock) {
			try {
				lock.wait(NOT_RUNNING_SLEEP_MSECS);
			} catch (InterruptedException e) {
				// ignored
			}
		}
	}
	
	/*
	 * 10.4.4 Interrupts
	 */
	
	public static boolean interruptPending() {
		return (Cpu.WP.get() != 0) && interruptsEnabled();
	}
	
	public static boolean checkforInterrupts() {
		if (interruptPending()) {
			return interrupt();
		} else {
			return false;
		}
	}
	
	// special interrupt for agents requesting to acces the mesa virtual memory
	private static final int DATA_REFRESH_INTERRUPT = 0x40000000;
	
	// special interrupt requesting to stop the mesa engine (e.g. by a UI button)
	private static final int EXTERNAL_STOP_INTERRUPT = 0x10000000;
	
	private static void innerRequestInterrupt(int intMask) {
		int oldWP = Cpu.WP.get();
		int newWP = oldWP | intMask;
		while(!Cpu.WP.compareAndSet(oldWP, newWP)) {
			oldWP = Cpu.WP.get();
			newWP = oldWP | intMask;
		}
		synchronized(lock) {
			lock.notifyAll();
		}
	}
	
	/**
	 * Enqueue one or more standard interrupts to the mesa engine
	 * @param intMask bitmask for the interrupt(s) to raise
	 */
	public static void requestMesaInterrupt(short intMask) {
		innerRequestInterrupt(intMask & 0xFFFF);
	}
	
	/**
	 * Enqueue a data refresh interrupt for processing pending
	 * data transfers from agents to mesa virtual memory.
	 */
	public static void requestDataRefresh() {
		innerRequestInterrupt(DATA_REFRESH_INTERRUPT);
	}
	
	/**
	 * Enqueue the request to stop the mesa engine.
	 */
	public static void requestMesaEngineStop() {
		innerRequestInterrupt(EXTERNAL_STOP_INTERRUPT);
	}
	
	public static boolean interrupt() {
		short mask = 1;
		boolean requeue = false;
		
		// atomically get the wake-up bits
		int pendingWakeups = Cpu.WP.get();
		while(!Cpu.WP.compareAndSet(pendingWakeups, 0)) {
			pendingWakeups = Cpu.WP.get();
		}
		if (pendingWakeups == 0) { return false; }
		
		// the mesa PrincOps wakeups (subset of all possible interrupts to the mesa engine)
		short wakeups = (short)(pendingWakeups & 0xFFFF);
		
		// is a "stop the engine" request pending?
		if ((pendingWakeups & EXTERNAL_STOP_INTERRUPT) != 0) {
			throw new Cpu.MesaStopped("Mesa engine stopped by external request");
		}
		
		// ensure that the mesa memory has all ingone external data if requested
		if ((pendingWakeups & DATA_REFRESH_INTERRUPT) != 0) {
			Agents.processPendingMesaMemoryUpdates();
		}
		
		// if no mesa wakeups pending => done
		if (wakeups == 0) {
			return false;
		}
		
		// notify the conditions to be waked up and request requeuing if necessary
		for (int i = InterruptLevels - 1; i >= 0; i--) {
			if ((wakeups & mask) != 0) {
				requeue |= notifyWakeup(PDA_LP_header_interrupt + (i * InterruptItem_Size) + InterruptItem_condition);
			}
			mask <<= 1;
		}
		return requeue;
	}
	
	public static boolean notifyWakeup(/* LONG POINTER TO Condition */ int c) {
		boolean requeue = false;
		cleanupCondition(c);
		short cond = Mem.readWord(c);
		if (getCondition_tail(cond) == PsbNull) {
			cond = setConditionWakeup(cond);
			Mem.writeWord(c, cond);
		} else {
			wakeHead(c);
			requeue = true;
		}
		return requeue;
	}
	
	/*
	 * 10.4.4.3 Disabling Interrupts
	 */

	
	public static void enableInterrupts() {
		Cpu.WDC = (short)Math.max(0, Cpu.WDC - 1);
	}
	
	public static void disableInterrupts() {
		Cpu.WDC++;
	}
	
	public static boolean interruptsEnabled() {
		return (Cpu.WDC == 0);
	}
	
	/*
	 * 10.4.5 Timeouts
	 */
	
	// Ticks: TYPE = CARDINAL
	// TimeOutInterval: TYPE = LONG POINTER
	
	private static int time = 0;
	
	public static void resetPTC(int to) {
		Cpu.PTC = to;
		time = Cpu.IT();
	}
	
	// UI refreshing:
	// -> 25 screen refreshs per second means one refresh each 40 ms
	// -> 5 statistics refreshs per second means ~ 1 refresh after 5 screen refreshs 
	private static final long UI_REFRESH_INTERVAL = 37; // milliseconds
	private static final int STATS_REFRESH_INTERVAL = 5;
	private static long nextUiRefresh = 0;
	private static int lastMpNotified = -1;
	private static int statisticsThrottle = STATS_REFRESH_INTERVAL;  
	
	private static volatile iMesaMachineDataAccessor displayRefresher = null;
	
	private static short[] dummyPageFlags = null;
	
	public static void registerUiRefreshCallback(iMesaMachineDataAccessor refresher) {
		displayRefresher = refresher;
	}
	
	// the invoker must throttle usage of this method, for optimizing to avoid checking too often
	// as System.nenoTime() / System.currentTimeMillis() effectively slow  down things in Java...
	public static boolean checkForTimeouts() {
		// Dwarf implementation specific part: refresh UI at (more or less) regular intervals
		
		// ensure that the mesa memory has all ingone external data
		Agents.processPendingMesaMemoryUpdates();
		
		// cyclically refresh the ui
		long now = System.currentTimeMillis();
		if (now > nextUiRefresh) {
			// set next refresh wakeup timestamp
			nextUiRefresh = now + UI_REFRESH_INTERVAL;
			
			// refresh if we have a connected UI
			iMesaMachineDataAccessor refresher = displayRefresher;
			if (refresher != null) {
				// notify MP if changed
				int currMP = Cpu.getMP();
				if (currMP != lastMpNotified) {
					refresher.acceptMP(currMP);
					lastMpNotified = currMP;
				}
				
				// notify statistics at a lower pace
				if (--statisticsThrottle <= 0) {
					refresher.acceptStatistics(
							Cpu.insns,
							Agents.getDiskReads(),
							Agents.getDiskWrites(),
							Agents.getFloppyReads(),
							Agents.getFloppyWrites(),
							Agents.getNetworkpacketsReceived(),
							Agents.getNetworkpacketsSent());
					statisticsThrottle = STATS_REFRESH_INTERVAL;
				}
				
				// refresh screen, handling the case when the display memory is not mapped into virtual memory
				short[] vPageFlags = Mem.pageFlags;
				if (Mem.displayFirstMappedVirtualPage == 0) {
					if (dummyPageFlags == null) {
						dummyPageFlags = new short[Mem.getDisplayPageSize()];
						for (int i = 0; i < dummyPageFlags.length; i++) {
							dummyPageFlags[i] = PrincOpsDefs.MAPFLAGS_REFERENCED | PrincOpsDefs.MAPFLAGS_DIRTY;
						}
					}
					vPageFlags = dummyPageFlags;
				} else {
					dummyPageFlags = null;
				}
				refresher.accessRealMemory(
					Mem.getDisplayRealMemory(),
					Mem.getDisplayRealPage() * PrincOpsDefs.WORDS_PER_PAGE, 
					Mem.getDisplayPageSize() * PrincOpsDefs.WORDS_PER_PAGE,
					vPageFlags,
					Mem.displayFirstMappedVirtualPage
					);
				Mem.resetDisplayPagesFlags();
			}
		}
		
		// PrincOps part
		int temp = Cpu.IT();
		if (interruptsEnabled() && (temp - time) > Cpu.TimeOutInterval) {
			time = temp;
			Cpu.PTC = (Cpu.PTC + 1) & 0xFFFF; // Cpu.PTC++;
			if (Cpu.PTC == 0) { Cpu.PTC++; }
			return timeoutScan();
		} else {
			return false;
		}
	}
	
	private static boolean timeoutScan() {
		boolean requeue = false;
		int count = Mem.readWord(PDA_LP_header_count) & 0xFFFF;
		for (short psb = PsbStart; psb < (PsbStart + count); psb++) {
			int timeout = fetchPSB_timeout(psb) & 0xFFFF;
			if (timeout != 0 && timeout == Cpu.PTC) {
				short flags = fetchPSB_flags(psb);
				flags = unsetPsbFlagsWaiting(flags);
				storePSB_flags(psb, flags);
				storePSB_timeout(psb, (short)0);
				requeue(0, PDA_LP_header_ready, psb);
				requeue = true;
			}
		}
		return requeue;
	}
	
	/*
	 * dump utilities for Cpu-debugger
	 */
	
	public static void dumpQueueChain(String header, int lpQueue) {
		short count = readPdaWord(PDA_LP_header_count);
		short queue = readPdaWord(lpQueue);
		int idx = getPsbLink_next(queue);
		int queueIdx = idx;
		Cpu.logf("             %s - queue 0x%04X at 0x%08X\n", header, queue, lpQueue);
		while(true) {
			short link = fetchPSB_link(idx);
			short flags = fetchPSB_flags(idx);
			short context = fetchPSB_context(idx);
			short timeout = fetchPSB_timeout(idx);
			short mds = fetchPSB_mds(idx);
			Cpu.logf("               PSB[%d] : link = 0x%04X , flags = 0x%04X , context = 0x%04X , timeout = 0x%04X , mds = 0x%04X\n",
				idx, link, flags, context, timeout, mds);
			Cpu.logf("                      link[ priority = %d , next = %3d , %sfailed , %spermanent , %spreempted ]\n",
					getPsbLink_priority(link),
					getPsbLink_next(link),
					isPsbLinkFailed(link) ? "+" : "-",
					isPsbLinkPermanent(link) ? "+" : "-",
					isPsbLinkPreempted(link) ? "+" : "-"
					);
			Cpu.logf("                     flags[ cleanup = %3d , %swaiting , %sabort ]\n",
					getPsbFlags_cleanup(flags),
					isPsbFlagsWaiting(flags) ? "+" : "-",
					isPsbFlagsAbort(flags) ? "+" : "-");
			
			short next = fetchPSB_link(idx);
			Cpu.logf("               [%d] -> 0x%04X\n", idx, next);
			idx = getPsbLink_next(next);
			if (idx == queueIdx) { break; }
			if (idx >= (count + PsbStart)) {
				Cpu.logf("        ## next idx(%d) >= count(%d)+PsbStart(%d) ##\n", idx, count, PsbStart);
				break;
			}
		}
	}
	
	public static void dumpProcessStatusArea() {
		short count = readPdaWord(PDA_LP_header_count);
		short ready = readPdaWord(PDA_LP_header_ready);
		Cpu.logf("\n        ** ProcessStatusArea: PDA = 0x%08X , header_size = %d , 1st PSB-idx = %d , ready = 0x%04X , count = %03d\n",
				Cpu.PDA,
				ProcessDataArea_header_Size,
				PsbStart,
				ready,
				count);
		
		int idx = getPsbLink_next(ready);
		int readyIdx = idx;
		Cpu.logf("             ready queue (indexes):\n");
		Cpu.logf("               0x%04X\n", ready);
		while(true) {
			short next = fetchPSB_link(idx);
			Cpu.logf("               [%d] -> 0x%04X\n", idx, next);
			idx = getPsbLink_next(next);
			if (idx == readyIdx) { break; }
			if (idx >= (count + PsbStart)) {
				Cpu.logf("        ## next idx >= count+PsbStart ##\n");
				break;
			}
		}
		
		for(int i = PsbStart; i < PsbStart + count; i++) {
			short link = fetchPSB_link(i);
			short flags = fetchPSB_flags(i);
			short context = fetchPSB_context(i);
			short timeout = fetchPSB_timeout(i);
			short mds = fetchPSB_mds(i);
			Cpu.logf("         PSB[%d] : link = 0x%04X , flags = 0x%04X , context = 0x%04X , timeout = 0x%04X , mds = 0x%04X\n",
				i, link, flags, context, timeout, mds);
			Cpu.logf("                      link[ priority = %d , next = %3d , %sfailed , %spermanent , %spreempted ]\n",
				getPsbLink_priority(link),
				getPsbLink_next(link),
				isPsbLinkFailed(link) ? "+" : "-",
				isPsbLinkPermanent(link) ? "+" : "-",
				isPsbLinkPreempted(link) ? "+" : "-"
				);
			Cpu.logf("                     flags[ cleanup = %3d , %swaiting , %sabort ]\n",
				getPsbFlags_cleanup(flags),
				isPsbFlagsWaiting(flags) ? "+" : "-",
				isPsbFlagsAbort(flags) ? "+" : "-");
		}
	}
	
}
