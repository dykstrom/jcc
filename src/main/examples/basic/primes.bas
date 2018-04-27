REM Calculate all primes less than a number N
N = 100

number = 2
WHILE number < N

    REM Check if number is prime
    isPrime = 1
    divisor = 2
    WHILE isPrime = 1 AND divisor <= number \ 2
        REM If number is dividable by divisor, it is not prime
        IF number MOD divisor = 0 THEN
            isPrime = 0
        END IF
        divisor = divisor + 1
    WEND

    REM Print prime number
    IF isPrime = 1 THEN
        PRINT number
    END IF

    number = number + 1
WEND
