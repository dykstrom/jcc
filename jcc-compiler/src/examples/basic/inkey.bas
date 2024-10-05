' -*- basic-qb45 -*-
' Demonstrate function inkey$

DIM key$ AS STRING

PRINT "Press any key to continue (q to quit)"
WHILE key$ <> "q"
    SLEEP
    LET key$ = inkey$()
    PRINT "You pressed: "; key$
WEND
