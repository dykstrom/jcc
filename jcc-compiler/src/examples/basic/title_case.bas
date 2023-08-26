' Convert a string to Title Case

LINE INPUT "Enter string: "; source$

' Remove any leading or trailing spaces
source$ = ltrim$(rtrim$(source$))

start% = 1
WHILE start% <> 0
    ' Find next space
    end% = instr(start%, source$, " ")
    
    IF end% = 0 THEN
        ' No more spaces, add the last word
        word$ = mid$(source$, start%)
        result$ = result$ + ucase$(mid$(word$, 1, 1)) + lcase$(mid$(word$, 2))
        start% = end%
    ELSE
        ' Found space, add word and space
        word$ = mid$(source$, start%, end% - start%)
        result$ = result$ + ucase$(mid$(word$, 1, 1)) + lcase$(mid$(word$, 2)) + " "
        start% = end%
        
        ' Find next non-space
        WHILE mid$(source$, start%, 1) = " "
            start% = start% + 1
        WEND
    END IF
WEND

PRINT "Source: "; source$
PRINT "Result: "; result$
