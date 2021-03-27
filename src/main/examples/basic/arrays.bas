' Calculates the min, max and average of numbers in an array

dim array(9999) as double
dim min as double, max as double, avg as double, sum as double
dim index as integer, N as integer

N = 10000

randomize timer

' Fill the array with N random numbers between 0 and 1
index = 0
while index < N
    array(index) = rnd
    index = index + 1
wend

min = 1.0
max = 0.0
sum = 0.0

index = 0
while index < N
    if array(index) < min then min = array(index)
    if array(index) > max then max = array(index)
    sum = sum + array(index)
    index = index + 1
wend

print "Min: "; min
print "Max: "; max
print "Avg: "; sum / N
