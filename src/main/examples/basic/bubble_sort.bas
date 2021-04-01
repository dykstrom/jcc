' Sorts an array of numbers using the bubble sort algorithm

dim numbers%(99) as integer

N% = 100

randomize timer

' Fill the array with random numbers between 1 and 1000
i% = 0
while i% < N%
    numbers%(i%) = int(rnd * 1000) + 1
    i% = i% + 1
wend

print "Unsorted:"
gosub printNumbers

' Sort the array
i% = 0
while i% < N%
    j% = N% - 1
    while j% > i%
        if numbers%(j%) < numbers%(j% - 1) then
            swap numbers%(j%), numbers%(j% - 1)
        endif
        j% = j% - 1
    wend
    i% = i% + 1
wend

print "Sorted:"
gosub printNumbers

end

' Description:
'   A subroutine that prints all numbers in the global array numbers%.
' Modifies:
'   s$, x%
printNumbers:

s$ = ""
x% = 0
while x% < N%
    s$ = s$ + str$(numbers%(x%))
    x% = x% + 1
wend
print s$

return
