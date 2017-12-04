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
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.iUiDataConsumer;

/**
 * Agent for the mouse pointer of a Dwarf machine.
 * <p>
 * This agent is bidirectional: the current mouse position in the
 * Dwarf window is transmitted from the ui to the mesa engine on the
 * one side, a new mouse pointer bitmap and hotspot defined by the
 * running Pilot based OS (XDE or BWS) are registered to the ui on
 * the other side.
 * </p>
 * <p>
 * For the mouse shape, a new bitmap given to the display agent by the
 * OS is passed from the display agent to the mouse agent, as the new
 * mouse shape is only complete when the hotspot is given, which happens
 * when the mouse agent receives a new mouse position to place the
 * mouse bitmap at the new hotspot position. Receiving this position
 * triggers the callback to the ui with the new mouse pointer shape.   
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class MouseAgent extends Agent {
	
	// the current mouse position as transmitted to the mesa engine
	private int mesaCurrX;
	private int mesaCurrY;
	
	// the hotspot in the mouse buitmap
	private int mouseHotspotX = 0;
	private int mouseHotspotY = 0;
	
	// the mouse pointer bitmap is setting a new mouse pointer shape is pending
	private short[] newCursorBitmap = null;
	
	// the mouse position coming from the ui (accessing these must be synchronized(
	private int uiCurrX = 0; // last position passed to the mesa machine
	private int uiCurrY = 0;
	private int uiNextX = 0; // new position from the ui to be passed to the mesa engine 
	private int uiNextY = 0;
	private boolean mouseMoved = false; // is passing the mouse position pending?

	/*
	 * MouseFCBType
	 */
	private static final int fcb_w_currentMousePosition_X = 0; // current position: set by the "real" mouse attached
	private static final int fcb_w_currentMousePosition_Y = 1;
	private static final int fcb_w_cursorOffset_X = 2; // delta as specified by the mesa client program
	private static final int fcb_w_cursorOffset_Y = 3;
	private static final int fcb_w_newValue_X = 4; // new position as specified by the mesa client program
	private static final int fcb_w_newValue_Y = 5;
	private static final int fcb_w_command = 6;
	private static final int FCB_SIZE = 7;
	
	/*
	 * MouseCommandType
	 */
	private static final short Command_nop = 0;
	private static final short Command_setPosition = 1;
	private static final short Command_setCursorPosition = 2;
	
	/*
	 * mesa machine interface
	 */
	
	public MouseAgent(int fcbAddress) {
		super(AgentDevice.mouseAgent, fcbAddress, FCB_SIZE);
		
		this.enableLogging(Config.AGENTS_LOG_MOUSE);
		
		this.uiCurrX = this.mesaCurrX + this.mouseHotspotX;
		this.uiCurrY = this.mesaCurrY + this.mouseHotspotY;
		this.logf("CTOR  => uiCurrX = %d , uiCurrY = %d\n", this.uiCurrX, this.uiCurrY);
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// nothing to shutdown for this agent
	}

	@Override
	public void call() {
		int command = this.getFcbWord(fcb_w_command);
		
		int currX = this.getFcbWord(fcb_w_currentMousePosition_X);
		int currY = this.getFcbWord(fcb_w_currentMousePosition_Y);
		int offsX = this.getFcbWord(fcb_w_cursorOffset_X);
		int offsY = this.getFcbWord(fcb_w_cursorOffset_Y);
		int newX = this.getFcbWord(fcb_w_newValue_X);
		int newY = this.getFcbWord(fcb_w_newValue_Y);
		
		int trgX = 0;
		int trgY = 0;
		
		switch(command) {
		case Command_nop:
			logf("call() - nop\n");
			break;
			
		case Command_setPosition:
			if (this.newCursorBitmap != null) {
				// compute hotspot position and register the new mouse shape with the ui
				logf("call() - setPosition with newCursorBitmap :: newX(%d) , newY(%d) ; mesaCurrX = %d , mesaCurrY = %d\n",
						newX, newY, this.mesaCurrX, this.mesaCurrY);
				
				int deltaHotspotX = this.mesaCurrX - newX;
				int deltaHotspotY = this.mesaCurrY - newY;
				this.mouseHotspotX = Math.max(0,  Math.min(15, this.mouseHotspotX + deltaHotspotX));
				this.mouseHotspotY = Math.max(0,  Math.min(15, this.mouseHotspotY + deltaHotspotY));
				logf("  => deltaHotspotX = %d , deltaHotspotY = %d  ==> newHotspotX = %d , newHotspotY = %d\n",
						deltaHotspotX, deltaHotspotY, this.mouseHotspotX, this.mouseHotspotY);
				if (this.uiPointerBitmapAcceptor != null) {
					uiPointerBitmapAcceptor.setPointerBitmap(this.newCursorBitmap, this.mouseHotspotX, this.mouseHotspotY);
				}
				this.newCursorBitmap = null;

				this.logf("  => uiCurrX = %d , uiCurrY = %d\n", this.uiCurrX, this.uiCurrY);
				this.mesaCurrX = this.uiCurrX - this.mouseHotspotX;
				this.mesaCurrY = this.uiCurrY - this.mouseHotspotY;
				this.setFcbWord(fcb_w_currentMousePosition_X, this.mesaCurrX);
				this.setFcbWord(fcb_w_currentMousePosition_Y, this.mesaCurrY);
				this.logf("  => new mesaCurrX = %d , mesaCurrY = %d\n", this.mesaCurrX, this.mesaCurrY);
			} else {
				// TODO: reposition the "real" mouse pointer?
				trgX = newX + offsX;
				trgY = newY + offsY;
				logf("call() - setPosition :: newX(%d) , newY(%d) , offsX(%d) , offsY(%d) => targetX = %d , targetY = %d\n",
						newX, newY, offsX, offsY, trgX, trgY);
//	??			this.setFcbWord(fcb_w_currentMousePosition_X, newX);
//	??			this.setFcbWord(fcb_w_currentMousePosition_Y, newY);
			}
			break;
			
		case Command_setCursorPosition:
			// TODO: reposition the "real" mouse pointer?
			trgX = currX + offsX;
			trgY = currY + offsY;
			logf("call() - setCursorPosition :: currX(%d) , currY(%d , offsX(%d) , offsY(%d) => targetX = %d , targetY = %d\n",
					currX, currY, offsX, offsY, trgX, trgY);
			break;
			
		default:
			logf("call() - invalid command: %d\n", command);
		}
	}

	@Override
	protected void initializeFcb() {
		this.uiCurrX = Mem.getDisplayPixelWidth() / 2;
		this.uiCurrY = Mem.getDisplayPixelHeight() / 2;
		this.mesaCurrX = this.uiCurrX - this.mouseHotspotX;
		this.mesaCurrY = this.uiCurrY - this.mouseHotspotY;
		
		this.setFcbWord(fcb_w_currentMousePosition_X, this.mesaCurrX);
		this.setFcbWord(fcb_w_currentMousePosition_Y, this.mesaCurrY);
		this.setFcbWord(fcb_w_cursorOffset_X, 0);
		this.setFcbWord(fcb_w_cursorOffset_Y, 0);
		this.setFcbWord(fcb_w_newValue_X, 0);
		this.setFcbWord(fcb_w_newValue_Y, 0);
		this.setFcbWord(fcb_w_command, Command_nop);
	}
	
	/*
	 * ui interface
	 */
	
	private iUiDataConsumer.PointerBitmapAcceptor uiPointerBitmapAcceptor = null;
			
	public void setPointerBitmapAcceptor(iUiDataConsumer.PointerBitmapAcceptor acceptor) {
		this.uiPointerBitmapAcceptor = acceptor; 
	}
	
	public void setPointerBitmap(short[] cursor) {
		this.newCursorBitmap = cursor;
	}
	
	public synchronized void recordMouseMoved(int toX, int toY) {
		this.uiNextX = toX;
		this.uiNextY = toY;
		this.mouseMoved = true;
		Processes.requestDataRefresh();
		this.logf("recordMouseMoved( toX = %d, toY = %d )\n", this.uiNextX, this.uiNextY);
	}
	
	public synchronized void refreshMesaMemory() {
		if (this.mouseMoved) {
			this.uiCurrX = this.uiNextX;
			this.uiCurrY = this.uiNextY;
			this.mesaCurrX = this.uiCurrX - this.mouseHotspotX;
			this.mesaCurrY = this.uiCurrY - this.mouseHotspotY;
			
			this.logf("refreshMesaMemory( uiX = %d , uiY = %d ) => mesaX = %d , mesaY = %d\n",
					this.uiCurrX, this.uiCurrY, this.mesaCurrX, this.mesaCurrY);
			
			this.setFcbWord(fcb_w_currentMousePosition_X, this.mesaCurrX);
			this.setFcbWord(fcb_w_currentMousePosition_Y, this.mesaCurrY);
			this.mouseMoved = false;
		}
	}
}
