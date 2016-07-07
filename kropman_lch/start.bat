@ECHO OFF
REM $Id$
REM $URL$
CLS
CD /d %~dp0
ECHO Running PI-5 Multi-Agent System for Kropman Insiteview Local Control Hub.>CON
CALL "java" -Dlog4j.configuration=file:/%~dp0/log.properties -jar ./target/pi5-kropman-lch.jar ./eve.yaml 1> kropman.log 2>&1
IF NOT ["%ERRORLEVEL%"]==["0"] PAUSE
