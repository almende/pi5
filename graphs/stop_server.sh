if [[ -a .http/run.pid ]]; then {
echo killing process `cat .http/run.pid`;
if kill `cat .http/run.pid`; then rm .http/run.pid; fi;
}; else echo No PID file found; fi
