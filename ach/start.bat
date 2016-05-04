@ECHO OFF
REM $Id$
REM $URL$
CLS
CD /d %~dp0
ECHO Running INERTIA Prosumer Optimization ^& Control Multi-Agent System for the Aggregator Control Hub with settings:>CON
ECHO.>CON
MORE eve.yaml >CON
CALL "java" -Dlog4j.configuration=file:/%~dp0/log4j.properties -jar ./pi5-ach.jar ./eve.yaml 1>ach.log 2>&1
IF NOT ["%ERRORLEVEL%"]==["0"] PAUSE