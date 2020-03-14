# Arrays


## Contents

*   [Overview](#overview)
*   [Main Classes](#main-classes)
*   [Garbage Collection](#garbage-collection)
    *   [Array Elements](#array-elements)
    *   [Entire Arrays](#entire-arrays)
*   [Bounds Checking](#bounds-checking)
*   [Memory Layout](#memory-layout)


## Overview

Arrays in JCC can hold elements of any of the simple types: boolean, float, integer, and string. They can have a single or multiple dimensions. However, at present they can only be static, that is, they must be created when the program starts, and exist until the program ends. The size of an array must also be static, defined by one or more literal values or constants.


## Main Classes

The type `Arr` is used to define the type of an array. An instance of class `ArrayDeclaration` is used to encapsulate the declaration of a single array. Besides a type (that is always an instance of class `Arr`), an `ArrayDeclaration` has a list of subscript expressions -- the dimensions of the array.

![Array Declaration](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/ArrayDeclaration.puml)

The AST class used when accessing an array element is `ArrayAccessExpression`. Besides an `Identifier` that identifies the array, this class also has a list of subscript expressions that specifies the element to access in the array.

![Array Access](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.github.com/dykstrom/jcc/master/docs/diagrams/ArrayAccess.puml)


## Garbage Collection


### Array Elements

Garbage collection of array elements has not yet been implemented, but the planned design is:

For each array that contains dynamically allocated elements (that is, strings), there will be a corresponding array of variable type pointers in the GC section of the stack. Any code that updates an array element must also update the corresponding variable type pointer, and call `memory_register`.


### Entire Arrays

Garbage collection of entire arrays is not applicable as long as there are only static arrays that can never go out of scope.


## Bounds Checking

TBD


## Memory Layout

Arrays are contiguous blocks of memory. If the element type of the array is integer or float, the actual element data is stored inside the array. If the element type is string, the array stores pointers to the actual strings.

Each array needs some meta data accessible in runtime. This data is stored just before the array in memory. In the (64-bit) memory cell immediately before the first array element, the number of dimensions is stored. In the memory cells before that, the size of each dimension is stored, in reverse order. For an array of N dimensions, it looks something like this:

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
