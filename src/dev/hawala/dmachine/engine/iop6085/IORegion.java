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

import java.util.ArrayList;
import java.util.List;

import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PrincOpsDefs;

/**
 * Infrastructure for locations in the Input/Output Region (IOR)
 * and related structures of a 6085 machine.
 * <p>
 * The components in this class allow to describe the data structures in the IORegion
 * and give names to the structures and fields in these structures. These names will be used
 * when dumping the whole structures or logging memory reads/writes to the fields.
 * <br>
 * There are 2 kinds of structures: fix-positioned structures at an address allocated once for
 * the static memory areas of devices (for function or device control blocks) and relocatable
 * structures mapped to a variable location allocated by the face-client or the head for a single
 * i/o operation (e.g. input/output control blocks, buffers). 
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019)
 */
public abstract class IORegion extends Mem /* inherit from Mem for accessing the real memory word array ! */ {
	
	private static final int FIRST_IOR_PAGE = 32; // start page for the IO region
	private static final int MAX_IOR_PAGES = 32; // => 16 KByte ~ enough space??
	private static final int SEGMENT_GRANULARITY_WORDS = 8; // words per segment unit ~ 16 bytes
	
	// first real (word) address in the IO region
	public static final int IOR_BASE = FIRST_IOR_PAGE * PrincOpsDefs.WORDS_PER_PAGE;
	
	// real address-offset to IOR_BASE for the next free location in the IO region
	private static int currIorMesaAddressOffset = 0;
	
	// identified locations allocated in the IO region
	private static final IORAddress[] iorAddresses  = new IORAddress[ MAX_IOR_PAGES * PrincOpsDefs.WORDS_PER_PAGE ];
	
	/**
	 * dumpable IO region locations
	 */
	public interface IORDumpable {
		void dump(String prefix); // to stdout...
	}
	
	/**
	 * Location in IO region
	 */
	public interface IORAddress extends IORDumpable {
		
		/**
		 * @return name of the address in IO region
		 */
		String getName();
		
		/**
		 * @return real address in the IO region
		 */
		int getRealAddress();
		
		default short getIOPSegment() {
			return (short)((this.getRealAddress() / SEGMENT_GRANULARITY_WORDS) & 0xFFFF);
		}
		
		default short getIOPSegmentOffset() {
			return (short)(this.getRealAddress() % SEGMENT_GRANULARITY_WORDS);
		}
		
		@Override
		default void dump(String prefix) {
			System.out.println("-- dump of IORAddress not supported --");
		}
		
		default int getWordLength() {
			return 1;
		}
		
		default List<Field> getFields() {
			return null;
		}
		
	}
	
	/**
	 * 16-bit word location in IO region
	 */
	public interface Word extends IORAddress {
		
		/**
		 * @return 16-bit value at this location
		 */
		short get();
		
		/**
		 * Set 16-bit value at this location
		 * @param value
		 */
		void set(short value);
		
		void addField(Field f);
		
	}
	
	/**
	 * 32-bit value (double word) location in IO region
	 */
	public interface DblWord extends IORAddress {
		
		/**
		 * @return 32-bit value at this location
		 */
		int get();
		
		/**
		 * Set 32-bit value at this location
		 * @param value
		 */
		void set(int value);
		
		@Override
		default int getWordLength() {
			return 2;
		}
	}
	
	/**
	 * Subfield of a 16 bit word location in the IO region with numeric semantics.
	 */
	public interface Field extends DblWord { }
	
	/**
	 * Subfield of a 16 bit word location in the IO region with boolean semantics.
	 */
	public interface BoolField extends Field {
		default boolean is() { return (this.get() != 0); }
		default void set(boolean b) { this.set((b) ? 1 : 0); }
	}
	
	/**
	 * 16 bit IOP boolean
	 */
	public interface IOPBoolean extends IORAddress {
		boolean get();
		void set(boolean value);
	}
	
	/**
	 * Get the location element for a location in the IO region.
	 * 
	 * @param address the real address to be resolved.
	 * @return the location element at this address or a dummy location if
	 * 		the location is outside the IO region or not defined.
	 */
	public static IORAddress resolveRealAddress(int address) {
		int a = address - IOR_BASE;
		if (a < 0 || a >= iorAddresses.length) {
			return new IORBase("OUTSIDE-IOR-REGION", new IOLocation(a));
		}
		IORAddress iorLocation = iorAddresses[a];
		if (iorLocation == null) {
			iorLocation = new IORBase("UNDEFINED-IOR-REGION", new IOLocation(a));
			iorAddresses[a] = iorLocation;
		}
		return iorLocation;
	}
	
	public static void dumpIORegionStructure(int iorVMBaseAddress) {
		System.out.printf("## Begin IORegion\n");
		for (int a = 0; a < currIorMesaAddressOffset; a++) {
			IORAddress iorLocation = iorAddresses[a];
			if (iorLocation != null) {
				int expAddr = IOR_BASE + a;
				int locAddr = iorLocation.getRealAddress();
				if (expAddr != locAddr) {
					System.out.printf("## expAddr[0x%06X] != locAddress[0x%06X] [ virtual: 0x%06X ] => 0x%04X -- %s\n",
							expAddr, locAddr, iorVMBaseAddress + a, mem[locAddr], iorLocation.getName());
				} else {
					System.out.printf(
							"real: 0x%06X [ virtual: 0x%06X ] => 0x%04X -- %s\n",
							locAddr, iorVMBaseAddress + a, mem[locAddr], iorLocation.getName());
				}
			}
		}
		System.out.printf("## End IORegion\n");
	}
	
	/**
	 * Reserve the next word location for the given handler and location names.
	 * @param handlerName the IO handler reserving the location
	 * @param locationName the name of the location
	 * @return the location object
	 */
	protected static Word mkWord(String handlerName, String locationName) {
		return innerMkWord(handlerName, locationName, false);
	}
	
	/**
	 * Reserve the next word location for byte-swapped access for the given handler and location names.
	 * @param handlerName the IO handler reserving the location
	 * @param locationName the name of the location
	 * @return the location object
	 */
	protected static Word mkByteSwappedWord(String handlerName, String locationName) {
		return innerMkWord(handlerName, locationName, true);
	}
	
	/**
	 * Get a swapped-byte accessor for a word location.
	 * @param w the base location
	 * @return an accessor wrapping {@code w} for getting/setting the swapped value
	 */
	protected static Word mkSwapped(Word w) {
		return new SwappedWord(w);
	}
	
	/**
	 * Reserve the next word location for byte-swapped for the given handler and location names.
	 * @param handlerName the IO handler reserving the location
	 * @param locationName the name of the location
	 * @return the location object
	 */
	protected static DblWord mkDblWord(String handlerName, String locationName) {
		return innerMkDblWord(handlerName, locationName, false);
	}
	
	/**
	 * Reserve the next double word location for byte-swapped access for the given handler and location names.
	 * @param handlerName the IO handler reserving the location
	 * @param locationName the name of the location
	 * @return the location object
	 */
	protected static DblWord mkByteSwappedDblWord(String handlerName, String locationName) {
		return innerMkDblWord(handlerName, locationName, true);
	}
	
	/**
	 * Define a numeric (sub-)field inside a word location defined by the field bits in the word.
	 * @param fieldName the name of the field
	 * @param w the word location containing the field
	 * @param bits the (consecutive) bits containing the subfield as bit mask
	 * @return the location accessor for getting and setting the field value
	 */
	protected static Field mkField(String fieldName, Word w, int bits) {
		return new IORField(fieldName, w, bits, false);
	}
	
	/**
	 * Define a boolean (sub-)field inside a word location defined by the field bits in the word.
	 * @param fieldName the name of the field
	 * @param w the word location containing the field
	 * @param bits the (consecutive) bits containing the subfield as bit mask
	 * @return the location accessor for getting and setting the field value
	 */
	protected static BoolField mkBoolField(String fieldName, Word w, int bits) {
		return new IORField(fieldName, w, bits, true);
	}
	
	/**
	 * Define a double-word based on 2 single word, which should be consecutive in memory.
	 * @param w0 the least-significant (lower) word for the double-word
	 * @param w1 the most-significant (upper) word for the double-word
	 * @return a compound field mapping the 2 single words from/to a double-word
	 */
	protected static DblWord mkCompoundDblWord(Word w0, Word w1) {
		return new CompoundDblWord(w0, w1);
	}
	
	/**
	 * Reserve the next word location for a 16 boolean for the given handler and location names.
	 * @param handlerName the IO handler reserving the location
	 * @param locationName the name of the location
	 * @return the location object
	 */
	protected static IOPBoolean mkIOPBoolean(String handlerName, String locationName) {
		if (currIorMesaAddressOffset >= iorAddresses.length) {
			throw new IllegalArgumentException("No more space in IO-Region");
		}
		iIOLocation location = new IOLocation(currIorMesaAddressOffset);
		IOPBoolean b = new WordBoolean(handlerName + ":" + locationName, location);
		iorAddresses[currIorMesaAddressOffset] = b;
		currIorMesaAddressOffset += 1;
		return b;
	}
	
	/**
	 * Define a boolean subfield on the upper or lower byte of a word location.
	 *@param fieldName the name of the field
	 * @param w the word location containing the field
	 * @param hiByte {@code true} if the boolean is located in the upper byte of the word, else {@code false}
	 * @return the accessor object for the boolean byte
	 */
	protected static IOPBoolean mkIOPShortBoolean(String fieldName, Word w, boolean hiByte) {
		return new ByteBoolean(fieldName, w, hiByte);
	}
	
	/**
	 * Move the address of the next free location to the next segment boundary (16 bytes or 8 word granularity)
	 * <p>
	 * This method must be called when starting a new independently addressable data structure (FCB, DCB, ...).
	 * </p>
	 * @return the current real memory offset to IOR_BASE that will be used for the next allocated IORegion structure 
	 */
	protected static int syncToSegment() {
		int segmentLimitOffset = currIorMesaAddressOffset % SEGMENT_GRANULARITY_WORDS;
		if (segmentLimitOffset != 0) {
			currIorMesaAddressOffset += SEGMENT_GRANULARITY_WORDS - segmentLimitOffset;
		}
		return currIorMesaAddressOffset;
	}
	
	/**
	 * Swap the bytes of a word.
	 * @param v input word
	 * @return word with swapped upper and lower bytes
	 */
	public static int byteSwap(short v) {
		return ((v >> 8) & 0xFF) | ((v & 0xFF) << 8);
	}
	
	private static void dmp(String pattern, Object... args) {
		System.out.printf(pattern, args);
	}
	
	private static IOStruct lastIoStruct = null;
	
	public static IORAddress resolveLastKnownStructure(int realAddr) {
		if (lastIoStruct == null) { return null; }
		return lastIoStruct.resolveRealAddress(realAddr);
	}
	
	/**
	 * Base class for IO record structures and substructures embedded in records.
	 * Instances of subclasses describe a contiguous memory area in the IORegion,
	 * either at a fixed or relocatable position (relocating is optional).
	 * <p>
	 * (the {@code mk*}-methods mirror the static top level methods for creating
	 * locations, but here inside the relocatable IO record instead of the next free
	 * absolute position in the IORegion)
	 * </p>
	 */
	public static abstract class IOStruct implements IORDumpable {
		private final IOStruct embeddingParent;
		private final iIOLocation baseLocation;
		private final String structName;
		private int currOffset = 0;
		
		private List<IORDumpable> children = new ArrayList<>();
		private int realFirstAddr = -1;
		private int realLastAddr = -1;
		
		/**
		 * Constructor for top-level records.
		 * 
		 * @param base initial address of the structure (can be rebased later)
		 * @param name the name of the record (for logging)
		 */
		protected IOStruct(int base, String name) {
			this.baseLocation = new IOBaseLocation(base);
			this.structName = name;
			this.embeddingParent = null;
		}
		
		/**
		 * Constructor for embedded substructures.
		 * 
		 * @param embeddingParent record which this sub-record is part of
		 * @param name the name of the record (for logging)
		 */
		protected IOStruct(IOStruct embeddingParent, String name) {
			this.baseLocation = (embeddingParent == null) ? new IOLocation(currIorMesaAddressOffset) : embeddingParent.getBaseLocation(this);
			this.structName = (embeddingParent == null) ? name : embeddingParent.structName + "." + name;
			this.embeddingParent = embeddingParent;
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s>> %s [ at real address: 0x%06X ]\n", prefix, this.structName, this.getRealAddress());
			
			String childPrefix = prefix + "   ";
			for (IORDumpable child : this.children) {
				child.dump(childPrefix);
			}
			
			dmp("%s<< %s\n", prefix, structName);
		}
		
		private iIOLocation getBaseLocation(IORDumpable child) {
			this.children.add(child);
			return new IOLocation(this.baseLocation, this.currOffset);
		}
		
		protected int getRealAddress() {
			return this.baseLocation.realAddress();
		}
		
		/** 
		 * Close sub-structure, <b>must</b> be called at end of the structure definition
		 * to ensure that the offsets of items following to this substructure in the parent
		 * have correct offsets.
		 */
		protected void endStruct() {
			if (this.embeddingParent != null) {
				this.embeddingParent.addStructLength(this.currOffset);
			}
		}
		
		private void addStructLength(int structLength) {
			this.currOffset += structLength;
		}
		
		public void rebaseToVirtualAddress(int newBase) {
			if (this.baseLocation instanceof IOBaseLocation) {
				// System.out.printf("## relocating %s to virtual address 0x%06X\n", this.structName, newBase);
				this.rebaseToRealAddress(Mem.getRealAddress(newBase, false)); // TODO: better writable: true ??
			} else {
				System.out.println("** Warning: NOT relocating absolute positioned IOStruct");
			}
		}
		
		public void rebaseToRealAddress(int newBase) {
			if (this.baseLocation instanceof IOBaseLocation) {
				//System.out.printf("## relocating %s to real address 0x%06X\n", this.structName, newBase);
				
				((IOBaseLocation)this.baseLocation).rebase(newBase);
				if (this.structName.startsWith("Floppy") ) {
					lastIoStruct = this;
				}
				this.realFirstAddr = newBase;
				this.realLastAddr = newBase + currOffset - 1;
//				for (IORDumpable c : this.children) {
//					if (c instanceof IORAddress) {
//						IORAddress iora = (IORAddress)c;
//						if (this.realFirstAddr == -1) {
//							this.realFirstAddr = iora.getRealAddress();
//						}
//						this.realLastAddr = iora.getRealAddress();
//					}
//				}
				
//				dmp("## after relocation: realFirstAddr = 0x%06X , realLastAddr = 0x%06X\n", this.realFirstAddr, this.realLastAddr);
//				dmp("## begin relocated IOStruct %s\n", this.structName);
//				this.dump("");
//				dmp("## end relocated IOStruct\n");
			} else {
				lastIoStruct = null;
				System.out.println("** Warning: NOT relocating absolute positioned IOStruct");
			}
		}
		
		private IORAddress resolveRealAddress(int addr) {
			return this.resolveRealAddress(addr, true);
		}
		
		private IORAddress resolveRealAddress(int addr, boolean checkRange) {
			if (checkRange && (addr < this.realFirstAddr || addr > this.realLastAddr)) { return null; }
			//dmp("IOStruct.resolveRealAddress(0x%06X)...\n", addr);
			for (IORDumpable c : this.children) {
				if (c instanceof IORAddress) {
					IORAddress iora = (IORAddress)c;
					int ioraAddr = iora.getRealAddress();
					if (ioraAddr >= addr && ioraAddr < (ioraAddr + iora.getWordLength())) {
						return iora;
					}
				} else if (c instanceof IOStruct) {
					IOStruct ios = (IOStruct)c;
					IORAddress iora = ios.resolveRealAddress(addr, false);
					if (iora != null) {
						return iora;
					}
				}
			}
			return null;
		}
		
		protected Word mkWord(String memberName) {
			Word w = new IORWord(
							this.structName + "." + memberName,
							new IOLocation(this.baseLocation, this.currOffset));
			this.children.add(w);
			this.currOffset++;
			return w;
		}
		
		protected Word mkByteSwappedWord(String memberName) {
			Word w = new IORByteSwappedWord(
							this.structName + "." + memberName,
							new IOLocation(this.baseLocation, this.currOffset));
			this.children.add(w);
			this.currOffset++;
			return w;
		}
		
		protected DblWord mkDblWord(String memberName) {
			DblWord w = new IORDblWord(
								this.structName + "." + memberName,
								new IOLocation(this.baseLocation, this.currOffset));
			this.children.add(w);
			this.currOffset += 2;
			return w;
		}
		
		protected DblWord mkByteSwappedDblWord(String memberName) {
			DblWord w = new IORByteSwappedDblWord(
								this.structName + "." + memberName,
								new IOLocation(this.baseLocation, this.currOffset));
			this.children.add(w);
			this.currOffset += 2;
			return w;
		}
		
		protected Field mkField(String fieldName, Word w, int bits) {
			Field f = new IORField(fieldName, w, bits, false);
			this.children.add(f);
			return f;
		}
		
		protected BoolField mkBoolField(String fieldName, Word w, int bits) {
			BoolField f = new IORField(fieldName, w, bits, true);
			this.children.add(f);
			return f;
		}
		
		protected DblWord mkCompoundDblWord(Word w0, Word w1) {
			DblWord dw = new CompoundDblWord(w0, w1);
			this.children.add(dw);
			return dw;
		}
		
		protected IOPBoolean mkIOPBoolean(String memberName) {
			IOPBoolean b = new WordBoolean(
								this.structName + "." + memberName,
								new IOLocation(this.baseLocation, this.currOffset));
			this.children.add(b);
			this.currOffset++;
			return b;
		}
		
		protected IOPBoolean mkIOPShortBoolean(String fieldName, Word w, boolean hiByte) {
			IOPBoolean b = new ByteBoolean(fieldName, w, hiByte);
			this.children.add(b);
			return b;
		}
	}
	
	/*
	 * internal / implementation items
	 */
	
	private static Word innerMkWord(String handlerName, String locationName, boolean swapped) {
		if (currIorMesaAddressOffset >= iorAddresses.length) {
			throw new IllegalArgumentException("No more space in IO-Region");
		}
		iIOLocation location = new IOLocation(currIorMesaAddressOffset);
		Word a = swapped
			? new IORByteSwappedWord(handlerName + ":" + locationName, location)
			: new IORWord(handlerName + ":" + locationName, location);
		iorAddresses[currIorMesaAddressOffset] = a;
		currIorMesaAddressOffset += 1;
		return a;
	}
	
	private static DblWord innerMkDblWord(String handlerName, String locationName, boolean swapped) {
		if ((currIorMesaAddressOffset + 1) >= iorAddresses.length) {
			throw new IllegalArgumentException("No more space in IO-Region");
		}
		iIOLocation location = new IOLocation(currIorMesaAddressOffset);
		DblWord a = swapped
			? new IORDblWord(handlerName + ":" + locationName + "[low]", location)
			: new IORByteSwappedDblWord(handlerName + ":" + locationName + "[low]", location);
		IORBase h = new IORBase(handlerName + ":" + locationName + "[high]", new IOLocation(currIorMesaAddressOffset + 1));
		iorAddresses[currIorMesaAddressOffset] = a;
		iorAddresses[currIorMesaAddressOffset + 1] = h;
		currIorMesaAddressOffset += 2;
		return a;
	}
	
	private interface iIOLocation {
		int realAddress();
	}
	
	private static class IORBaseLocation implements iIOLocation {
		@Override public int realAddress() { return IOR_BASE; }
	}
	
	private static class IOLocation implements iIOLocation {
		private final iIOLocation base; 
		private final int offset;
		private IOLocation(int offset) {
			this.base = new IORBaseLocation();
			this.offset = offset;
		}
		private IOLocation(iIOLocation base, int offset) {
			this.base = base;
			this.offset = offset;
		}
		@Override public int realAddress() { return this.base.realAddress() + this.offset; }
	}
	
	private static class IOBaseLocation implements iIOLocation {
		private int baseLocation;
		private IOBaseLocation(int base) { this.baseLocation = base; }
		private void rebase(int newBase) { this.baseLocation = newBase; }
		@Override public int realAddress() { return this.baseLocation; }
	}
	
	private static class IORBase implements IORAddress {
		
		protected final String name;
		protected final iIOLocation location;
		
		private IORBase(String name, iIOLocation location) {
			this.name = name;
			this.location = location;;
		}
		
		@Override
		public String getName() { return this.name; }
		
		@Override
		public int getRealAddress() { return this.location.realAddress(); }
		
		@Override
		public void dump(String prefix) {
			dmp("%s[ at real address: 0x%06X ] : NO_VALUE : %s (IORBase)\n", prefix, this.getRealAddress(), this.name);
		}
		
	}

	private static class IORWord extends IORBase implements Word {
		
		private List<Field> fields = null;
		
		private IORWord(String name, iIOLocation location) {
			super(name, location);
		}
		
		@Override
		public short get() {
			return mem[this.location.realAddress()];
		}
		
		@Override
		public void set(short value) {
			mem[this.location.realAddress()] = value;
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s[ at real address: 0x%06X ] : 0x%04X : %s (IORWord)\n", prefix, this.getRealAddress(), this.get(), this.name);
		}
		
		
		@Override
		public void addField(Field f) {
			if (this.fields == null) { this.fields = new ArrayList<>(); }
			this.fields.add(f);
		}
		
		@Override
		public List<Field> getFields() {
			return this.fields;
		}
	}

	private static class IORByteSwappedWord extends IORWord {
		
		private IORByteSwappedWord(String name, iIOLocation location) {
			super(name, location);
		}
		
		@Override
		public short get() {
			return (short)byteSwap(mem[this.location.realAddress()]);
		}
		
		@Override
		public void set(short value) {
			mem[this.location.realAddress()] = (short)byteSwap(value);
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s[ at real address: 0x%06X ] : 0x%04X : %s (IORByteSwappedWord)\n", prefix, this.getRealAddress(), this.get(), this.name);
		}
		
	}
	
	private static class IORDblWord extends IORBase implements DblWord {
		
		private IORDblWord(String name, iIOLocation location) {
			super(name, location);
		}
		
		@Override
		public int get() {
			short low = mem[this.location.realAddress()];
			short high = mem[this.location.realAddress() + 1];
			return (high << 16) | (low & 0x0000FFFF);
		}
		
		@Override
		public void set(int value) {
			mem[this.location.realAddress()] = (short)(value & 0xFFFF);
			mem[this.location.realAddress() + 1] = (short)(value >>> 16);
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s[ at real address: 0x%06X ] : 0x%08X : %s (IORDblWord)\n", prefix, this.getRealAddress(), this.get(), this.name);
		}
		
	}
	
	private static class IORByteSwappedDblWord extends IORBase implements DblWord {
		
		private IORByteSwappedDblWord(String name, iIOLocation location) {
			super(name, location);
		}
		
		@Override
		public int get() {
			int low = byteSwap(mem[this.location.realAddress()]);
			int high = byteSwap(mem[this.location.realAddress() + 1]);
			return (high << 16) | (low & 0x0000FFFF);
		}
		
		@Override
		public void set(int value) {
			mem[this.location.realAddress()] = (short)byteSwap((short)(value & 0xFFFF));
			mem[this.location.realAddress() + 1] = (short)byteSwap((short)(value >>> 16));
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s[ at real address: 0x%06X ] : 0x%08X : %s (IORByteSwappedDblWord)\n", prefix, this.getRealAddress(), this.get(), this.name);
		}
		
	}
	
	private static class IORField implements BoolField {
		
		private final Word base;
		
		private final int shiftBy;
		private final int bits;
		private final int mask;
		
		private final boolean isBool;
		
		private final String name;
		
		public IORField(String fieldName, Word w, int bits, boolean isBool) {
			int tmpShift = 0;
			int tmpBits = bits & 0xFFFF;
			for (int i = 0; i < 16; i++) {
				if ((tmpBits & 0x0001) != 0) { break; }
				tmpShift++;
				tmpBits = tmpBits >>> 1;
			}
			
			this.base = w;
			this.shiftBy = tmpShift;
			this.bits = bits & 0xFFFF;
			this.mask = this.bits ^ 0xFFFF;
			this.name = String.format("%s[bits:0x%04X]:%s", w.getName(), bits, fieldName);
			this.isBool = isBool;
			
			w.addField(this);
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public int getRealAddress() {
			return this.base.getRealAddress();
		}

		@Override
		public int get() {
			int val = (this.base.get() & this.bits) >>> this.shiftBy;
			return val;
		}

		@Override
		public void set(int value) {
			int val = (value << this.shiftBy) & this.bits;
			int rest = this.base.get() & this.mask;
			int newVal = val | rest;
			this.base.set((short)(newVal & 0xFFFF));
		}
		
		@Override
		public void dump(String prefix) {
			if (this.isBool) {
				dmp("%s--[ at real address: 0x%06X ] : %s : %s (BoolField)\n", prefix, this.getRealAddress(), Boolean.toString(this.is()), this.name);
			} else {
				dmp("%s--[ at real address: 0x%06X ] : 0x%04X : %s (IORField)\n", prefix, this.getRealAddress(), this.get(), this.name);
			}
		}
		
	}
	
	private static class SwappedWord implements Word {
		
		private final Word base;
		
		private SwappedWord(Word w) {
			this.base = w;
		}

		@Override
		public String getName() {
			return this.base.getName();
		}

		@Override
		public int getRealAddress() {
			return this.base.getRealAddress();
		}

		@Override
		public short get() {
			return (short)byteSwap(this.base.get());
		}

		@Override
		public void set(short value) {
			this.base.set((short)byteSwap(value));
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s--[ at real address: 0x%06X ] : 0x%04X : %s (SwappedWord)\n", prefix, this.getRealAddress(), this.get(), this.getName());
		}

		@Override
		public void addField(Field f) { }
		
	}
	
	private static class CompoundDblWord implements DblWord {
		
		private final String name;
		private final Word w0;
		private final Word w1;
		
		public CompoundDblWord(Word w0, Word w1) {
			this.name = w0.getName() + "[asDblWord]";
			this.w0 = w0;
			this.w1 = w1;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public int getRealAddress() {
			return this.w0.getRealAddress();
		}

		@Override
		public int get() {
			int res = (this.w1.get() << 16) | (this.w0.get() & 0xFFFF);
			return res;
		}

		@Override
		public void set(int value) {
			this.w0.set((short)(value & 0xFFFF));
			this.w1.set((short)((value >>> 16) & 0xFFFF));
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s>>>>[ at real address: 0x%06X ] : 0x%08X : %s (CompoundDblWord)\n", prefix, this.getRealAddress(), this.get(), this.getName());
		}
		
	}
	
	private static class WordBoolean extends IORBase implements IOPBoolean {
		private WordBoolean(String name, iIOLocation location) {
			super(name, location);
		}

		@Override
		public boolean get() {
			return (mem[this.location.realAddress()] != 0);
		}

		@Override
		public void set(boolean value) {
			mem[this.location.realAddress()] = (value) ? (short)0xFFFF : 0;
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s[ at real address: 0x%06X ] : %s : %s (WordBoolean)\n", prefix, this.getRealAddress(), Boolean.toString(this.get()), this.getName());
		}	
	}
	
	private static class ByteBoolean implements IOPBoolean {
		private final Word base;
		
		private final boolean hiByte;
		private final int getMask;
		private final int putMaskRest;
		
		private final String name;
		
		public ByteBoolean(String fieldName, Word w, boolean hiByte) {
			this.base = w;
			this.hiByte = hiByte;
			this.getMask = (hiByte) ? 0xFF00 : 0x00FF;
			this.putMaskRest = (hiByte) ? 0x00FF : 0xFF00;
			this.name = String.format("%s-- [boolean,%sByte]:%s", w.getName(), (hiByte) ? "upper" : "lower", fieldName);
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public int getRealAddress() {
			return this.base.getRealAddress();
		}

		@Override
		public boolean get() {
			int val = this.base.get() & this.getMask;
			return (val != 0);
		}

		@Override
		public void set(boolean value) {
			int bits = (value) ? this.getMask : 0;
			this.base.set((short)((this.base.get() & this.putMaskRest) | bits));
		}
		
		@Override
		public void dump(String prefix) {
			dmp("%s--[ at real address: 0x%06X ] : %s : %s (ByteBoolean,%s)\n", prefix, this.getRealAddress(), Boolean.toString(this.get()), this.getName(), this.hiByte ? "hiByte" : "loByte");
		}
	}
	
}