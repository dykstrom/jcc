' Sort an array of numbers using the bubble sort algorithm

CONST MAX = 100

DIM numbers%(MAX) AS INTEGER
DIM i%, j%, x% AS INTEGER
DIM s$ AS STRING

RANDOMIZE timer

' Fill the array with random numbers between 1 and 1000
FOR i% = 0 TO MAX - 1
    numbers%(i%) = cint(rnd * 1000.0) + 1
NEXT i%

PRINT "Unsorted:"
GOSUB printNumbers

' Sort the array
FOR i% = 0 TO MAX - 1
    FOR j% = MAX - 1 TO i% + 1 STEP -1
        IF numbers%(j%) < numbers%(j% - 1) THEN
            SWAP numbers%(j%), numbers%(j% - 1)
        END IF
    NEXT j%
NEXT i%

PRINT "Sorted:"
GOSUB printNumbers

END

' Description:
'   A subroutine that prints all numbers in the global array numbers%.
' Modifies:
'   s$, x%
printNumbers:

s$ = ""
FOR x% = 0 TO ubound(numbers%) - 1
    s$ = s$ + str$(numbers%(x%))
NEXT x%
PRINT s$

RETURN
