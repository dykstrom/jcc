# Arrays


## Contents

*   [Overview](#overview)
*   [Main Classes](#main-classes)
*   [Garbage Collection](#garbage-collection)
    *   [Array Elements](#array-elements)
    *   [Entire Arrays](#entire-arrays)
*   [Bounds Checking](#bounds-checking)
*   [Option Base](#option-base)
*   [Memory Layout](#memory-layout)


## Overview

Arrays in JCC can hold elements of the simple types: boolean, float, integer, and string. They can have a single or multiple dimensions. However, at present they can only be static, that is, they must be created when the program starts, and exist until the program ends. The size of an array must also be static, defined by one or more literal values or constants.


## Main Classes

An instance of class `Arr` is used to represent the type of an array. An instance of class `ArrayDeclaration` is used to encapsulate the declaration of a single array. Besides a type (that is always an instance of class `Arr`), an `ArrayDeclaration` has a list of subscript expressions -- the dimensions of the array.

![Array Declaration](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/ArrayDeclaration.puml)

The AST class used when accessing an array element is `ArrayAccessExpression`. Besides an `Identifier` that identifies the array, this class also has a list of subscript expressions that specifies the element to access in the array.

![Array Access](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/ArrayAccess.puml)


## Garbage Collection


### Array Elements

For each array that contains dynamically allocated elements (that is, strings), there is a corresponding array of variable type pointers in the GC section of the stack. Any code that updates an array element also updates the corresponding variable type pointer.


### Entire Arrays

Garbage collection of entire arrays is not applicable as long as there are only static arrays that can never go out of scope.


## Option Base

The array subscripts are 0-based by default. The Basic module of JCC supports the OPTION BASE 
statement that can make the subscripts 1-based instead of 0-based. The only allowed bases are 
0 (the default) and 1. If the array subscript base is set to 1, all subscript expressions are 
wrapped in a `SubExpression` that subtracts 1 from the original subscript expression before 
accessing an array element.


## Bounds Checking

TBD


## Memory Layout

Arrays are contiguous blocks of memory. If the element type of the array is integer or float, the actual element data is stored inside the array. If the element type is string, the array stores pointers to the actual strings.

Each array needs some metadata accessible in runtime. This data is stored just before the array in memory. In the (64-bit) memory cell immediately before the first array element, the number of dimensions is stored. In the memory cells before that, the size of each dimension is stored, in reverse order. For an array of N dimensions, it looks something like this:

```
   Size of dimension N
   ...
   Size of dimension 2
   Size of dimension 1
   Number of dimensions
   Array element 0
   Array element 1
   ...
```

The data of a single-dimension array is just a sequence of array elements in memory. The data of
a multi-dimension array has a layout that depends on the number of dimensions. Below are examples
of arrays of dimension 1, 2, and 3.

A one-dimensional array a(5):

```
   0 1 2 3 4
```

The exact location of an element in a one-dimensional array is calculated like:

```
   Code      Memory Location     Element
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

The algorithm for calculating the location of an array element can be described like this is
pseudo code:

```
   Initialize the index to 0
   For each dimension:
      Multiply the index by the size of the dimension
      Evaluate the subscript expression for the dimension
      Add the evaluated value to the index
```
