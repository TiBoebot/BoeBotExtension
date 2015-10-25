REM @echo off
SET PATH="C:\Program Files\java\jdk1.8.0_60\bin";%PATH%
SET PATH=%PATH%;"C:\Program Files\java\jdk1.8.0_11\bin"
cd %~DP0
jar cmf manifest.txt BoeBotExtension.jar com
cd bin
jar uf ../BoeBotExtension.jar *
cd ..
xcopy /y BoeBotExtension.jar "C:\Program Files (x86)\bluej\lib\extensions"
rem svn update BoeBotLib
xcopy /y BoeBotLib\BoeBotLib.jar "C:\Program Files (x86)\bluej\lib\userlib"
pause