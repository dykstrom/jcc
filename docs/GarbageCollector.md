# Garbage Collector


## Contents

*   [Overview](#overview)
*   [Main Classes](#main-classes)
    *   [Extensions to Compiler](#extensions-to-compiler)
    *   [Generation of Library Functions](#generation-of-library-functions)
*   [Library Functions](#library-functions)
*   [Command Line Options](#command-line-options)


## Overview

TBD

![GC Memory Diagram](diagrams/GC.png)


## Main Classes

The main classes that are involved in managing memory can be grouped into two groups: classes
that extend the main compiler classes with garbage collection functionality, and classes that
generate the library functions that manage memory in runtime.


### Extensions to Compiler

TDB

![GC Extensions](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/GCExtensions.puml)


### Generation of Library Functions 

The diagram below shows the classes involved in generating assembly code for the library functions.
The library functions themselves are described in section [Library Functions](#library-functions).

![Memory Management Functions](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/MemoryManagementFunctions.puml)


## Library Functions

In runtime, most of the GC functionality is performed by three library functions that are 
compiled into the executable. These functions are called by the code that is generated for
assignment expressions to manage memory.

<table>
  <tr>
    <th>Function</th>
    <th>Java Class</th>
    <th>Description</th>
  </tr>  
  <tr>
    <td>memory_mark</td>
    <td>MemoryMarkFunction</td>
    <td>Mark used memory in allocation list</td>
  </tr>
  <tr>
    <td>memory_register</td>
    <td>MemoryRegisterFunction</td>
    <td>
        Creates a new node for some allocated memory, and adds it to the beginning of the 
        allocation list.<br/>
        <br/>
        Makes sure that the variable type pointer points to the new node, 
        and that the node is unmarked.<br/>
        <br/>
        Finally, increases allocation count and checks if number of allocated memory blocks
        have reached the configured limit. If so, calls functions memory_mark and memory_sweep 
        to reclaim some memory.
    </td>
  </tr>
  <tr>
    <td>memory_sweep</td>
    <td>MemorySweepFunction</td>
    <td>Delete unused memory in allocation list</td>
  </tr>
</table>


## Command Line Options

The following command line options (to the compiler) affects garbage collection in the compiled
program:

<table>
  <tr>
    <th>Option</th>
    <th>Default</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>-initial-gc-threshold</td>
    <td>100</td>
    <td>Number of allocations done before first garbage collection</td>
  </tr>
  <tr>
    <td>-print-gc</td>
    <td>false</td>
    <td>Print messages at garbage collection</td>
  </tr>
</table>
