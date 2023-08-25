' Print messages in different colors

DIM esc AS STRING
DIM black AS STRING, red AS STRING, green AS STRING, yellow AS STRING
DIM blue AS STRING, magenta AS STRING, cyan AS STRING, white AS STRING
DIM inverse AS STRING, normal AS STRING

esc = chr$(27)

black   = esc + "[30m"
red     = esc + "[31m"
green   = esc + "[32m"
yellow  = esc + "[33m"
blue    = esc + "[34m"
magenta = esc + "[35m"
cyan    = esc + "[36m"
white   = esc + "[37m"
inverse = esc + "[7m"
normal  = esc + "[0m"

PRINT black;   "BLACK";   normal
PRINT red;     "RED";     normal
PRINT green;   "GREEN";   normal
PRINT yellow;  "YELLOW";  normal
PRINT blue;    "BLUE";    normal
PRINT magenta; "MAGENTA"; normal
PRINT cyan;    "CYAN";    normal
PRINT white;   "WHITE";   normal
PRINT inverse; "INVERSE"; normal
