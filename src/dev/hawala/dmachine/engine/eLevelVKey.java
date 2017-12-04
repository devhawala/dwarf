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

/**
 * Enumeration of the keys on a (6085) Xerox keyboard, along
 * with the bit position in the FCB of the keyboard agent used
 * to inform the running program which key is pressed. 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public enum eLevelVKey {
	// fcb word[0]
	    knull            (0), // "null" is a reserved word in Java => "knull"
	    Bullet           (1),
	    SuperSub         (2),
	    Case             (3),
	    Strikeout        (4),
	    KeypadTwo        (5),
	    KeypadThree      (6),
	    SingleQuote      (7),
	    KeypadAdd        (8),
	    KeypadSubtract   (9),
	    KeypadMultiply   (10),
	    KeypadDivide     (11),
	    KeypadClear      (12),
	    Point            (13),  // left mouse button
	    Adjust           (14),  // right mouse button
	    Menu             (15),  // middle mouse button
	// fcb word[1]
	    Five             (16),
	    Four             (17),
	    Six              (18),
	    E                (19),
	    Seven            (20),
	    D                (21),
	    U                (22),
	    V                (23),
	    Zero             (24),
	    K                (25),
	    Dash             (26),
	    P                (27),
	    Slash            (28),
	    Font             (29),
	    Same             (30),
	    BS               (31),
	// fcb word[2]
	    Three            (32),
	    Two              (33),
	    W                (34),
	    Q                (35),
	    S                (36),
	    A                (37),
	    Nine             (38),
	    I                (39),
	    X                (40),
	    O                (41),
	    L                (42),
	    Comma            (43),
	    Quote            (44),
	    RightBracket     (45),
	    Open             (46),
	    Special          (47),
	// fcb word[3]
	    One              (48),
	    Tab              (49),
	    ParaTab          (50),
	    F                (51),
	    Props            (52),
	    C                (53),
	    J                (54),
	    B                (55),
	    Z                (56),
	    LeftShift        (57),
	    Period           (58),
	    SemiColon        (59),
	    NewPara          (60),
	    OpenQuote        (61),
	    Delete           (62),
	    Next             (63),
	// fcb word[4]
	    R                (64),
	    T                (65),
	    G                (66),
	    Y                (67),
	    H                (68),
	    Eight            (69),
	    N                (70),
	    M                (71),
	    Lock             (72),
	    Space            (73),
	    LeftBracket      (74),
	    Equal            (75),
	    RightShift       (76),
	    Stop             (77),
	    Move             (78),
	    Undo             (79),
	// fcb word[5]
	    Margins          (80),
	    KeypadSeven      (81),
	    KeypadEight      (82),
	    KeypadNine       (83),
	    KeypadFour       (84),
	    KeypadFive       (85),
	    English          (86),
	    KeypadSix        (87),
	    Katakana         (88),
	    Copy             (89),
	    Find             (90),
	    Again            (91),
	    Help             (92),
	    Expand           (93),
	    KeypadOne        (94),
	    DiagnosticBitTwo (95),
	// fcb word[6]
	    DiagnosticBitOne (96),
	    Center           (97),
	    KeypadZero       (98),
	    Bold             (99),
	    Italic           (100),
	    Underline        (101),
	    Superscript      (102),
	    Subscript        (103),
	    Smaller          (104),
	    KeypadPeriod     (105),
	    KeypadComma      (106),
	    LeftShiftAlt     (107),
	    DoubleQuote      (108),
	    Defaults         (109),
	    Hiragana         (110),
	    RightShiftAlt    (111);

	private final int idx;
	
	private final int word;
	
	private final short bit;
	
	private final short mask;
	
	private eLevelVKey(int i) {
		this.idx = i;
		this.word = i / PrincOpsDefs.WORD_BITS;
		this.bit = (short)(1 << (PrincOpsDefs.WORD_BITS - 1 - (i % PrincOpsDefs.WORD_BITS)));
		this.mask = (short)~this.bit;
	}
	
	public int getAbsoluteBit() { return this.idx; }
	
	public int getWord() { return this.word; }
	
	public short getBit() { return this.bit; }
	
	public short getMask() { return this.mask; }
	
	public void setPressed(short[] kbdWords) {
		kbdWords[this.word] &= this.mask;
	}
	
	public void setReleased(short[] kbdWords) {
		kbdWords[this.word] |= this.bit;
	}
}