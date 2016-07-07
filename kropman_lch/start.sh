#!/bin/bash 

java -Djava.util.logging.config.file=./log.properties -jar ./target/pi5-kropman-lch.jar ./eve.yaml &> kropman-lch.log &
