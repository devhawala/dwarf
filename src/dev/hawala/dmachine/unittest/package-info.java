/**
 * UnitTests for the mesa engine instructions.
 * <p>
 * As for the instruction implementations, the tests are grouped by
 * the chapters of the PrincOps document where the instructions
 * are specified.
 * <br>
 * However, there are currently no tests for the Control Transfer (chapter 9)
 * and Processes (chapter 10) instructions. The reason is partly laziness and
 * partly the fact that the environment required to test most of these instructions
 * (code segments, linkages, process table setups) encompasses half an OS like Pilot.
 * </p>
 * <p>
 * The problem with the last point is that building that environment based on the
 * same assumptions used to implement the instructions will not result in an useful
 * test. The real test environment for testing control transfer is in fact Pilot,
 * So the "strategy" for these 2 chapters was to implement the instructions, review the
 * implementation twice in longer time intervals, correcting recognized deviations from
 * the PrincOps. This approach worked well for the Processes instructions (which worked
 * fine from the start on when booting Pilot), but however only in the second attempt
 * for the control transfer instructions.
 * </p>
 */
package dev.hawala.dmachine.unittest;