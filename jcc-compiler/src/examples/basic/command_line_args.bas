' -*- basic-qb45 -*-
' Parse and print command line args

DIM argc AS INTEGER
DIM argv(100) AS STRING
DIM args AS STRING
DIM first AS INTEGER, last AS INTEGER, i AS INTEGER

' Remove any leading or trailing spaces from the command line
args = ltrim$(rtrim$(command$()))
argc = 0

' Parse command line args
first = 1
WHILE first
    ' Find next space
    last = instr(first, args, " ")

    IF last = 0 THEN
        ' No more spaces, add last arg
        argv(argc) = mid$(args, first)
        argc = argc + 1
        first = last
    ELSE
        ' Found space, add arg and continue
        argv(argc) = mid$(args, first, last - first)
        argc = argc + 1
        first = last

        ' Find next non-space
        WHILE mid$(args, first, 1) = " "
            first = first + 1
        WEND
    END IF
WEND

PRINT
PRINT args
PRINT

' Print the parsed args
LET i = 0
WHILE i < argc
    PRINT "Arg "; i; " = '"; argv(i); "'"
    i = i + 1
WEND
