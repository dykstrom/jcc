# Arrays


## Contents

*   [Overview](#overview)
*   [Main Classes](#main-classes)
*   [Memory Layout](#memory-layout)
*   [Element Access](#element-access)
*   [Option Base](#option-base)
*   [Bounds Checking](#bounds-checking)
*   [Garbage Collection](#garbage-collection)


## Overview

Arrays in JCC can hold elements of the simple types: float, integer, and string. 
They can have a single or multiple dimensions. However, at present they can only 
be static, that is, they must be created when the program starts, and exist until 
the program ends. The size of an array must also be static, defined by one or more 
literal values or constants.

In BASIC, an array that is used without having been declared is created implicitly, 
with the inclusive upper bound 10 in every dimension. The declaration is synthesized 
by `BasicSemanticsParser` and added to the start of the program, so that code 
generation allocates the array as it would any other &ndash; see 
`docs/system/basic-language.md`.


## Main Classes

An instance of class `Arr` is used to represent an array type. An instance of 
class `ArrayDeclaration` is used to encapsulate the declaration of a single array. 
Besides a type (that is always an instance of class `Arr`), an `ArrayDeclaration` 
has a list of subscript expressions &ndash; the dimensions of the array.

![Array Declaration](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/ArrayDeclaration.puml)

The AST class used when accessing an array element is `ArrayAccessExpression`. 
Besides an `Identifier` that identifies the array, this class also has a list 
of subscript expressions (indices) that specifies the element to access in the 
array.

![Array Access](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/ArrayAccess.puml)


## Memory Layout

Each array is stored as its own private module-level global with an aggregate type:

```
   @<name>_arr      = private global   [N x T] zeroinitializer        ; element storage
   @<name>_arr_dims = private constant [D x i64] [i64 s0, i64 s1, …]  ; dimension sizes
```

`N` is the product of the (inclusive-adjusted) dimension sizes, `T` is the element type
(`i64`, `double`, or `ptr`), and `D` is the number of dimensions. String elements default to
a pointer to the empty-string constant rather than a null pointer, so an unassigned element
prints as the empty string, matching scalar string variables. Because every array is its own
global, setting an element can never corrupt an adjacent variable.


## Element Access

The data of a single-dimension array is just a sequence of array elements in 
memory. The data of a multi-dimension array has a layout that depends on the 
number of dimensions. Note that in BASIC, the size and indices used to declare 
and access arrays are inclusive, while in most other languages they are 
exclusive. This code creates an array of size 6 in BASIC:

```BASIC
DIM a(5) AS INTEGER
```

In other languages a similar statement would instead create an array of size 5.
Below are examples of arrays of dimension 1, 2, and 3.

A one-dimensional array a(5):

```
   0 1 2 3 4
```

The exact location of an element in a one-dimensional array is calculated like:

```
   Code      Memory Location     Element
   a(0)      0                   0
   a(1)      1                   1
```

A two-dimensional array a(5, 2):

```
   00 01
   10 11
   20 21
   30 31
   40 41
```

The exact location of an element in a two-dimensional array is calculated like:

```
   Code        Memory Location       Element
   a(0, 0)     (0 * 2) + 0 == 0      00
   a(1, 1)     (1 * 2) + 1 == 3      11
   a(3, 0)     (3 * 2) + 0 == 6      30
   a(4, 1)     (4 * 2) + 1 == 9      41
```

A three-dimensional array a(4, 2, 3):

```
   [000 001 002] [010 011 012]
   [100 101 102] [110 111 112]
   [200 201 202] [210 211 212]
   [300 301 302] [310 311 312]
```

The exact location of an element in a three-dimensional array is calculated like:

```
   Code            Memory Location                  Element
   a(0, 0, 0)      ((0 * 2) + 0) * 3 + 0 ==  0      000
   a(0, 0, 1)      ((0 * 2) + 0) * 3 + 1 ==  1      001
   a(0, 1, 0)      ((0 * 2) + 1) * 3 + 0 ==  3      010
   a(1, 0, 0)      ((1 * 2) + 0) * 3 + 0 ==  6      100
   a(1, 1, 1)      ((1 * 2) + 1) * 3 + 1 == 10      111
   a(3, 1, 2)      ((3 * 2) + 1) * 3 + 2 == 23      312
```

The algorithm for calculating the location of an array element can be described 
like this in pseudocode:

```
   Initialize the index to 0
   For each dimension:
      Multiply the index by the size of the dimension
      Evaluate the subscript expression for the dimension
      Add the evaluated value to the index
```

This multiply-accumulate computation produces a single flat `i64` index. A
`getelementptr T, ptr @<name>_arr, i64 <index>` then yields the element address for a
`load` (read) or `store` (write).


## Option Base

The array subscripts are 0-based by default. The BASIC module of JCC supports the 
OPTION BASE statement that can make the subscripts 1-based instead of 0-based. The 
only allowed bases are 0 (the default) and 1. The OPTION BASE statement only affects 
the lower bound of the array, and not the dimension metadata. It needs no runtime
call, because the lower bound is a compile-time constant.

`LBOUND` and `UBOUND` are lowered inline, so the `libjccbas` runtime functions are not
used:

* `LBOUND(a[, d])` is the `OPTION BASE` value (0 or 1), the same for every dimension.
* `UBOUND(a[, d])` is `size(d) - 1`, where `size(d)` is read from `@<name>_arr_dims` at index
  `d - 1` (the 1-based dimension `d` defaults to 1). A runtime dimension argument is therefore
  supported directly.


## Bounds Checking

TBD


## Garbage Collection

Arrays remain static (created at program start, live until exit), so garbage collection of
entire arrays does not apply. Their string elements are garbage-collected: a string stored
into an element is registered with the collector, and the array's whole element region is a
GC root — a single range in the `@jcc.gc.global.roots` table. So a retained element survives
every collection, while a string it replaces becomes unreachable and is reclaimed on the next
cycle. See the "Garbage collector plumbing" and "Dynamic string memory" notes in
`docs/system/code-generation.md`.
