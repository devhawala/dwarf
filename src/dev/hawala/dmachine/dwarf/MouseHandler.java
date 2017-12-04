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

package dev.hawala.dmachine.dwarf;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import dev.hawala.dmachine.engine.iUiDataConsumer;

/**
 * Listener for mouse events for the Dwarf screen pane, forwarding
 * new pointer coordinates or mouse button state changes to the mesa engine.
 * <br>
 * Any mouse event will grab the input focus to the Dwarf window. 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class MouseHandler implements MouseListener, MouseMotionListener {
		
	private final MainUI mainWindow;
	private final iUiDataConsumer mesaEngine;
	
	private final int maxX;
	private final int maxY;
	
	private int lastX = Integer.MIN_VALUE;
	private int lastY = Integer.MIN_VALUE;
	
	/**
	 * Constructor.
	 * 
	 * @param window the main window of the Dwarf application
	 * @param consumer the ui event ccallbacks of the mesa engine
	 * @param displayWidth the horizontal size of the mesa display
	 * @param displayHeight the vertical size of the mesa display
	 */
	public MouseHandler(MainUI window, iUiDataConsumer consumer, int displayWidth, int displayHeight) {
		this.mainWindow = window;
		this.mesaEngine = consumer;
		
		this.maxX = displayWidth - 1;
		this.maxY = displayHeight - 1;
	}
	
	private void handleNewMousePosition(MouseEvent ev) {
		if (ev == null) { return; }
		
		if (!this.mainWindow.getDisplayPane().hasFocus()) {
			this.mainWindow.getDisplayPane().grabFocus();
		}

		int newX = Math.min(Math.max(0, ev.getX()), this.maxX);
		int newY = Math.min(Math.max(0, ev.getY()), this.maxY);
		
		if (this.lastX != newX || this.lastY != newY) {
			this.lastX = newX;
			this.lastY = newY;
			this.mesaEngine.acceptMousePosition(this.lastX, this.lastY);
		}
	}

	@Override
	public void mouseDragged(MouseEvent ev) {
		this.handleNewMousePosition(ev);
	}

	@Override
	public void mouseMoved(MouseEvent ev) {
		this.handleNewMousePosition(ev);
	}

	@Override
	public void mouseClicked(MouseEvent ev) {
		this.handleNewMousePosition(ev);
	}

	@Override
	public void mouseEntered(MouseEvent ev) {
		this.handleNewMousePosition(ev);
	}

	@Override
	public void mouseExited(MouseEvent ev) {
		this.handleNewMousePosition(ev);
	}

	@Override
	public void mousePressed(MouseEvent ev) {
		this.handleNewMousePosition(ev);
		this.mesaEngine.acceptMouseKey(ev.getButton(), true);
	}

	@Override
	public void mouseReleased(MouseEvent ev) {
		this.handleNewMousePosition(ev);
		this.mesaEngine.acceptMouseKey(ev.getButton(), false);
	}
		
}