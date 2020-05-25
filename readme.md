## Dwarf, an emulator for the Xerox Mesa Machine architecture.

Dwarf is an emulator for the Xerox Mesa processor architecture. Dwarf is implemented
in Java 8 and should therefore run on most systems where the Java 8 SE JRE and a decent
window system is available.

In the tradition of Xerox processor implementations having *D* as first letter (Dolphin, Dorado,
Dicentra, Dandelion, Daybreak, Dove, Daisy, ...), this Java software implementation
has been named *D*warf. 

And yes, it is not the first open source software emulator for the Mesa architecture, this
merit goes (to my knowledge) to Don Woodward for his [Dawn emulator](http://www.woodward.org/mps/)
and Yasuhiro Hasegawa for his [Guam mesa-emulator](https://github.com/yokwe/mesa-emulator/tree/master/guam).  

### Prerequisites

To run Dwarf, you will first need a Java 8 runtime installed for your operating system. Dwarf
was developed on Linux and tested with Linux-Mint and MS-Windows 7.

Next you will need a bootable virtual disk having an Pilot 15.3 based Xerox operation system
installed and a compatible germ file. This can be the XDE disk provided by Don Woodward on
his site for his Dawn emulator or your GlobalView 2.1 disk.

For XDE see the Don Woodwards [Dawn emulator](http://www.woodward.org/mps/) page: the page provides
links to the compressed `Dawn.dsk` file and to the source package for Dawn, where the `Dawn.germ`
can be found in the directory `Source/Dawn/Resource`.

For Globalview (the successor to ViewPoint as successor to Star), you're on your own, but your
favorite internet search engine may be helpful...

### Working characteristics of Dwarf

Besides a mesa processor, Dwarf supports the following peripherals found on a Xerox workstation
like Dandelion (8000 system) or Dove (6085 system):

- real memory in exponential increments between 256 kwords (512 KiB) and 8192 kwords (16 MiB)

- virtual memory in exponential increments from real memory size up to 32768 kwords (64 MiB)

- black&white or (8-bit) color bitmapped display, rendered in a Java Swing window

- keyboard

- 2- or 3-button mouse

- one harddisk image

- one floppy drive (1.44 MByte raw or legacy IMD/DMK floppies, changeable at runtime)

- network device, interfacing to a Dodo NetHub or as fallback to an internal time service

Dwarf supports the following floppy formats:

- IMD for legacy floppies if the file extension is `.imd` (case-insensitive)

- DMK for legacy floppies if the file extension is `.dmk` (case-insensitive)

- raw format for 3.5" floppies as created by the original emulator on PCs

The term "legacy floppy" means that the image was created from a floppy disk written by
a 8010 (8" floppy) or a 6085 (5.25" floppy) workstation with a Pilot based OS like XDE (4.0
or later) or ViewPoint (1.0 or later). Legacy floppy images are mounted in R/O mode, as
changes cannot be written back into the original format (IMD or DMK).  
The disk content of the legacy floppy image is implanted in a template 3.5" image
based on the XDE sector layout. This allows to read the legacy floppy content.

When using or thinking of using Dwarf, some characteristics of the implementation should
be kept in mind:

- Dwarf loads the complete virtual hard disk into main memory, performing all disk operations
in memory. When (and if) the system is shut down normally, the modifications to the hard disk
are written to a compressed delta file containing the differences to the original disk, which
stays unmodified. When the same system is next started, Dwarf first reads the original disk
and applies the delta in memory.  
This approach gives fast disk I/O, but possibly requires increasing the Java heap space for
large disks (however the "usual" 30 MByte Pilot disk plus 16 MByte real memory can be used
with Java defaults)

- similarly Dwarf loads a complete virtual floppy disk image file into memory
for diskette operations. In case of a raw  (1.44 MByte) floppy image, when ejecting the floppy
or shutting down the machine, the complete floppy image file is overwritten if the floppy was modified
(no delta file). Legacy IMD or DMK floppies are always mounted read-only and are not written
back, as only reading IMD or DMK files is supported.

- unlike Xerox machines, Dwarf is not a microcoded machine, so Dwarf can only execute the
Mesa instructions; running Lisp or Smalltalk or Cedar environments (would these be available)
by loading a different microcode is not possible.

- Dwarf supports networking by connecting to a Dodo NetHub, allowing to interact with
network elements connected to the same NetHub, these can be other emulated Xerox machines,
a Dodo server providing XNS services or a "real" network with the NetHubGateway (see
[Dodo Services - XNS with Java](https://github.com/devhawala/dodo)).

### Running Dwarf

Besides having the Dawn program installed (mainly a simple runnable Java jar file) with the disk
and germ files, a configuration file defining the characteristics of the machine to run is needed. 

#### Installing or building Dwarf

Dwarf is available as ready to run release on Github: download the newest `dist.zip` package
from the github repository, unzip the package, done (more or less).

The archive `dist.zip` contains a runnable jar `dwarf.jar` and the default keyboard mapping
file, as well as sample configuration and shell script files for running XDE (Dawn) and
GlobalView (GVWin) environments.    
Copy your disk and germ files for the environments into the corresponding directories and if
necessary adapt the filenames in respective configuration file.

Hint: the disk or floppy files should not reside in the same directory as the `dwarf.jar` file,
as the Java runtime will high probably enforce the read-only access to files in this directory.    
The `dist.zip` archive contains a exemplary directory structure.

For building from source, download the ZIP or clone the Dwarf Github repository, this gives you
a project directory that can be imported into Eclipse (at least Mars release, preferably Neon).
The main Java class for the _Run Configuration_ is: `dev.hawala.dmachine.Dwarf`  

#### Defining the configuration of a Dwarf machine

A Dwarf machine is configured with a conventional Java properties file, so the name-value
separator is the `=` character and comments are introduced by a `#`.

The following configuration parameters define a Dwarf machine:

- `boot`  
the filename of the boot disk  
_required_

- `germ`  
the filename of the germ file  
_required_

- `switches`  
the boot switches to use  
_optional, default_: `8Wy{|}\346\347\350\377`

- `addressBitsReal`  
number of address bits for the real memory, this must be a value between 18 ad 23,
giving between 256 kwords (512 kByte) and 8192 kwords (16 MByte)  
_optional, default_: `22`

- `addressBitsVirtual`  
number of address bits for the virtual memory, this must be a value between `addressBitsReal`
and 25, allowing up to 32768 kwords (64 MByte)  
_optional, default_: `23`

- `displayWidth`  
the width of the mesa machine display in pixels, must be a multiple 16  
_optional, default_: `1024`

- `displayHeight`  
the height of the mesa machine display in pixels  
_optional, default_: `640`

- `displayTypeColor`    
boolean specifying the display mode for Dwarf: the value `false` specifies a monochrome
display (default), while `true` specifies the display as 8-bit deep color display
(with a color lookup table controlled by the mesa machine);    
setting this option to `true` is intended for running GVWin 2.1, although the Dawn/XDE disk
also works with color mode (not showing any colors however)    
_optional, default_: `false`

- `keyboardMapFile`  
the name of the keyboard mapping file to use; see the syntax description at the
beginning of the sample mapping file [kbd\_linux\_de\_DE.map](keyboard-maps/kbd_linux_de_DE.map)  
_optional, default_: none (if not given, a minimal mapping for a german keyboard is used)

- `xeroxControlKeyCode`  
the key code for the modifier key to be used for generating the special Xerox keys not available
of a contemporary standard keyboard; the value can be given as hex code introduced by `0x` or
as the Java key name (`VK_`...)  
_optional, default_: 0x00000011 (VK_CONTROL)

- `resetKeysOnFocusLost`  
if `true` the keyboard state will be reset to "no keys pressed" when Dwarf loses the input
focus; when `false` the keyboard will not be reset. Resetting all keys to "up" is useful
when changing the current window with Alt-TAB, as the Alt-key is a special key on a Xerox
keyboard.  
_optional, default_: `true`

- `processorId`    
the processor or machine id for the Dwarf machine (or MAC address in todays wording)  
(if networking is used, it should be ensured that **all** machines on the network
have an unique processor id, or Pilot-based machines will stop by entering 0915 state)    
_optional, default_: `00-1D-BA-AE-04-C3`

- `title`  
the text to display in the title bar of Dwarfs window  
_optional, default_: value of `boot` (the boot disk name)

- `oldDeltasToKeep`  
the number of deltas to keep in addition to the current delta; when saving
a new delta, Dwarf renames the now previous delta using the its save timestamp
and deletes the older backups exceeding `oldDeltasToKeep`  
_optional, default_: `5`

- `initialFloppy`  
the filename of a virtual floppy file to load in the floppy drive before
starting the engine  
_optional, no default_ (no floppy initially loaded)

- `floppyDirectory`          
the directory-name to use as starting point in the file selection dialog
for the _insert floppy_ function  
_optional, no default_ (platform dependent, probably the home directory)

- `netHubHost`    
the name of the NetHub host to connect to or empty if no NetHub connection
is to be used    
_optional, default_: _empty_ (i.e. no NetHub connection = no networking)

- `netHubPort`    
the port where the NetHub is listening (must be in range 1..65535) or
any invalid port number (e.g. `0`) for no NetHub connection      
_optional, default_: 3333

- `localTimeOffsetMinutes`    
time zone parameter for the internal time service if no NetHub is used:
difference between local time and GMT in minutes, with positive values being
to the east and negative to the west (e.g. Germany is 60 without DST and 120
with DST, whereas Alaska should be -560 without DST resp -480 with DST)    
_optional, default_: 0 (i.e. GMT)

- `autostart`  
the boolean value `true`lets the machine start automatically after
building up the Dwarf UI; by default, the Dwarf machine must be started
manually with the _Start_ button in the toolbar.  
_optional, default_: `false`

Sample properties `dawn.properties` for running the Dawn disk:

```
#
# Dwarf configuration for Dawn's xde system
#

title = XDE

boot = Dawn/Dawn001.dsk
germ = Dawn/Dawn.germ
switches = 8Wy{|}\\346\\347\\350\\377

processorId = 10-00-FF-12-34-56

displayWidth = 1024
displayHeight = 640

addressBitsVirtual = 23
addressBitsReal = 22

keyboardMapFile = keyboard-maps/kbd_linux_de_DE.map

floppyDirectory = Dawn/floppies

# network configuration, uncomment to use NetHub
#netHubHost = localhost
#netHubPort = 3333

# internal time service as network fallback: time zone offset to GMT in minutes (negative => west, positive => east)
# Germany with DST => +120 ; Alaska => -480 
localTimeOffsetMinutes = 120

autostart = true
```

#### Command line parameters for Dwarf

Dwarf is packaged in a runnable jar file (`dwarf.jar`) and started with

`java -jar dwarf.jar` _`parameters`_

and the following parameters:

- _configuration-properties-file_  
the properties file for the Dwarf machine; if the filename given does
not have the `.properties` file extension, it will be added to the given
name  
_required_

- `-run`  
start the Dwarf machine, overriding the `autostart` parameter in the
configuration file  
_optional_

- `-v`  
dump the parameters loaded from the configuration file before building up
the Dwarf UI.  
_optional_

- `-logkeypressed`  
log the key press events to the console, issuing the hexadecimal extended key code and
if possible the symbol name corresponding to that key; any of these values
can be used in a keyboard mapping file.  
_optional_

- `-merge`  
instead of running the mesa emulator, merge the current delta for the boot disk into
the harddisk image file; the current disk file, the delta and all backup deltas are
archived into a zip file (named after the disk file and the current timestamp) and all
delta files are deleted before doing the merge.  
_optional_ (as alternative to running the mesa engine) 

Examples:

`java -jar dwarf.jar dawn -run`  
(run the virtual xerox machine configured in the file `dawn.properties`)

`java -jar dwarf.jar dawn -merge`  
(merge the delta into the disk file for virtual xerox machine configured in the file `dawn.properties`)

#### Running a Dwarf machine

The following screenshots show the button bar:

![Dwarf-Button-Bar](images/Dwarf-Button-Bar.png)

and the status line:

![Dwarf-Status-Line](images/Dwarf-Status-Line.png)

of a running Dwarf emulator.

##### Button bar

The state of the buttons reflects the possible options, e.g. the "Start" button is grayed
and disabled if the mesa engine is already running. The buttons should be self-explaining
and display a hint when the mouse stays a while over them.

The _Insert_ button opens the file selection dialog for the floppy image file to load.
Virtual floppy files must have have an extension `.imd` or `.dmk` for loading a legacy
floppy image or any extension but a size of 1440 KiB = 1.474.560 bytes to be accepted in the
file selection dialog for "inserting" the floppy. If the _R/O_ checkbox is checked, the
floppy will be loaded in read-only mode, rejecting any attempts to modify the diskette.
Legacy floppy images are always forced to be loaded in read-only mode.  
The _Eject_ button removes the floppy, overwriting the floppy image with the new
content if the floppy was modified.  

##### Keyboard

The keyboard mapping can be freely defined through a mapping file, however providing a
sensible fallback (at least if you have a german keyboard) if no file is specified.

The probably most interesting information is about the special Xerox command keys
not available on modern PCs, which are mapped by default as follows:

Xerox key|PC key(s)
---------|------
Stop|ESC
Open|Ctrl-O
Props|Ctrl-P
Copy|Ctrl-C
Move|Ctrl-M
Next|Ctrl-N
Find|Ctrl-F
Again|Ctrl-A
Same|Ctrl-S
Undo|Ctrl-U
Help|Ctrl-H

##### Status line

The status line of Dwarfs emulator window shows the current MP code (maintenance panel code)
at the left, followed by some statistics of the running machine (uptime in seconds, number
of instructions executed so far, number of disk page reads/writes, number of floppy page
reads/writes, number of network packets received/sent).

#### Halting the running system

The safe way to bring down the guest OS is to use the corresponding function of its UI (e.g.
"Boot button" in the "Boot from:" menu in XDE), as this ensures that the OS flushes all
buffers (or at least should so) before executing one of the stopping instructions.
When the mesa engine stops running, the state of the virtual harddisk and of the possibly
loaded virtual floppy is saved to disk.

Clicking the "Stop" button or closing Dwarfs window while the mesa engine is running will
open a confirmation dialog: confirming will do a hard stop of mesa engine by leaving the
instruction interpreter loop and then save the current state of the harddisk and floppy.
However the OS has no chance to flush its buffers, so the current state of the harddisk
is probably inconsistent and may require a scavenger run on next startup.

Stopping Dwarf externally using the host OS means (Ctrl-C in the shell running the Java program,
`kill` command etc.) will of course prevent writing back the harddisk resp. floppy content,
so changes will be lost.

Warning: unlike other emulators, Dwarf does not overwrite the original virtual harddisk
file, but saves changes in (compressed) delta files. This means that the delta must first
be included in the harddisk image file (using the `-merge` command line parameter)
before the harddisk file is used with an other emulator.  
Not merging the delta has the following effects:

- when running an other emulator with the virtual harddisk, the changes made in Dwarf
  boot sessions will not be visible, as Dwarf leaves the original file unchanged
  
- saving changes to the virtual harddisk file from the other emulator will make the
  delta files saved by Dwarf useless, as the base for the delta changed and will
  high probably result in a corrupt disk when booting the new base disk file with the delta.  
  However removing the delta and starting with only the new base disk should work (if
  the other emulator was closed regularly) and the changes in the disk will be
  visible in Dwarf's session.

### Known limitations

- running Dwarf has currently a "boot once" approach, meaning there is no way to reset or restart
  the guest OS once it was booted: to restart the OS, the Dwarf program must be stopped and
  started again
  
- furthermore other non-vital devices are missing, having only rudimentary agent implementations
  (stream, tty, serial and parallel ports).

- using (explicitly or implicitly) functionality requiring the StreamAgent to communicate
  with the "external" world (local file access, local printing, copy&paste, ...) will probably
  leave the system in an unusable state (hourglass mouse pointer with fast running cpu), as
  Dwarfs agent probably gives the wrong answer for "not available". 

- although Dwarfs mesa processor implementation supports both the "old" global frame architecture
(all global frames are in the Main Data Space) and the "new" architecture ("MDS-relieved": global
frames can reside outside the MDS), only the "new" variant has ever been tested, as no bootable
disk for an "old" OS version is known to be available, besides the fact that interfacing the vital
devices (disk, keyboard etc.) is undocumented and no sample open-source implementation is available for
analysis.  
Therefore no option to choose the "old" global frame architecture is available.   
 

### Known Bugs

(currently no known bugs; a stability problem in BYTBLT(R) instructions has been fixed, as well as
the misinterpretation of the princops regarding handling the bit-addresses in BITBLT/COLORBLT
instructions when direction is 'reverse', along with smaller flaws preventing the correct rendering
of the help icon in GlobalView)

### Development history

- 2020-05-25    
added color display support, allowing to run GVWin 2.1 in 8-bit color mode

- 2020-05-23    
implemented some of the floating point instructions, changed status line font for better
rendering on linux-mint

- 2019-05-01    
fixes for BITBLT/COLORBLT, fixes for smaller flaws preventing the correct rendering of the help
icon in GlobalView

- 2019-04-26    
bugfix to instructions BYTEBLT and BYTEBLTR, utility program CnvDmk2Imd.java
for converting DMK floppy image to IMD format

- 2018-09-16    
added network support by reworking the NetworkAgent for interfacing the
NetHub of Dodo Services and use an internal time service if networking is disabled

- 2018-07-17    
added the "-merge" command line parameter (for merging the delta file into the harddisk image file)

- 2018-07-14    
fixes for legacy floppies (reject DOS floppies,
improved compatibility with more DMK-/IMD-imaging tools)

- 2018-07-08    
added support for reading IMD and DMK legacy floppies

- 2018-06-26    
keyboard improvements

- 2018-01-09    
made the modifier key for generating special xerox keys configurable,
added experimental internal time server

- 2017-12-04    
initial commit to github: First release for Dwarf, able to run Dawn/XDE and GVWin 2.1 as
standalone machines

### Bibliography
The following documents available in the internet were useful for creating Dwarf and its mesa engine:

- [Mesa Principles of Operation, Version 4.0, May 1985](http://bitsavers.informatik.uni-stuttgart.de/pdf/xerox/mesa/princ_ops/Mesa_Processor_Principles_of_Operation_Version_4.0_May85.pdf)

- [Mesa Principles of Operation: Changed Chapters](http://www.woodward.org/PrincOps/changedChapters.htm)

- [PrincOps Corrections](http://www.woodward.org/PrincOps/01xPrincOpsCorrections.html)

- [6085 FE Training](http://bitsavers.informatik.uni-stuttgart.de/pdf/xerox/6085/service/6085_FE_training.pdf)

### Acknowledgments/Credits

Special thanks go to Don Woodward and Yasuhiro Hasegawa who implemented Mesa emulator programs and
published their work as open source, including their own documentation (see Bibliography). These programs sources
and documents were extremely helpful and in fact essential in filling the gaps left by the Xerox PrincOps
documents about such "details" as:

- the numeric opcodes for the instructions

- interfacing the peripherals like screen, mouse, keyboard, disk etc.

- undocumented instructions like VMFIND (essential for the Pilot-OS) or COLORBLT

- initialization sequence for loading and preparing the germ to bring a mesa engine in a runnable state
  as defined by PrincOps for the initial XFER.
  
Further thanks go to websites resp. museums like [Bitsavers](http://bitsavers.org/),
[Computer History Museum](http://www.computerhistory.org/) or [DigiBarn Computer Museum](http://www.digibarn.com/)
(just to name a few!) and the people behind these sites and museums
which archive the heritage of the emerging digital age, be it documents, hardware or software, among them also for Xerox systems.

Xerox itself has thankworthy made the Alto software and documentation accessible (hosted by the Computer History Museum, see
[Xerox PARC Alto filesystem archive](http://xeroxalto.computerhistory.org/index.html)), a similar generosity regarding
documents and software for the more modern D-machines and successors would of course be welcome.

### License

Dwarf is released under the BSD license, see the file `License.txt`.

### Disclaimer

All product names, trademarks and registered trademarks mentioned herein and in the
source files for the Dwarf program are the property of their respective owners.
