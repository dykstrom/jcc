' Print sin(x) and cos(x) for a number of angles

dim PI as double, rad as double
dim angle as integer

PI = 4.0 * atn(1)

angle = 0
while angle <= 360
    ' Convert angle to radians
    rad = angle * PI / 180
    print "sin("; angle; ")="; sin(rad); ", cos("; angle; ")="; cos(rad)
    angle = angle + 30    
wend
