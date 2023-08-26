' Print current date and time in ISO format

usDate$ = date$
isoTime$ = time$
GOSUB usDateToIsoDate
PRINT isoDate$; " "; isoTime$
END

' Description:
'   A subroutine that converts a date in US date format (MM-dd-yyyy)
'   to ISO date format (yyyy-MM-dd).
' Arguments:
'   usDate$  (IN)  the date in US date format
'   isoDate$ (OUT) the date in ISO date format
' Modifies:
'   day$, month$, year$
usDateToIsoDate:
day$ = mid$(usDate$, 4, 2)
month$ = left$(usDate$, 2)
year$ = right$(usDate$, 4)
isoDate$ = year$ + "-" + month$ + "-" + day$
RETURN
