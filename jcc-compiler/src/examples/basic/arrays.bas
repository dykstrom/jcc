' Calculate the min, max, and average of numbers in an array

CONST N = 10000

DIM array(N) AS DOUBLE
DIM min AS DOUBLE, max AS DOUBLE, sum AS DOUBLE
DIM index AS INTEGER

RANDOMIZE timer

' Fill the array with N random numbers between 0 and 1
index = 0
WHILE index < N
    array(index) = rnd
    index = index + 1
WEND

min = 1.0
max = 0.0
sum = 0.0

index = 0
WHILE index < N
    IF array(index) < min THEN min = array(index)
    IF array(index) > max THEN max = array(index)
    sum = sum + array(index)
    index = index + 1
WEND

PRINT "Min: "; min
PRINT "Max: "; max
PRINT "Avg: "; sum / N
