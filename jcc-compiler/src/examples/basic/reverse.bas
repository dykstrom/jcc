' -*- basic-qb45 -*-
' Demonstrate string reversal using mid$ and len

DIM input$ AS STRING
DIM output$ AS STRING
DIM i AS INTEGER
DIM length AS INTEGER

PRINT "Enter a string to reverse: ";
LINE INPUT input$

LET length = len(input$)
LET output$ = ""
LET i = length

' Build reversed string by extracting characters from end to start
WHILE i >= 1
    LET output$ = output$ + mid$(input$, i, 1)
    LET i = i - 1
WEND

PRINT "Original: "; input$
PRINT "Reversed: "; output$
