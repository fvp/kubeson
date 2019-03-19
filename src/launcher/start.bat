@echo off

for /d %%a in ("C:\Program Files (x86)\Java\*1.8*") do set "folder=%%a"

IF ["%folder%"] == [""] (
	echo No Java 8 - 32 bits JVM Found!
	pause
	exit
)

start "" "%folder%\bin\javaw" -jar ulf-viewer-${project.version}-jfx.jar
exit