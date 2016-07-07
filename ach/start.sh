#!/bin/bash 

#java -Dlog4j.configuration=file:./log4j.properties -jar ./target/pi5-ach.jar ./eve.yaml &> ach.log &
java -Djava.util.logging.config.file=./log.properties -jar ./target/pi5-ach.jar ./eve.yaml &> ach.log &
