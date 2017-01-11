@echo off
:start
echo Starting L2Jserver Login Server.
echo.
java -Xmx128m -cp ./../libs/*;l2jserver.jar net.sf.l2j.loginserver.LoginServer
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Admin Restart ...
echo.
goto start
:error
echo.
echo Server terminated abnormally
echo.
:end
echo.
echo server terminated
echo.
pause
