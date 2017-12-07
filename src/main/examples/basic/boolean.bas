00 REM Test boolean expressions

10 LET a% = 5 + 2
20 LET b% = a% - 2
30 LET c = a% > b%
40 LET d = a% < b%
50 LET e = c AND d
60 PRINT "e = "; e; ", e OR TRUE = "; e OR TRUE
70 PRINT "(c OR d) AND a% <> b% AND TRUE = "; (c OR d) AND a% <> b% AND TRUE
80 PRINT "NOT c XOR d OR e = "; NOT c XOR d OR e
90 END
