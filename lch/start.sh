#!/bin/bash 

java -Dlog4j.configuration=file:log4j.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus3.yaml &> sim-bus3.log &
java -Dlog4j.configuration=file:log4j.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus5.yaml &> sim-bus5.log &
java -Dlog4j.configuration=file:log4j.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus6.yaml &> sim-bus6.log &
java -Dlog4j.configuration=file:log4j.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus11.yaml &> sim-bus11.log &

