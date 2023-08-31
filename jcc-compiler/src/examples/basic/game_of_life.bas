' -*- basic-qb45 -*-
' Simulate Conway's Game of Life

CONST WIDTH = 60, HEIGHT = 30

DIM board(WIDTH, HEIGHT) AS INTEGER, buffer(WIDTH, HEIGHT) AS INTEGER
DIM x AS INTEGER, y AS INTEGER, xx AS INTEGER, yy AS INTEGER
DIM generation AS INTEGER, count AS INTEGER
DIM str AS STRING, separator AS STRING
DIM t AS DOUBLE

separator = string$(WIDTH, "-")

CLS
    
' Populate board
GOSUB initializeRandom
'GOSUB initializeBlinker
'GOSUB initializeBeacon
'GOSUB initializeGlider

' Loop for a number of generations
' Display the board and evolve the cells into a new generation
generation = 0
WHILE generation < 5000
    ' Move cursor to home location
    PRINT chr$(27); "[H"
    PRINT "Generation "; generation
    GOSUB printBoard
    GOSUB evolveBoard
    generation = generation + 1
    ' Sleep for 0.5 seconds
    t = timer
    WHILE timer < t + 0.5
    WEND
WEND

' End of main program
END


' Description:
'   A subroutine that initializes the board with random cells.
' Modifies:
'   x, y, board
initializeRandom:

RANDOMIZE timer

y = 0
WHILE y < HEIGHT
    x = 0
    WHILE x < WIDTH
        IF rnd > 0.5 THEN board(x, y) = 1
        x = x + 1
    WEND
    y = y + 1
WEND

RETURN


' Description:
'   A subroutine that initializes the board with a "blinker".
' Modifies:
'   board
initializeBlinker:

board(0, 1) = 1
board(1, 1) = 1
board(2, 1) = 1

RETURN


' Description:
'   A subroutine that initializes the board with a "beacon".
' Modifies:
'   board
initializeBeacon:

board(0, 0) = 1
board(1, 0) = 1
board(0, 1) = 1
board(1, 1) = 1

board(2, 2) = 1
board(3, 2) = 1
board(2, 3) = 1
board(3, 3) = 1

RETURN


' Description:
'   A subroutine that initializes the board with a "glider".
' Modifies:
'   board
initializeGlider:

board(1, 0) = 1
board(2, 1) = 1
board(2, 2) = 1
board(1, 2) = 1
board(0, 2) = 1

RETURN


' Description:
'   A subroutine that evolves all the cells on the board into a new generation.
' Modifies:
'   x, y, board, buffer
evolveBoard:

' Iterate over all cells, evolve each cell, and store the result in a buffer
y = 0
WHILE y < HEIGHT
    x = 0
    WHILE x < WIDTH
        GOSUB evolveCell
        x = x + 1
    WEND
    y = y + 1
WEND

' Copy buffer back to board
y = 0
WHILE y < HEIGHT
    x = 0
    WHILE x < WIDTH
        board(x, y) = buffer(x, y)
        x = x + 1
    WEND
    y = y + 1
WEND

RETURN


' Description:
'   A subroutine that evolves a single cell, storing the result in 'buffer'.
' Modifies:
'   buffer, count, xx, yy
evolveCell:

count = 0

' Check above row
xx = x - 1
yy = y - 1
GOSUB countIfLive
xx = x
yy = y - 1
GOSUB countIfLive
xx = x + 1
yy = y - 1
GOSUB countIfLive

' Check same row
xx = x - 1
yy = y
GOSUB countIfLive
' Do not count yourself
xx = x + 1
yy = y
GOSUB countIfLive

' Check below row
xx = x - 1
yy = y + 1
GOSUB countIfLive
xx = x
yy = y + 1
GOSUB countIfLive
xx = x + 1
yy = y + 1
GOSUB countIfLive

' Live cells with two or three neighbours survive
' Dead cells with three neighbours are reborn
' Other cells die
IF count = 2 AND board(x, y) = 1 THEN
    buffer(x, y) = 1
ELSE IF count = 3 THEN
    buffer(x, y) = 1
ELSE
    buffer(x, y) = 0
END IF

RETURN


' Description:
'   A subroutine that increases 'count' if the cell specified by 'xx' and 'yy' 
'   is on the board and is live.
' Modifies:
'   count
countIfLive:

IF xx >= 0 AND xx < WIDTH AND yy >= 0 AND yy < HEIGHT AND board(xx, yy) = 1 THEN
    count = count + 1
END IF

RETURN


' Description:
'   A subroutine that prints the board.
' Modifies:
'   x, y, str
printBoard:

PRINT separator
y = 0
WHILE y < HEIGHT
    str = ""
    x = 0
    WHILE x < WIDTH
        IF board(x, y) THEN
            str = str + "O"
        ELSE
            str = str + " "
        END IF
        x = x + 1
    WEND
    PRINT str
    y = y + 1
WEND
PRINT separator

RETURN
