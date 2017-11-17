REM Call some functions

string$ = "BASIC"
length% = len(string$)

print abs(len("") - length%)
print instr(string$, "SI")

value% = -17

print sgn(value%) * abs(value%)
