if [[ -a .http/run.pid ]]; then echo Already running? Old pid file found; exit; fi;

mkdir -p .http
nohup python -m SimpleHTTPServer > .http/http.output 2>&1 & echo $! > .http/run.pid

echo Started webserver at http://localhost:8000/graphs.html
