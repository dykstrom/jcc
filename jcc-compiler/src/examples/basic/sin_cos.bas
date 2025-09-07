' Print sin(x) and cos(x) for a number of angles

' TODO: Restore to original when automatic type casting has been implemented.

DIM PI AS DOUBLE, rad AS DOUBLE
DIM angle AS DOUBLE

PI = 4.0 * atn(1)

angle = 0.0
WHILE angle <= 360.0
    ' Convert angle to radians
    rad = angle * PI / 180.0
    PRINT "sin("; angle; ")="; sin(rad); ", cos("; angle; ")="; cos(rad)
    angle = angle + 30.0
WEND
