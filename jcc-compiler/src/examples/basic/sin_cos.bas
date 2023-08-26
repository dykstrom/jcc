' Print sin(x) and cos(x) for a number of angles

DIM PI AS DOUBLE, rad AS DOUBLE
DIM angle AS INTEGER

PI = 4.0 * atn(1)

angle = 0
WHILE angle <= 360
    ' Convert angle to radians
    rad = angle * PI / 180
    PRINT "sin("; angle; ")="; sin(rad); ", cos("; angle; ")="; cos(rad)
    angle = angle + 30    
WEND
