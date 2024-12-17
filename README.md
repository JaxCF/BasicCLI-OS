## BasicCLI-OS
BasicCLI is a Unix-like command-line-interface operating system that utilizes multiple standard operating system concepts to manage system resources effectively.

### Kernel
The kernel relies on several functions from the SysLib class that each perform basic interactions with the system to accomplish their tasks.

### Shell
The shell is capable of executing individual commands synchronously or sequentially, depending on syntax.

### Scheduler
There are three different scheduler implementations, each with their own use case and benefit.

**Scheduler_mfq** uses a multi-level feedback queue method to schedule processes. <br />
**Scheduler_pri** uses a priority-based scheduling scheme. <br />
**Scheduler_rr** uses a round-robin method for scheduling.

### Cache
The cache is created and managed using the second-chance algorithm for page replacement. <br /> <br />

*I would like to make it clear that some classes in this project, including SysLib, Boot, FileSystem, and others, were provided for me. I did not write them myself, and I credit the computer science department at UWB for allowing me to utilize their code and build upon it.*