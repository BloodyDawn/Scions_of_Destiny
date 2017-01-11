@echo off
chcp 866>nul
REM New database_installer v1.0
REM Script by Zomb1eKiller

set config_file=vars.txt
set config_version=0

set workdir="%cd%"
set full=0
set stage=0
set logging=0

set upgrade_mode=0
set backup=.
set logdir=.
set safe_mode=1
set cmode=c
set fresh_setup=0
echo language settings
echo -----------------------
echo.
echo Select Your Language:
echo.
echo 1. English
echo 2. Russian (Русский)
echo.
set /P language="Language number: "
echo.
cls
:loadconfig
cls
title Database Installer - Reading Configuration from File...
if not exist %config_file% goto configure
ren %config_file% vars.bat
call vars.bat
ren vars.bat %config_file%
call :colors 17
if /i %config_version% == 2 goto ls_db_ok
set upgrade_mode=2
echo It seems to be the first time you run this version of
echo database_installer but I found a settings file already.
echo I'll hopefully ask this questions just once.
echo.
echo Configuration upgrade options:
echo.
echo 1 Import and continue: I'll read your old settings and
echo     continue execution, but since no new settings will be
echo     saved, you'll see this menu again next time.
echo.
echo 2 Import and configure: This tool has some new available
echo     options, you choose the values that fit your needs
echo     using former settings as a base.
echo.
echo 3 Ignose stored settings: I'll let you configure me
echo     with a fresh set of default values as a base.
echo.
echo 4 View saved settings: See the contents of the config
echo     file.
echo.
echo 5 Quit: Did you came here by mistake?
echo.
set /P upgrade_mode="Type a number, press Enter (default is '%upgrade_mode%'): "
if %upgrade_mode%==1 goto ls_db_ok
if %upgrade_mode%==2 goto configure
if %upgrade_mode%==3 goto configure
if %upgrade_mode%==4 (cls&type %config_file%&pause&goto loadconfig)
if %upgrade_mode%==5 goto :eof
goto loadconfig

:colors
if /i "%cmode%"=="n" (
if not "%1"=="17" (	color F	) else ( color )
) else ( color %1 )
goto :eof

:configure
cls
call :colors 17
title Database Installer - Setup
set config_version=2
if NOT %upgrade_mode% == 2 (
set fresh_setup=1
set lsuser=root
set lspass=
set lsdb=l2jdb
set lshost=localhost
set cbuser=root
set cbpass=
set cbdb=l2jdb
set cbhost=localhost
set gsuser=root
set gspass=
set gsdb=l2jdb
set gshost=localhost
set cmode=c
set backup=.
set logdir=.
)
cls
if %language% == 2 (
echo.
echo Настройки Логин Сервера
echo -----------------------
echo.
set /P lsuser="MySQL Пользователь (стандартно '%lsuser%'): "
set /P lspass="Пароль (стандартно '%lspass%'): "
set /P lsdb="БазаДанных (стандартно '%lsdb%'): "
set /P lshost="Адрес (хост)(стандартно '%lshost%'): "
echo.
) else (
echo.
echo Login Server settings
echo -----------------------
echo.
set /P lsuser="MySQL Username (default is '%lsuser%'): "
set /P lspass="Password (default is '%lspass%'): "
set /P lsdb="Database (default is '%lsdb%'): "
set /P lshost="Host (default is '%lshost%'): "
echo.)
cls
if %language% == 2 (
echo.
echo Настройки Гейм Сервера
echo ----------------------
echo.
set /P gsuser="MySQL Пользователь (стандартно '%gsuser%'): "
set /P gspass="Пароль (стандартно '%gspass%'): "
set /P gsdb="БазаДанных (стандартно '%gsdb%'): "
set /P gshost="Адрес (хост)(стандартно '%gshost%'): "
echo. 
) else (
echo.
echo Game Server settings
echo ----------------------
echo.
set /P gsuser="User (default is '%gsuser%'): "
set /P gspass="Pass (default is '%gspass%'): "
set /P gsdb="Database (default is '%gsdb%'): "
set /P gshost="Host (default is '%gshost%'): "
echo. )
cls
goto safe2

:safe2
cls
echo.
echo @echo off > %config_file%
echo set config_version=%config_version% >> %config_file%
echo set cmode=%cmode%>> %config_file%
echo set safe_mode=%safe_mode% >> %config_file%
echo set lsuser=%lsuser%>> %config_file%
echo set lspass=%lspass%>> %config_file%
echo set lsdb=%lsdb%>> %config_file%
echo set lshost=%lshost% >> %config_file%
echo set gsuser=%gsuser%>> %config_file%
echo set gspass=%gspass%>> %config_file%
echo set gsdb=%gsdb%>> %config_file%
echo set gshost=%gshost%>> %config_file%
echo set logdir=%logdir%>> %config_file%
echo set backup=%backup%>> %config_file%
echo.
if %language% == 2 (
echo Установка скрипта завершена, ваши настройки сохранены
echo в файле '%config_file%'. Внимание: ваши пароли сохранены
echo Как чистый текст.
echo.
) else (
echo Script setup complete, your settings were saved in the
echo '%config_file%' file. Remember: your passwords are stored
echo as clear text.
echo.
)
pause
goto loadconfig

:ls_err1
cls
set lsdbprompt=y
call :colors 47
title Database Installer - Login Server DataBase Backup ERROR!
echo.
if %language% == 2 (
echo Ошибка при backup! Возможно базы данных не существует?
echo Я попытаюсь создать базу данных %lsdb% для вас.
echo.
echo Создавать базу данных логин сервера:
echo.
echo y Да
echo.
echo n Нет
echo.
echo r Перенастройка
echo.
echo q Выход
echo.
) else (
echo Backup attempt failed! A possible reason for this to
echo happen, is that your DB doesn't exist yet. I could
echo try to create %lsdb% for you.
echo.
echo ATTEMPT TO CREATE LOGINSERVER DATABASE:
echo.
echo y Yes
echo.
echo n No
echo.
echo r Reconfigure
echo.
echo q Quit
echo.
)
set /p lsdbprompt=Choose (default yes):
if /i %lsdbprompt%==y goto ls_db_create
if /i %lsdbprompt%==n goto cs_backup
if /i %lsdbprompt%==r goto configure
if /i %lsdbprompt%==q goto end
goto ls_err1

:ls_db_create
cls
call :colors 17
set cmdline=
set stage=2
title Database Installer - Login Server DataBase Creation
echo.
if %language% == 2 (
echo Пытаюсь создать базу данных логин сервера.
) else (
echo Trying to create a Login Server DataBase.
)
set cmdline=mysql.exe -h %lshost% -u %lsuser% --password=%lspass% -e "CREATE DATABASE %lsdb%" 2^> NUL
%cmdline%
if %ERRORLEVEL% == 0 goto ls_db_ok
if %safe_mode% == 1 goto omfg

:ls_err2
cls
set omfgprompt=q
call :colors 47
title Database Installer - Login Server DataBase Creation ERROR!
echo.
if %language% == 2 (
echo Произошла ошибка при создании базы данных для вашего
echo логин сервера.
echo.
echo Возможные причины:
echo 1-Вы ввели не верные данные. Проверьте логин, пароль и т.д.
echo 2-Пользователь %lsuser% не имеет привилегий для
echo создания базы данных. Проверьте ваши привилегии.
echo 3-База данных уже существует...?
echo.
echo c Продолжить
echo.
echo r Перенастройка
echo.
echo q Выход
echo.
) else (
echo An error occured while trying to create a database for
echo your login server.
echo.
echo Possible reasons:
echo 1-You provided innacurate info , check user, password, etc.
echo 2-User %lsuser% don't have enough privileges for
echo database creation. Check your MySQL privileges.
echo 3-Database exists already...?
echo.
echo Unless you're sure that the pending actions of this tool
echo could work, i'd suggest you to look for correct values
echo and try this script again later.
echo.
echo c Continue running
echo.
echo r Reconfigure
echo.
echo q Quit now
echo.)
set /p omfgprompt=Choose (default quit):
if /i %omfgprompt%==c goto cs_backup
if /i %omfgprompt%==r goto configure
if /i %omfgprompt%==q goto end
goto ls_err2

:ls_db_ok
cls
set loginprompt=u
call :colors 17
title Database Installer - Login Server DataBase WARNING!
echo.
if %language% == 2 (
echo Тип установки Базы Данных логин сервера:
echo.
echo f Полная: Все данные в базе данных будут стерты
echo.
echo u Обновление: Запись поверх данных.
echo.
echo s Пропустить: Перейти к установке гейм севрера
echo.
echo r Перенастройка: Вы можете изменить логин, пароль,
echo     базу данных и начать заного с новыми данными
echo.
echo q Выход
echo.
) else (
echo LOGINSERVER DATABASE install type:
echo.
echo f Full: WARNING! I'll destroy ALL of your existing login
echo     data.
echo.
echo u Upgrade: I'll do my best to preserve all login data.
echo.
echo s Skip: I'll take you to the game server database
echo     installation and upgrade options.
echo.
echo r Reconfigure: You'll be able to redefine MySQL path,
echo     user and database information and start over with
echo     those fresh values.
echo.
echo q Quit
echo.
)
set /p loginprompt=Choose (default upgrade):
if /i %loginprompt%==f goto ls_cleanup
if /i %loginprompt%==u goto ls_upgrade
if /i %loginprompt%==s goto gs_db_ok
if /i %loginprompt%==r goto configure
if /i %loginprompt%==q goto end
goto ls_db_ok

:ls_cleanup
call :colors 17
set cmdline=
title Database Installer - Login Server DataBase Full Install
echo.
if %language% == 2 (
echo Удаляю старые таблицы...
) else (
echo Deleting Login Server tables for new content.
)
set cmdline=mysql.exe -h %lshost% -u %lsuser% --password=%lspass% -D %lsdb% ^< ls_cleanup.sql 2^> NUL
%cmdline%
if not %ERRORLEVEL% == 0 goto omfg
set full=1
echo.
if %language% == 2 (
echo Таблицы успешно удалены.
) else (
echo Login Server tables has been deleted.
)
goto ls_install

:ls_upgrade
cls
echo.
if %language% == 2 (
echo Обновление базы данных логин сервера
) else (
echo Upgrading structure of Login Server tables.)
echo.
echo @echo off> temp.bat
if exist ls_errors.log del ls_errors.log
for %%i in (..\sql\login\updates\*.sql) do echo mysql.exe -h %lshost% -u %lsuser% --password=%lspass% -D %lsdb% --force ^< %%i 2^>^> ls_errors.log >> temp.bat
call temp.bat> nul
del temp.bat
move ls_errors.log %workdir%
goto ls_install

:ls_install
cls
set cmdline=
if %full% == 1 (
title Database Installer - Login Server DataBase Installing...
echo.
if %language% == 2 (
echo Установка базы данных логин сервера.
) else (
echo Installing new Login Server content.)
echo.
) else (
title Database Installer - Login Server DataBase Upgrading...
echo.
if %language% == 2 (
echo Обновление базы данных логин сервера.
) else (
echo Upgrading Login Server content.)
echo.
)
if %logging% == 0 set output=NUL
set dest=ls
for %%i in (..\sql\login\*.sql) do call :dump %%i

echo done...
echo.
goto gs_db_ok

:gs_err1
cls
set gsdbprompt=y
call :colors 47
title Database Installer - Game Server DataBase Backup ERROR!
echo.
if %language% == 2 (
echo Ошибка при backup! Возможно базы данных не существует?
echo Я попытаюсь создать базу данных %lsdb% для вас.
echo.
echo Создать новую базу данных:
echo.
echo y Да
echo.
echo n Нет
echo.
echo r Перенастройка
echo.
echo q Выход
echo.
) else (
echo Backup attempt failed! A possible reason for this to
echo happen, is that your DB doesn't exist yet. I could
echo try to create %gsdb% for you, but maybe you prefer to
echo continue with last part of the script.
echo.
echo ATTEMPT TO CREATE GAME SERVER DATABASE?
echo.
echo y Yes
echo.
echo n No
echo.
echo r Reconfigure
echo.
echo q Quit
echo.)
set /p gsdbprompt=Choose (default yes):
if /i %gsdbprompt%==y goto gs_db_create
if /i %gsdbprompt%==n goto eof
if /i %gsdbprompt%==r goto configure
if /i %gsdbprompt%==q goto end
goto gs_err1

:gs_db_create
cls
call :colors 17
set stage=6
set cmdline=
title Database Installer - Game Server DataBase Creation
echo.
if %language% == 2 (
echo Пытаюсь создать базу данных гейм сервера)
else (
echo Trying to create a Game Server DataBase..
)
set cmdline=mysql.exe -h %gshost% -u %gsuser% --password=%gspass% -e "CREATE DATABASE %gsdb%" 2^> NUL
%cmdline%
if %ERRORLEVEL% == 0 goto gs_db_ok
if %safe_mode% == 1 goto omfg

:gs_err2
cls
set omfgprompt=q
call :colors 47
title Database Installer - Game Server DataBase Creation ERROR!
echo.
if %language% == 2 (
echo Произошла ошибка при создании базы данных для вашего
echo гейм сервера.
echo.
echo Возможные причины:
echo 1-Вы ввели не верные данные. Проверьте логин, пароль и т.д.
echo 2-Пользователь %gsuser% не имеет привилегий для
echo создания базы данных. Проверьте ваши привилегии.
echo 3-База данных уже существует...?
echo.
echo r Перенастройка
echo.
echo q Выход
echo.
) else (
echo An error occured while trying to create a database for
echo your Game Server.
echo.
echo Possible reasons:
echo 1-You provided innacurate info, check username, pass, etc.
echo 2-User %gsuser% don't have enough privileges for
echo database creation.
echo 3-Database exists already...?
echo.
echo I'd suggest you to look for correct values and try this
echo script again later. But you can try to reconfigure it now.
echo.
echo r Reconfigure
echo.
echo q Quit now
echo.
)
set /p omfgprompt=Choose (default quit):
if /i %omfgprompt%==r goto configure
if /i %omfgprompt%==q goto end
goto gs_err2

:gs_db_ok
cls
set installtype=u
call :colors 17
title Database Installer - Game Server DataBase WARNING!
echo.
if %language% == 2 (
echo Установка базы данных гейм сервера:
echo.
echo f Полная: Внимание! Я уничтожу все данные
echo.
echo u Обновление.
echo.
echo q Выход
echo.
) else (
echo GAME SERVER DATABASE install:
echo.
echo f Full: WARNING! I'll destroy ALL of your existing character
echo     data (i really mean it: items, pets.. ALL)
echo.
echo u Upgrade: I'll do my best to preserve all of your character
echo     data.
echo.
echo q Quit
)
set /p installtype=Choose (default upgrade):
if /i %installtype%==f goto gs_cleanup
if /i %installtype%==u goto gs_upgrade
if /i %installtype%==q goto end
goto gs_db_ok

:gs_cleanup
call :colors 17
set cmdline=
title Database Installer - Game Server DataBase Full Install
echo.
if %language% == 2 (
echo Удаляю все таблицы...
) else (
echo Deleting all Game Server tables for new content.
)
set cmdline=mysql.exe -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% ^< gs_cleanup.sql 2^> NUL
%cmdline%
if not %ERRORLEVEL% == 0 goto omfg
set full=1
echo.
if %language% == 2 (
echo Таблицы удалены.
) else (
echo Game Server tables has been deleted.
)
goto gs_install

:gs_upgrade
cls
echo.
if %language% == 2 (
echo Обновляю таблицы гейм сервера.
) else (
echo Upgrading structure of Game Server tables (this could take awhile, be patient).)
echo.
echo @echo off> temp.bat
if exist gs_errors.log del gs_errors.log
for %%i in (..\sql\game\updates\*.sql) do echo mysql.exe -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% --force ^< %%i 2^>^> gs_errors.log >> temp.bat
call temp.bat> nul
del temp.bat
move gs_errors.log %workdir%
goto gs_install

:gs_install
cls
set cmdline=
if %full% == 1 (
title Database Installer - Game Server DataBase Installing...
echo.
if %language% == 2 (
echo Установка новых таблиц...
) else (
echo Installing new Game Server content.)
echo.
) else (
title Database Installer - Game Server DataBase Upgrading...
echo.
if %language% == 2 (
echo Обновление таблиц...
) else (
echo Upgrading Game Server content.)
echo.
)
if %logging% == 0 set output=NUL
set dest=gs
for %%i in (..\sql\game\*.sql) do call :dump %%i

echo done...
echo.
goto end

:dump
set cmdline=
if /i %full% == 1 (set action=Installing) else (set action=Upgrading)
echo %action% %1>>"%output%"
echo %action% %~nx1
if "%dest%"=="ls" set cmdline=mysql.exe -h %lshost% -u %lsuser% --password=%lspass% -D %lsdb% ^< %1 2^>^>"%output%"
if "%dest%"=="cb" set cmdline=mysql.exe-h %cbhost% -u %cbuser% --password=%cbpass% -D %cbdb% ^< %1 2^>^>"%output%"
if "%dest%"=="gs" set cmdline=mysql.exe -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% ^< %1 2^>^>"%output%"
%cmdline%
if %logging%==0 if NOT %ERRORLEVEL%==0 call :omfg2 %1
goto :eof

:omfg2
cls
set ntpebcak=c
call :colors 47
title Database Installer - Potential DataBase Issue at stage %stage%
echo.
if %language% == 2 (
echo Проблема при выполнении команды :
echo mysql.exe -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb%
echo.
echo в файле %~nx1
echo.
echo Что вы хотите сделать?
echo.
echo l Логировать: Я создам лог файл с ошибкойи продолжу
echo.
echo c Продолжить: Сделать вид что ничего не было :)
echo.
echo r Перенастройка
echo.
echo q Выход
echo.
) else (
echo Something caused an error while executing instruction :
echo mysql.exe -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb%
echo.
echo with file %~nx1
echo.
echo What we should do now?
echo.
echo l Log it: I will create a log for this file, then continue
echo     with the rest of the list in non-logging mode.
echo.
echo c Continue: Let's pretend that nothing happened and continue with
echo     the rest of the list.
echo.
echo r Reconfigure: Perhaps these errors were caused by a typo.
echo     you can restart from scratch and redefine paths, databases
echo     and user info again.
echo.
echo q Quit now
echo.)
set /p ntpebcak=Choose (default continue):
if /i %ntpebcak%==c (call :colors 17 & goto :eof)
if /i %ntpebcak%==l (call :logginon %1 & goto :eof)
if /i %ntpebcak%==r (call :configure & exit)
if /i %ntpebcak%==q (call :end)
goto omfg2

:logginon
cls
call :colors 17
title Database Installer - Game Server Logging Options turned on
set logging=1
if %full% == 1 (
  set output=%logdir%\install-%~nx1.log
) else (
  set output=%logdir%\upgrade-%~nx1.log
)
echo.
if %language% == 2 (
echo По вашему запросу я создам LOG файл.
echo.
echo Я назову его %output%
echo.
echo Если у вас существует такой файл, удалите его иначе ничего не произойдет.
echo.
pause
set cmdline=mysql.exe -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% ^<..\sql\%1 2^>^>"%output%"
date /t >"%output%"
time /t >>"%output%"
%cmdline%
echo Лог файл создан, продолжаю прошлую операцию...
) else (
echo Per your request, i'll create a log file for your reading pleasure.
echo.
echo I'll call it %output%
echo.
echo If you already have such a file and would like to keep a copy.
echo go now and read it or back it up, because it's not going to be rotated
echo or anything, instead i'll just overwrite it.
echo.
pause
set cmdline=mysql.exe -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% ^<..\sql\%1 2^>^>"%output%"
date /t >"%output%"
time /t >>"%output%"
%cmdline%
echo Log file created, resuming normal operations...
)
call :colors 17
set logging=0
set output=NUL
goto :eof

:omfg
set omfgprompt=q
call :colors 57
cls
title Database Installer - Potential PICNIC detected at stage %stage%
echo.
if %language% == 2 (
echo Проблема при выполнении команды:
echo.
echo "%cmdline%"
echo.
echo Проверьте данные, возможно они не верны и 
echo загрузите скрипт позже.
if %stage% == 1 set label=ls_err1
if %stage% == 2 set label=ls_err2
if %stage% == 3 set label=cs_err1
if %stage% == 4 set label=cs_err2
if %stage% == 5 set label=gs_err1
if %stage% == 6 set label=gs_err2
echo.
echo c Продолжить выполнение скрипта
echo.
echo r Перенастройка
echo.
echo q Выход
echo.
) else (
echo There was some problem while executing:
echo.
echo "%cmdline%"
echo.
echo I'd suggest you to look for correct values and try this
echo script again later. But maybe you'd prefer to go on now.
if %stage% == 1 set label=ls_err1
if %stage% == 2 set label=ls_err2
if %stage% == 3 set label=cs_err1
if %stage% == 4 set label=cs_err2
if %stage% == 5 set label=gs_err1
if %stage% == 6 set label=gs_err2
echo.
echo c Continue running the script
echo.
echo r Reconfigure
echo.
echo q Quit now
echo.
)

set /p omfgprompt=Choose (default quit):
if /i %omfgprompt%==c goto %label%
if /i %omfgprompt%==r goto configure
if /i %omfgprompt%==q goto end
goto omfg

:end
call :colors 17
title Database Installer - Finished installation
cls
echo.
echo Database Installer - Finished installation
echo.
echo 2011-2012 Festina Dev. Team
echo.
pause