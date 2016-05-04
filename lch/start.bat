@ECHO OFF
REM $Id$
REM $URL$
CLS
CD /d %~dp0
ECHO Running PI-5 Multi-Agent System for Meso/Micro Simulator Local Control Hub.>CON
CALL "java" -Dlog4j.configuration=file:/%~dp0/log4j.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus3.yaml 1>sim-bus3.log 2>&1
CALL "java" -Dlog4j.configuration=file:/%~dp0/log4j.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus5.yaml 1>sim-bus5.log 2>&1
CALL "java" -Dlog4j.configuration=file:/%~dp0/log4j.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus6.yaml 1>sim-bus6.log 2>&1
CALL "java" -Dlog4j.configuration=file:/%~dp0/log4j.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus11.yaml 1>sim-bus11.log 2>&1
IF NOT ["%ERRORLEVEL%"]==["0"] PAUSE
