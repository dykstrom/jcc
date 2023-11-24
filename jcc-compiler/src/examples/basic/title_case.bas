' -*- basic-qb45 -*-
' Input a string and convert it to Title Case

' This function returns its argument s$, converted to Title Case.
DEF FNtoTitleCase$(s$ AS STRING) = ucase$(mid$(s$, 1, 1)) + lcase$(mid$(s$, 2))

' Main program starts here
LINE INPUT "Enter string: "; source$

' Remove any leading or trailing spaces
source$ = ltrim$(rtrim$(source$))

start% = 1
WHILE start% <> 0
    ' Find next space
    end% = instr(start%, source$, " ")

    IF end% = 0 THEN
        ' No more spaces, add last word
        word$ = mid$(source$, start%)
        result$ = result$ + FNtoTitleCase$(word$)
        start% = end%
    ELSE
        ' Found space, add word and space
        word$ = mid$(source$, start%, end% - start%)
        result$ = result$ + FNtoTitleCase$(word$) + " "
        start% = end%

        ' Find next non-space
        WHILE mid$(source$, start%, 1) = " "
            start% = start% + 1
        WEND
    END IF
WEND

PRINT "Source: "; source$
PRINT "Result: "; result$
