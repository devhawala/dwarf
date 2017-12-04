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
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PrincOpsDefs;

/**
 * Agent for the display of a Dwarf machine.
 * <p>
 * Besides providing the basic information about the display
 * (size, color depth), this agent provides a set of operations,
 * among which only one is effectively implemented to allow setting
 * the mouse pointer shape. However, as setting is a 2-step operation
 * (set the bits and then move the pointer position to adjust for
 * the new hotspot position), the bitmap is only forwarded to the 
 * mouseAgent, which will callback the ui when the mouse position
 * arrives.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class DisplayAgent extends Agent {
	
	private final MouseAgent mouseAgent;

	/*
	 * DisplayFCBType
	 */
	private static final int fcb_w_command = 0;
	private static final int fcb_w_status = 1;
	private static final int fcb_dbl_displayMemoryAddress = 2; // ! page and not address, and: real page (not virtual page)
	private static final int fcb_w_color_u0 = 4; // nibbles: red, green 
	private static final int fcb_w_color_u1 = 5; // nibbles: blue, reserved
	private static final int fcb_w_colormapping0 = 6;
	private static final int fcb_w_colormapping1 = 7;
	private static final int fcb_w_destRectanle_x = 8;
	private static final int fcb_w_destRectanle_y = 9;
	private static final int fcb_w_destRectanle_width = 10;
	private static final int fcb_w_destRectanle_height = 11;
	private static final int fcb_w_sourceOrigin_x = 12;
	private static final int fcb_w_sourceOrigin_y = 13;
	private static final int fcb_w16_cursorPattern = 14;
	private static final int fcb_w4_pattern = 30;
	private static final int fcb_w_patternFillMode = 34;
	private static final int fcb_w_complemented = 35;
	private static final int fcb_w_colorIndex = 36;
	private static final int fcb_w_displayType = 37; // DisplayType ~ Mem.DisplayType.getBitDepth()
	private static final int fcb_w_displayWidth = 38;
	private static final int fcb_w_displayHeight = 39;
	private static final int FCB_SIZE = 40;
	
	// DisplayIOFaceGuam
	private static final int Command_nop                  = 0;
	private static final int Command_setCLTEntry          = 1;
	private static final int Command_getCLTEntry          = 2;
	private static final int Command_setBackground        = 3;
	private static final int Command_setCursorPattern     = 4;
	private static final int Command_updateRectangle      = 5;
	private static final int Command_copyRectangle        = 6;
	private static final int Command_patternFillRectangle = 7;
	
	//DisplayStatus
	private static final int Status_success                = 0;
	private static final int Status_generalFailure         = 1;
	private static final int Status_invalidCLTIndex        = 2;
	private static final int Status_readOnlyCLT            = 3;
	private static final int Status_invalidDestRectangle   = 4;
	private static final int Status_invalidSourceRectangle = 5;
	
	//PatternFillMode
	private static final int Pfm_copy = 0;
	private static final int Pfm_and  = 1;
	private static final int Pfm_or   = 2;
	private static final int Pfm_xor  = 3;
	
	public DisplayAgent(int fcbAddress, MouseAgent mouseAgent) {
		super(AgentDevice.displayAgent, fcbAddress, FCB_SIZE);
		this.mouseAgent = mouseAgent;
		
		this.enableLogging(Config.AGENTS_LOG_DISPLAY);
	}

	@Override
	public void call() {
		int command = this.getFcbWord(fcb_w_command);
		switch(command) {
		
		case Command_nop:
			logf("call() - nop\n");
			this.setFcbWord(fcb_w_status, Status_success);
			break;
			
		case Command_setCLTEntry:
			this.setCLTEntry();
			break;
			
		case Command_getCLTEntry:
			this.getCLTEntry();
			break;
			
		case Command_setBackground:
			this.setBackground();
			break;
			
		case Command_setCursorPattern:
			this.setCursorPattern();
			break;
			
		case Command_updateRectangle:
			this.updateRectangle();
			break;
			
		case Command_copyRectangle:
			this.copyRectangle();
			break;
			
		case Command_patternFillRectangle:
			this.patternFillRectangle();
			break;
			
		default:
			logf("call() - unknown command %d\n");
			this.setFcbWord(fcb_w_status, Status_generalFailure);
			break;
		}
	}
	
	private void setCLTEntry() {
		logf("call() - setCLTEntry => status = readOnlyCLT\n");
		this.setFcbWord(fcb_w_status, Status_readOnlyCLT);
	}
	
	private void getCLTEntry() {
		int colorIndex = this.getFcbWord(fcb_w_colorIndex);
		logf("call() - getCLTEntry , colorIndex = %d\n", colorIndex);
		if (colorIndex > 1) {
			this.setFcbWord(fcb_w_status, Status_readOnlyCLT);
			return;
		}
		this.setFcbWord(fcb_w_color_u0, 0xFFFF);
		this.setFcbWord(fcb_w_color_u1, 0xFF00);
		this.setFcbWord(fcb_w_status, Status_success);
	}
	
	private void setBackground() {
		boolean inverse = (this.getFcbWord(fcb_w_complemented) != 0);
		logf("call() - setBackground , inverse = %s\n", (inverse) ? "true" : "false");
		
		// TODO: implement somehow
		
		this.setFcbWord(fcb_w_status, Status_success);
	}
	
	private void setCursorPattern() {
		logf("call() - setCursorPattern\n");
		logf("    +---------------------------------+\n");
		short[] cursor = new short[16];
		for (int i = 0; i < cursor.length; i++) {
			short line = this.getFcbWord(fcb_w16_cursorPattern + i);
			cursor[i] = line;
			logf("    | %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s |\n",
				((line & 0x8000) != 0) ? "x" : " ",
				((line & 0x4000) != 0) ? "x" : " ",
				((line & 0x2000) != 0) ? "x" : " ",
				((line & 0x1000) != 0) ? "x" : " ",
				((line & 0x0800) != 0) ? "x" : " ",
				((line & 0x0400) != 0) ? "x" : " ",
				((line & 0x0200) != 0) ? "x" : " ",
				((line & 0x0100) != 0) ? "x" : " ",
				((line & 0x0080) != 0) ? "x" : " ",
				((line & 0x0040) != 0) ? "x" : " ",
				((line & 0x0020) != 0) ? "x" : " ",
				((line & 0x0010) != 0) ? "x" : " ",
				((line & 0x0008) != 0) ? "x" : " ",
				((line & 0x0004) != 0) ? "x" : " ",
				((line & 0x0002) != 0) ? "x" : " ",
				((line & 0x0001) != 0) ? "x" : " "
				);
		}
		logf("    +---------------------------------+\n");
		
		if (this.mouseAgent != null) {
			this.mouseAgent.setPointerBitmap(cursor);
		}
		
		this.setFcbWord(fcb_w_status, Status_success);
	}
	
	private void updateRectangle() {
		int x = this.getFcbWord(fcb_w_destRectanle_x);
		int y = this.getFcbWord(fcb_w_destRectanle_y);
		int w = this.getFcbWord(fcb_w_destRectanle_width);
		int h = this.getFcbWord(fcb_w_destRectanle_height);
		logf("call() - updateRectangle x = %d , y = %d , width = %d , height = %d\n", x, y, w, h);
		
		// TODO: implement
		
		this.setFcbWord(fcb_w_status, Status_success);
	}
	
	private void copyRectangle() {
		int src_x = this.getFcbWord(fcb_w_sourceOrigin_x);
		int src_y = this.getFcbWord(fcb_w_sourceOrigin_y);
		int x = this.getFcbWord(fcb_w_destRectanle_x);
		int y = this.getFcbWord(fcb_w_destRectanle_y);
		int w = this.getFcbWord(fcb_w_destRectanle_width);
		int h = this.getFcbWord(fcb_w_destRectanle_height);
		logf("call() - copyRectangle src_x = %d , src_y = %d , x = %d , y = %d , width = %d , height = %d\n", src_x, src_y, x, y, w, h);
		
		// TODO: implement
		
		this.setFcbWord(fcb_w_status, Status_success);
	}
	
	private void patternFillRectangle() {
		int patternMode = this.getFcbWord(fcb_w_patternFillMode);
		String patternFillMode;
		switch(patternMode) {
		case Pfm_copy: patternFillMode = "copy(0)"; break;
		case Pfm_and: patternFillMode = "and(1)"; break;
		case Pfm_or: patternFillMode = "or(2)"; break;
		case Pfm_xor: patternFillMode = "xor(3)"; break;
		default: patternFillMode = "invalid(" + patternMode + ")";
		}

		int x = this.getFcbWord(fcb_w_destRectanle_x);
		int y = this.getFcbWord(fcb_w_destRectanle_y);
		int w = this.getFcbWord(fcb_w_destRectanle_width);
		int h = this.getFcbWord(fcb_w_destRectanle_height);
		logf("call() - patternFillRectangle x = %d , y = %d , width = %d , height = %d, mode = \n", x, y, w, h, patternFillMode);
		
		logf("         pattern:\n");
		logf("           +---------------------------------+\n");
		for (int i = 0; i < 4; i++) {
			short line = this.getFcbWord(fcb_w4_pattern + i);
			logf("           | %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s |\n",
					((line & 0x8000) != 0) ? "x" : " ",
					((line & 0x4000) != 0) ? "x" : " ",
					((line & 0x2000) != 0) ? "x" : " ",
					((line & 0x1000) != 0) ? "x" : " ",
					((line & 0x0800) != 0) ? "x" : " ",
					((line & 0x0400) != 0) ? "x" : " ",
					((line & 0x0200) != 0) ? "x" : " ",
					((line & 0x0100) != 0) ? "x" : " ",
					((line & 0x0080) != 0) ? "x" : " ",
					((line & 0x0040) != 0) ? "x" : " ",
					((line & 0x0020) != 0) ? "x" : " ",
					((line & 0x0010) != 0) ? "x" : " ",
					((line & 0x0008) != 0) ? "x" : " ",
					((line & 0x0004) != 0) ? "x" : " ",
					((line & 0x0002) != 0) ? "x" : " ",
					((line & 0x0001) != 0) ? "x" : " "
					);
		}
		logf("           +---------------------------------+\n");
		
		// TODO: implement
		
		this.setFcbWord(fcb_w_status, Status_success);
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
	protected void initializeFcb() {
		this.setFcbWord(fcb_w_command, Command_nop);
		this.setFcbWord(fcb_w_status, Status_success);
		this.setFcbDblWord(fcb_dbl_displayMemoryAddress, Mem.getDisplayRealPage());
		this.setFcbWord(fcb_w_color_u0, 0); // red, green
		this.setFcbWord(fcb_w_color_u1, 0); // blue, reserved
		this.setFcbWord(fcb_w_colormapping0, 0);
		this.setFcbWord(fcb_w_colormapping1, 1);
		this.setFcbWord(fcb_w_destRectanle_x, 0);
		this.setFcbWord(fcb_w_destRectanle_y, 0);
		this.setFcbWord(fcb_w_destRectanle_width, 0);
		this.setFcbWord(fcb_w_destRectanle_height, 0);
		this.setFcbWord(fcb_w_sourceOrigin_x, 0);
		this.setFcbWord(fcb_w_sourceOrigin_y, 0);
		for (int i = 0; i < 16; i++) { this.setFcbWord(fcb_w16_cursorPattern, 0); }
		for (int i = 0; i < 4; i++) { this.setFcbWord(fcb_w4_pattern, 0); }
		this.setFcbWord(fcb_w_patternFillMode, Pfm_copy);
		this.setFcbWord(fcb_w_complemented, PrincOpsDefs.FALSE);
		this.setFcbWord(fcb_w_colorIndex, 0);
		this.setFcbWord(fcb_w_displayType, Mem.getDisplayType().getType());
		this.setFcbWord(fcb_w_displayWidth, Mem.getDisplayPixelWidth());
		this.setFcbWord(fcb_w_displayHeight, Mem.getDisplayPixelHeight());
	}
	
}
