' Let the user guess a random number between 1 and 100

DIM guess AS INTEGER
DIM numberOfGuesses AS INTEGER
DIM s AS STRING
DIM secret AS INTEGER

RANDOMIZE timer
CLS

' Generate a random number between 1 and 100
secret = int(rnd * 100) + 1
guess = -1
numberOfGuesses = 0

' While guess is not correct, keep on asking
WHILE guess <> secret
    LINE INPUT "Please guess a number between 1 and 100: "; s
    guess = val(s)    
    
    IF guess < secret THEN
        PRINT "Too low!"
    ELSE IF guess > secret THEN
        PRINT "Too high!"
    ELSE 
        PRINT "Right you are!"
    END IF

    numberOfGuesses = numberOfGuesses + 1
WEND

PRINT "You needed "; numberOfGuesses; " guesses to get it right!"
