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

package dev.hawala.dmachine.engine.opcodes;

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Opcodes.OpImpl;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;

/**
 * Implementation of instructions defined in PrincOps 4.0
 * in chapter: 10 Processes  
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch10_Processes {
	
	/*
	 * 10.2 Process Instructions
	 */
	
	/*
	 * 10.2.1 Monitor Entry
	 */
	
	// ME - Monitor Entry
	public static final OpImpl OPC_xF1_ME = () -> {
		int m = Cpu.popLong();
		Cpu.checkEmptyStack();
		
		short mon = Mem.readWord(m);
		if (!Processes.isMonitorLocked(mon)) {
			mon = Processes.setMonitorLocked(mon);
			Mem.writeWord(m, mon);
			Cpu.push(PrincOpsDefs.TRUE);
		} else {
			Processes.enterFailed(m);
		}
	};
	
	/*
	 * 10.2.2 Monitor Exit
	 */
	
	// MX - Monitor Exit
	public static final OpImpl OPC_xF2_MX = () -> {
		int m = Cpu.popLong();
		Cpu.checkEmptyStack();
		
		if (Processes.exit(m)) {
			Processes.reschedule(false);
		}
	};
	
	/*
	 * 10.2.3 Monitor Wait
	 */
	
	// MW - Monitor Wait
	public static final OpImpl ESC_x02_MW = () -> {
		int t = Cpu.pop() & 0xFFFF;
		int c = Cpu.popLong();
		int m = Cpu.popLong();
		Cpu.checkEmptyStack();
		
		Processes.cleanupCondition(c);
		
		boolean requeue = Processes.exit(m);
		short flags = Processes.fetchPSB_flags(Cpu.PSB);
		short cond = Mem.readWord(c);
		
		if (!Processes.isPsbFlagsAbort(flags) || !Processes.isConditionAbortable(cond)) {
			if (Processes.isConditionWakeup(cond)) {
				cond = Processes.unsetConditionWakeup(cond);
				Mem.writeWord(c,  cond);
			} else {
				Processes.storePSB_timeout(Cpu.PSB, (t == 0) ? 0 : (short)Math.max(1, (Cpu.PTC + t) & 0xFFFF));
				flags = Processes.setPsbFlagsWaiting(flags);
				Processes.storePSB_flags(Cpu.PSB, flags);
				Processes.requeue(Processes.PDA_LP_header_ready, c, Cpu.PSB);
				requeue = true;
			}
		}
		if (requeue) {
			Processes.reschedule(false);
		}
	};
	
	/*
	 * 10.2.4 Monitor Reentry
	 */
	
	// MR - Monitor Reentry
	public static final OpImpl ESC_x03_MR = () -> {
		int c = Cpu.popLong();
		int m = Cpu.popLong();
		Cpu.checkEmptyStack();
		
		short mon = Mem.readWord(m);
		if (!Processes.isMonitorLocked(mon)) {
			Processes.cleanupCondition(c);
			short flags = Processes.fetchPSB_flags(Cpu.PSB);
			flags = Processes.setPsbFlags_cleanup(flags, Processes.PsbNull);
			Processes.storePSB_flags(Cpu.PSB, flags);
			if (Processes.isPsbFlagsAbort(flags)) {
				short cond = Mem.readWord(c);
				if (Processes.isConditionAbortable(cond)) { Cpu.processTrap(); }
			}
			mon = Processes.setMonitorLocked(mon);
			Mem.writeWord(m, mon);
			Cpu.push(PrincOpsDefs.TRUE);
		} else {
			Processes.enterFailed(m);
		}
	};
	
	/*
	 * 10.2.5 Notify and Broadcast
	 */
	
	// NC - Notify Condition
	public static final OpImpl ESC_x04_NC = () -> {
		int c = Cpu.popLong();
		Cpu.checkEmptyStack();
		
		Processes.cleanupCondition(c);
		short cond = Mem.readWord(c);
		if (Processes.getCondition_tail(cond) != Processes.PsbNull) {
			Processes.wakeHead(c);
			Processes.reschedule(false);
		}
	};
	
	// BC - Broadcast Condition
	public static final OpImpl ESC_x05_BC = () -> {
		boolean requeue = false;
		int c = Cpu.popLong();
		Cpu.checkEmptyStack();
		
		Processes.cleanupCondition(c);
		short cond = Mem.readWord(c);
		while (Processes.getCondition_tail(cond) != Processes.PsbNull) {
			Processes.wakeHead(c);
			requeue = true;
			cond = Mem.readWord(c);
		}
		if (requeue) {
			Processes.reschedule(false);
		}
	};
	
	/*
	 * 10.2.6 Requeue
	 */
	
	// REQ - Requeue
	public static final OpImpl ESC_x06_REQ = () -> {
		int psbHandle = Cpu.pop();
		int dstQue = Cpu.popLong();
		int srcQue = Cpu.popLong();
		Cpu.checkEmptyStack();
		Processes.requeue(srcQue, dstQue, Processes.psbIndex(psbHandle));
		Processes.reschedule(false);
	};
	
	/*
	 * 10.2.7 Set Process Priority
	 */
	
	// SPP - Set Process Priority
	public static final OpImpl ESC_x0F_SPP = () -> {
		int priority = Cpu.pop() & 0xFFFF;
		Cpu.checkEmptyStack();
		
		short link = Processes.fetchPSB_link(Cpu.PSB);
		link = Processes.setPsbLink_priority(link, priority);
		Processes.storePSB_link(Cpu.PSB, link);
		Processes.requeue(Processes.PDA_LP_header_ready, Processes.PDA_LP_header_ready, Cpu.PSB);
		Processes.reschedule(false);
	};
	
	/*
	 * 10.4.4.3 Disabling Interrupts
	 */
	
	// DI - Disable Interrupts
	public static final OpImpl ESC_x10_DI = () -> {
		if (Cpu.WDC == PrincOpsDefs.WdcMax) {
			Cpu.interruptError();
		}
		Processes.disableInterrupts();
	};
	
	// EI - Enable Interrupts
	public static final OpImpl ESC_x11_EI = () -> {
		if (Cpu.WDC == 0) {
			Cpu.interruptError();
		}
		Processes.enableInterrupts();
	};
	
}
