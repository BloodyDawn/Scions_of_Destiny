@echo off
:start
echo Starting L2Jserver Game Server.
echo.
java -Djava.util.logging.manager=net.sf.l2j.util.L2LogManager -Xmx1024m -cp ./../libs/*;l2jserver.jar net.sf.l2j.gameserver.GameServer
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
