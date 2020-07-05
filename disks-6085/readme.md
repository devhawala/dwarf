## Disk image files for Draco 6085 emulation

The files in this directory provide the basic disk images for ViewPoint and XDE that can be used
with the Draco 6085 emulator.

All 3 disks "descend" from the same XDE 5.0 disk found at Bitsavers (archive
[Micropolis_1325_XDE_5.0_Install.zip](http://bitsavers.informatik.uni-stuttgart.de/bits/Xerox/6085/Disk_Emulator_Images/Micropolis_1325_XDE_5.0_Install.zip)). As the extracted disk image in this archive only contains the sector
data, but not the labels also required by the Pilot OS, the raw emulator image file (`microp1325_xde5_937.em`)
had to be reprocessed with David Gesswein's MFM-tools (see [MFM Hard Disk Reader/Emulator](https://github.com/dgesswein/mfm))
to regenerate both the label and the data areas of the disk sectors for a Xerox 6085 system. Based on the 2 files created
by the mfm-tools from the raw emulator image, the initial emulation disk to be used by Draco could be created, allowing to
implement and test Draco's rigid disk emulation part.

The 3 disk files are intended to be used with the sample configurations in the distribution archive, but can be
used as starting point for own machine configurations. The reason for separating the configurations from the
disk images is simply to reduce network traffic: only the disk for the system to be used needs to be downloaded from
Github, one the other hand, no need to upload the disk package set (with over 16 MBytes) just because one file changes.
So in addition to the distribution package, the respective disk images must be downloaded as required.

The following disk image files for Draco are available:

- `xde5.0.zdisk`    
this is the original disk image regenerated from the archive at Bitsavers; it contains an XDE 5.0 environment
as installed from the XDE 5.0 distribution floppies, having a debugger boot file (`CoPilotDove.boot`)
on the CoPilot volume, a normal boot file (`TajoDove.boot`) on the Tajo volume and all provided standard XDE tools and tutorials installed on CoPilot (but no development tools like compiler, binder etc., as these are not contained on
the XDE 5.0 distribution floppies).    
To use this disk with the sample configuration, copy the file into the `xde5.0` subdirectory of the
unpacked distribution.    
As this disk seems to have been created with a real Xerox 6085 workstation, it can be considered as a reference
for Dracos rigid disk emulation quality. To be honest, the results are mixed: booting the disk from the CoPilot volume
and working with XDE (list, create, edit or delete files, access network services or the like) works as expected;
but anything involving world-swapping fails with varying MP codes (mostly with 0921 - boot loader device error)
and any attempt to stop the system with XDE context menus leave the disk in an unusable state (MP code 0915
on next reboots). In other words: Pilot 12.3 on an original 6085 disk is not fully satisfied with the hard disk
emulation provided by Draco. So:
  - NEVER shutdown the system with one of the XDE menu items ("Boot button", "Quit" or "Power off")!
  - ALWAYS shutdown the system running from this disk ONLY with the "Stop"-button of the emulator!
  - if the system comes into this state, fall back to a working state by deleting the last delta file for
    the disk and renaming a working archived delta file to the .zdisk.zdelta extension

- `xde5.0_2xTajo+hacks.zdisk`    
this is a XDE 5.0 environment completely rebuilt using the various floppy disk sets for XDE 5.0 available
at Bitsavers. It has the standard volumes (CoPilot and Tajo), but having the normal boot file (`TajoDove.boot`)
on both volumes; in addition to the standard installation files (XDE tools and tutorials), a large set of the
so-called unsupported tools (or "hacks") found on the XDE 5.0 floppy disk sets for 6085 and 8010 were copied
to the CoPilot volume and some of them are run at startup by User.cm (an early restricted Sword version, SmoothScroll,
Clock with DisplayImpl).    
To use this disk with the sample configuration, copy the file into the `xde5.0_2xTajo+hacks` subdirectory of the
unpacked distribution.    
This XDE setup has more stability than the original disk, as even if not all possible shutdown options of XDE
work correctly, the disk stays the disk in an usable state:
  - "Boot button" from the HeraldWindow context menu works (system can be restarted)
  - "Quit" from the "Exec Ops" desktop background menu works (equivalent to "Boot button")
  - "Power off"  from the "Exec Ops" desktop background menu locks up the system (MP goes back to 0990 after
     transiting through 0910/0920) but the system can be started normally after killing Dwarf/Draco

- `vp2.0.5.zdisk`    
this is a ViewPoint 2.0 environment built starting with the XDE 5.0 disk using the VP 2.0.5 floppy disk set for 6085
available at Bitsavers. In addition to the basic workstation (BWS, VP Editor), many tools from the additional VP 2.0
floppy disks were installed (VP Netcomm, Essentials Applications, Office Accessories, Document editor extensions,
Spreadsheet, File Converters, Help documents).    
Using the procedure provided at the end of section 3.3.3 in the
[Darkstar readme](https://github.com/livingcomputermuseum/Darkstar#333-installing-an-operating-system), the "Software 
Options" of the system were set to allow all applications and the netcomm option. This is however bound to the
machine id `10-00-FE-31-AB-21`, so setting a different processor id in the configuration using this disk
will require to set the software options again using the mentioned procedure.    
To use this disk with the sample configuration, copy the file into the `vp2.0.5` subdirectory of the
unpacked distribution.    
As Viewpoint 2.0 does not have an option for halting the system at logoff (and "Power off quick restart"
does not work with Draco and was uninstalled again), the only way for shutting down the ViewPoint system is
to logoff with any variant (retain, delete or transfer the desktop to the file server), wait until the screen
turns black with the bouncing keyboard and then simply halt the system with the "Stop"-button of the Dwarf/Draco
emulator (which will save disk changes as new delta file).



Remark: as all attempts to create an empty disk from scratch using head/cylinder geometries of various disks
available in the 1980's failed (the disks were not accepted by the installer), the Micropolis 1325 used as
starting point is currently the only disk type available for 6085 machines emulated with Draco.    
This disk has a raw capacity of 85 MByte and a formatted capacity of ~ 61 MByte (formatted for Pilot, 122880 sectors).