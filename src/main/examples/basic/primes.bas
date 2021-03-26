REM Calculate all primes less than a number N

DIM index AS INTEGER
DIM isPrime AS INTEGER
DIM maxIndex as INTEGER
DIM N AS INTEGER
DIM number AS INTEGER
DIM primes(100) AS INTEGER

N = 100
number = 2

WHILE number < N

    REM Check if number is prime
    isPrime = 1
    index = 0
    WHILE isPrime <> 0 AND index < maxIndex
        REM If number is dividable by any prime found so far, it is not prime
        isPrime = number MOD primes(index)
        index = index + 1
    WEND

    REM Print number if prime
    IF isPrime <> 0 THEN
        PRINT number
        primes(maxIndex) = number
        maxIndex = maxIndex + 1
    END IF

    number = number + 1
WEND
