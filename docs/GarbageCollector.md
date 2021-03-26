# Garbage Collector


## Contents

*   [Overview](#overview)
*   [Main Classes](#main-classes)
    *   [Extensions to Compiler](#extensions-to-compiler)
    *   [Generation of Library Functions](#generation-of-library-functions)
*   [Library Functions](#library-functions)
*   [Command Line Options](#command-line-options)


## Overview

JCC provides a simple [mark-and-sweep](https://en.wikipedia.org/wiki/Tracing_garbage_collection) 
garbage collector for languages that need garbage collection. The runtime support for garbage
collection is provided by three assembly functions that are compiled into the executable. The
code generator inserts calls to these functions in code that deals with dynamic memory. Currently,
the only type of dynamic memory that is supported is strings.

The diagram below describes the memory layout of a program with three string variables. Yellow
boxes represent memory allocated per variable. Blue boxes represent stack memory used for GC 
bookkeeping, while green boxes represent other, unrelated memory. The white boxes in the middle
represents the nodes in the allocation list that is used to keep track of marked and unmarked 
memory.

![GC Memory Diagram](diagrams/GC.png)


## Main Classes

The main classes that are involved in managing memory can be divided into two groups: classes
that extend the main compiler classes with garbage collection functionality, and classes that
generate the library functions that manage memory in runtime.


### Extensions to Compiler

The memory management functionality is implemented in two classes: 
`AbstractGarbageCollectingCodeGenerator` and `GarbageCollectingFunctionCallHelper`.

![GC Extensions](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/GCExtensions.puml)

Class `AbstractGarbageCollectingCodeGenerator` is responsible for allocating stack memory for
the variable type pointers, makes sure to call the correct library functions while generating
code for assign expressions, and extends the code generation for add expressions with support
for strings, which requires dynamic memory management.

Class `GarbageCollectingFunctionCallHelper` is responsible for cleaning up any dynamic memory
that was allocated while calling a function.


### Generation of Library Functions 

The diagram below shows the classes involved in generating assembly code for the library functions.
The library functions themselves are described in section [Library Functions](#library-functions).

![Memory Management Functions](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/MemoryManagementFunctions.puml)


## Library Functions

In runtime, most of the GC functionality is performed by three library functions that are 
compiled into the executable. These functions are called by the code that is generated for
assign expressions to manage memory.

<table>
  <tr>
    <th>Function</th>
    <th>Java Class</th>
    <th>Description</th>
  </tr>  
  <tr>
    <td>memory_mark</td>
    <td>MemoryMarkFunction</td>
    <td>
        Marks used memory in the allocation list. Loops through all variable type pointers, 
        and marks the memory nodes referenced by these type pointers.
    </td>
  </tr>
  <tr>
    <td>memory_register</td>
    <td>MemoryRegisterFunction</td>
    <td>
        Registers newly allocated memory in the allocation list. Creates a new node for 
        the allocated memory, and adds it to the beginning of the allocation list.<br/>
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
    <td>
        Deletes unused memory in the allocation list. For each node in the allocation list,
        checks if this node has been marked. If so, unmarks the node. If not, removes the 
        node from the list, and frees the associated memory, and the memory allocated for
        the node itself.
    </td>
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
