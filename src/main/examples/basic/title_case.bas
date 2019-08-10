' Convert a string to Title Case

line input "Enter string: "; source$

' Remove any leading or trailing spaces
source$ = ltrim$(rtrim$(source$))

start% = 1
while start% <> 0
    ' Find next space
    end% = instr(start%, source$, " ")
    
    if end% = 0 then
        ' No more spaces, add the last word
        word$ = mid$(source$, start%)
        result$ = result$ + ucase$(mid$(word$, 1, 1)) + lcase$(mid$(word$, 2))
        start% = end%
    else
        ' Found space, add word and space
        word$ = mid$(source$, start%, end% - start%)
        result$ = result$ + ucase$(mid$(word$, 1, 1)) + lcase$(mid$(word$, 2)) + " "
        start% = end%
        
        ' Find next non-space
        while mid$(source$, start%, 1) = " "
            start% = start% + 1
        wend
    end if
wend

print "Source: "; source$
print "Result: "; result$
