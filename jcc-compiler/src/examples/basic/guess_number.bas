' Let the user guess a random number between 1 and 100

dim guess as integer
dim numberOfGuesses as integer
dim s as string
dim secret as integer

randomize timer
' Generate a random number between 1 and 100
secret = int(rnd * 100) + 1
guess = -1
numberOfGuesses = 0

' While guess is not correct, keep on asking
while guess <> secret
    line input "Please guess a number between 1 and 100: "; s
    guess = val(s)    
    
    if guess < secret then
        print "Too low!"
    else if guess > secret then
        print "Too high!"
    else 
        print "Right you are!"
    end if

    numberOfGuesses = numberOfGuesses + 1
wend

print "You needed "; numberOfGuesses; " guesses to get it right!"
