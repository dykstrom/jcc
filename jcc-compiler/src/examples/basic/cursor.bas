' -*- basic-qb45 -*-
' Demonstrate functions csrlin and pos

DIM row AS INTEGER
DIM col AS INTEGER

' Clear the screen
CLS

' Position cursor at row 5, column 10 using ANSI escape codes
' ANSI code format: ESC[row;colH
PRINT chr$(27); "[5;10H";

' Get current cursor position
LET row = csrlin()
LET col = pos(0)

' Print the result at current position
PRINT "Cursor position: row "; row; ", col "; col
