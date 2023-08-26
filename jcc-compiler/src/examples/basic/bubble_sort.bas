' Sort an array of numbers using the bubble sort algorithm

CONST MAX = 100

DIM numbers%(MAX) AS INTEGER

RANDOMIZE timer

' Fill the array with random numbers between 1 and 1000
i% = 0
WHILE i% < MAX
    numbers%(i%) = int(rnd * 1000) + 1
    i% = i% + 1
WEND

PRINT "Unsorted:"
GOSUB printNumbers

' Sort the array
i% = 0
WHILE i% < MAX
    j% = MAX - 1
    WHILE j% > i%
        IF numbers%(j%) < numbers%(j% - 1) THEN
            SWAP numbers%(j%), numbers%(j% - 1)
        END IF
        j% = j% - 1
    WEND
    i% = i% + 1
WEND

PRINT "Sorted:"
GOSUB printNumbers

END

' Description:
'   A subroutine that prints all numbers in the global array numbers%.
' Modifies:
'   s$, x%
printNumbers:

s$ = ""
x% = 0
WHILE x% < ubound(numbers%)
    s$ = s$ + str$(numbers%(x%))
    x% = x% + 1
WEND
PRINT s$

RETURN
