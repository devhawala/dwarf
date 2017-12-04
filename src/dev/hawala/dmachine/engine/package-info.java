/**
 * The package {@code dev.hawala.dmachine.engine} encompasses all
 * components implementing the mesa engine for Dwarf.
 * <br>
 * The components are organized as follows:
 * <ul>
 * <li>the package itself contains the infrastructure components
 * of the mesa engine like the real/virtual memory implementation,
 * the cpu registers etc.
 * </li>
 * <li>the sub-package {@code .opcodes} contains the implementation
 * of the instructions, organized by the chapters of the document
 * <i>Mesa Processor Principles of Operation Version 4.0 (May85)</i>.
 * </li>
 * <li>the sub-package {@code .agents} holds the implementation of
 * the hardware-interfacing (or simulating) Agents which provide access
 * to the peripherals of a workstation like display, keyboard, mouse,
 * disk, floppy etc.
 * </li>
 * <li>the sub-package {@code .unittest} holds the more or less systematic
 * tests for the mesa instructions (with the exception of the control
 * transfer and processes instructions, sorry).
 * </li>
 * </ul>
 */
package dev.hawala.dmachine.engine;