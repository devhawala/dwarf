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

import dev.hawala.dmachine.engine.PrincOpsDefs;

/**
 * Definition of common data structures allocated statically in the IO region
 * for FCBs, DCBs etc. or dynamically for IOCBs, as well a some constants. 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public abstract class IOPTypes extends IORegion {
	
	/*
	 * handler IDs
	 */
	
	public static final int HandlerID_beep = 1;
	public static final int HandlerID_disk = 2;
	public static final int HandlerID_display = 3;
	public static final int HandlerID_ethernet = 4;
	public static final int HandlerID_floppy = 5;
	public static final int HandlerID_keyboardAndMouse = 6;
	public static final int HandlerID_maintPanel = 7;
	
	public static final int HandlerID_processor = 16;
	
	public static final int HandlerID_tty = 17;
	public static final int HandlerID_rs232c = 18;
	
	public static final int HandlerID_parallelPort = 97;
	
	public static final int HandlerID_last = 127;
	
	/*
	 * technical intel structures
	 */
	
	public static class IPCS {
		public final Word ip;
		public final Word cs;
		
		public IPCS(String name, String locationName) {
			this.ip = mkByteSwappedWord(name, locationName + ".ip");
			this.cs = mkByteSwappedWord(name, locationName + ".cs");
		}
	}
	
	public static class SegmentRec {
		public final Word ioRegionSegment;
		public final Word stackSegment;
		
		public SegmentRec(String name, String locationName) {
			this.ioRegionSegment = mkByteSwappedWord(name, locationName + ".ioRegionSegment");
			this.stackSegment = mkByteSwappedWord(name, locationName + ".stackSegment");
		}
	}
	
	public static class SPSS {
		public final Word sp;
		public final Word ss;
		
		public SPSS(String name, String locationName) {
			this.sp = mkByteSwappedWord(name, locationName + ".sp");
			this.ss = mkByteSwappedWord(name, locationName + ".ss");
		}
	}
	
	/*
	 * IOP structures
	 */

	public static class AlternateOpieAddress {
		public final Word low;
		public final Word high;
		
		public AlternateOpieAddress(String name, String locationName) {
			this.low = mkByteSwappedWord(name, locationName + ".low");
			this.high = mkByteSwappedWord(name, locationName + ".high");
		}
	}
	
	public static class ByteSwappedLinkPtr extends IOStruct {
		private final Word linkPtr;
		/**/public final Field nonNilPtr;
		/**/public final Field pointer;
		/**/public final Field pointerByte;
		
		public ByteSwappedLinkPtr(String name, String locationName) {
			super(null, name);
			this.linkPtr = IORegion.mkByteSwappedWord(name, locationName);
			/**/this.nonNilPtr = mkField("nonNilPtr", this.linkPtr, 0x8000);
			/**/this.pointer = mkField("pointer", this.linkPtr, 0x7FFE);
			/**/this.pointerByte = mkField("pointerByte", this.linkPtr, 0x0001);
		}
		
		public ByteSwappedLinkPtr(IOStruct embeddingParent, String locationName) {
			super(embeddingParent, locationName);

			this.linkPtr = mkByteSwappedWord(locationName);
			/**/this.nonNilPtr = mkField("nonNilPtr", this.linkPtr, 0x8000);
			/**/this.pointer = mkField("pointer", this.linkPtr, 0x7FFE);
			/**/this.pointerByte = mkField("pointerByte", this.linkPtr, 0x0001);
			
			this.endStruct();
		}
	}
	
	public static class ByteSwappedPointer extends IOStruct {
		public final Word ptr;
		/**/public final Field pointer;
		/**/public final Field pointerByte;
		
		public ByteSwappedPointer(String name, String locationName) {
			super(null, name);
			this.ptr = IORegion.mkByteSwappedWord(name, locationName);
			/**/this.pointer = mkField("pointer", this.ptr, 0xFFFE);
			/**/this.pointerByte = mkField("pointerByte", this.ptr, 0x0001);
		}
		
		public ByteSwappedPointer(IOStruct embeddingParent, String locationName) {
			super(embeddingParent, locationName);
			
			this.ptr = mkByteSwappedWord("ptr");
			/**/this.pointer = mkField("pointer", this.ptr, 0xFFFE);
			/**/this.pointerByte = mkField("pointerByte", this.ptr, 0x0001);
			
			this.endStruct();
		}
	}
	
	public static class ClientCondition extends IOStruct {
		public final Word handlerIDAndConditionRelMaskPtr;
		/**/public final Field handlerID;
		/**/public final Field conditionRelMaskPtr_maskWordOffset;
		/**/public final Field conditionRelMaskPtr_maskPtrByte;
		public final ByteSwappedLinkPtr conditionPtr;
		public final Word maskValue;
		
		
		public ClientCondition(String name, String locationName) {
			super(null, name);
			
			this.handlerIDAndConditionRelMaskPtr = IORegion.mkWord(name, locationName + ".handlerIDAndConditionRelMaskPtr");
			/**/this.handlerID = mkField("handlerID", this.handlerIDAndConditionRelMaskPtr, 0xFF00);
			/**/this.conditionRelMaskPtr_maskWordOffset = mkField("conditionRelMaskPtr.maskWordOffset", this.handlerIDAndConditionRelMaskPtr, 0x00FE);
			/**/this.conditionRelMaskPtr_maskPtrByte = mkField("conditionRelMaskPtr.maskPtrByte", this.handlerIDAndConditionRelMaskPtr, 0x0001);
			this.conditionPtr = new ByteSwappedLinkPtr(name, locationName + ".conditionPtr");
			this.maskValue = IORegion.mkWord(name, locationName + ".maskValue");
		}
		
		public ClientCondition(IOStruct embeddingParent, String locationName) {
			super(embeddingParent, locationName);
		
			this.handlerIDAndConditionRelMaskPtr = mkWord("handlerIDAndConditionRelMaskPtr");
			/**/this.handlerID = mkField("handlerID", this.handlerIDAndConditionRelMaskPtr, 0xFF00);
			/**/this.conditionRelMaskPtr_maskWordOffset = mkField("conditionRelMaskPtr.maskWordOffset", this.handlerIDAndConditionRelMaskPtr, 0x00FE);
			/**/this.conditionRelMaskPtr_maskPtrByte = mkField("conditionRelMaskPtr.maskPtrByte", this.handlerIDAndConditionRelMaskPtr, 0x0001);
			this.conditionPtr = new ByteSwappedLinkPtr(this, "conditionPtr");
			this.maskValue = mkWord("maskValue");
			
			this.endStruct();
		}
	}
	
	public static class IOPCondition {
		public final ByteSwappedLinkPtr tcbLinkPtr;
		
		public IOPCondition(String name, String locationName) {
			this.tcbLinkPtr = new ByteSwappedLinkPtr(name, locationName + ".tcbLinkPtr");
		}
	}
	
	public static class NotifyMask {
		public final Word byteMaskAndOffset;
		/**/public final Field byteMask;
		/**/public final Field byteOffset;
		
		public NotifyMask(String name, String locationName) {
			this.byteMaskAndOffset = mkWord(name, locationName + ".byteMaskAndOffset");
			/**/this.byteMask = mkField("byteMask", this.byteMaskAndOffset, 0xFF00);
			/**/this.byteOffset = mkField("byteOffset", this.byteMaskAndOffset, 0x00FF);
		}
	}
	
	public enum OpieAddressType {
	    nil(0),
	    extendedBus(020),
	    extendedBusPage(060),
	    iopLogical(0120),
	    iopIORegionRelative(0121),
	    pcLogical(0220),
	    virtualWord(0340),
	    virtualFirst64KRelative(0341),
	    virtualPage(0360),
	    last(0377);
	
	    public final int code;
		private OpieAddressType(int c) { this.code = c; }
	}
	
	public static class OpieAddress extends IOStruct {
		public final ByteSwappedPointer a15ToA0;
		private final Word a23ToA16AndType;
		/**/public final Field a23ToA16;
		/**/public final Field type;
		
		public OpieAddress(String name, String locationName) {
			super(null, name);
			this.a15ToA0 = new ByteSwappedPointer(name, locationName + ".a15ToA0");
			this.a23ToA16AndType = IORegion.mkWord(name, locationName + ".a23ToA16AndType");
			/**/this.a23ToA16 = mkField("a23ToA16", this.a23ToA16AndType, 0xFF00);
			/**/this.type = mkField("type", this.a23ToA16AndType, 0x00FF);
		}
		
		public OpieAddress(IOStruct embeddingParent, String locationName) {
			super(embeddingParent, locationName);
			
			this.a15ToA0 = new ByteSwappedPointer(this, "a15ToA0");
			this.a23ToA16AndType = mkWord("a23ToA16AndType");
			/**/this.a23ToA16 = mkField("a23ToA16", this.a23ToA16AndType, 0xFF00);
			/**/this.type = mkField("type", this.a23ToA16AndType, 0x00FF);
			
			this.endStruct();
		}
		
		public void fromLP(int lp) {
			this.a15ToA0.ptr.set((short)(lp & 0xFFFF));
			if ((lp & 0xFFFF0000) == 0) {
				this.a23ToA16.set(0);
				this.type.set(OpieAddressType.virtualFirst64KRelative.code);
			} else {
				this.a23ToA16.set((short)((lp & 0xFFFF0000) >>> 16));
				this.type.set(OpieAddressType.virtualWord.code);
			}
		}
		
		public int toLP() {
			int lp = this.a15ToA0.ptr.get() & 0xFFFF;
			if (this.type.get() == OpieAddressType.virtualFirst64KRelative.code) {
				return lp;
			}
			if (this.type.get() == OpieAddressType.virtualWord.code) {
				lp |= this.a23ToA16.get() << 16;
				return lp;
			}
			if (this.type.get() == OpieAddressType.virtualPage.code) {
				return (this.a15ToA0.ptr.get() & 0xFFFF) * PrincOpsDefs.WORDS_PER_PAGE;
			}
			return 0;
		}
	}
	
	public static class QueueBlock {
		public final OpieAddress queueHead;
		public final OpieAddress queueTail;
		public final OpieAddress queueNext;
		
		public QueueBlock(String name, String locationName) {
			this.queueHead = new OpieAddress(name, locationName + ".queueHead");
			this.queueTail = new OpieAddress(name, locationName + ".queueTail");
			this.queueNext = new OpieAddress(name, locationName + ".queueNext");
		}
	}
	
	public static class QueueEntry {
		private final Word queueTypeAndNextHandlerID;
		/**/public final Field queueType;
		/**/public final Field nextHandlerID;
		public final ByteSwappedLinkPtr nextTCBLinkPtr;
		
		public QueueEntry(String name, String locationName) {
			this.queueTypeAndNextHandlerID = mkWord(name, locationName + ".queueTypeAndNextHandlerID");
			/**/this.queueType = mkField("queueType", this.queueTypeAndNextHandlerID, 0xFF00);
			/**/this.nextHandlerID = mkField("nextHandlerID", this.queueTypeAndNextHandlerID, 0x00FF);
			this.nextTCBLinkPtr = new ByteSwappedLinkPtr(name, locationName + ".nextTCBLinkPtr");
		}
	}
	
	public static class TaskContextBlock {
		public final QueueEntry taskQueue;
		public final ByteSwappedPointer taskCondition;
		public final ByteSwappedPointer taskICPtr;
		public final Word taskSP;
		public final SPSS returnSPSS;
		private final Word prevAndPresentStateAndTaskHandlerID;
		/**/public final Field prevState;
		/**/public final Field presentState;
		/**/public final Field taskHandlerID;
		public final Word timerValue;
		
		public TaskContextBlock(String name, String locationName) {
			this.taskQueue = new QueueEntry(name, locationName + ".taskQueue");
			this.taskCondition = new ByteSwappedPointer(name, locationName + ".taskCondition");
			this.taskICPtr = new ByteSwappedPointer(name, locationName + ".taskICPtr");
			this.returnSPSS = new SPSS(name, locationName + ".returnSPSS");
			this.taskSP = mkByteSwappedWord(name, locationName + ".taskSP");
			this.prevAndPresentStateAndTaskHandlerID = mkWord(name, locationName + ".prevAndPresentStateAndTaskHandlerID");
			/**/this.prevState = mkField("prevState", this.prevAndPresentStateAndTaskHandlerID, 0xF000);
			/**/this.presentState = mkField("presentState", this.prevAndPresentStateAndTaskHandlerID, 0x0F00);
			/**/this.taskHandlerID = mkField("taskHandlerID", this.prevAndPresentStateAndTaskHandlerID, 0x00FF);
			this.timerValue = mkByteSwappedWord(name, locationName + ".timerValue");
		}
	}
	
	public static class IORTable {
		public final Word mesaHasLock;
		public final Word iopRequestsLock;
		public final SegmentRec[] segments = new SegmentRec[HandlerID_last + 1];
		
		public IORTable() {
			final String name = "IORTable";
			this.mesaHasLock = mkWord(name, "mesaHasLock");
			this.iopRequestsLock = mkWord(name, "iopRequestsLock");
			for (int i = 0; i < segments.length; i++) {
				this.segments[i] = new SegmentRec(name, "segments[" + i + "]");
			}
		}
	}
	
}