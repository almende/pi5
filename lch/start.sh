#!/bin/bash 

java -Djava.util.logging.config.file=./log.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus3.yaml &> sim-bus3.log &
java -Djava.util.logging.config.file=./log.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus5.yaml &> sim-bus5.log &
java -Djava.util.logging.config.file=./log.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus6.yaml &> sim-bus6.log &
java -Djava.util.logging.config.file=./log.properties -jar ./target/pi5-lch.jar ./simulation/eve_bus11.yaml &> sim-bus11.log &
