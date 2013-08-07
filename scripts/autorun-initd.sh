#!/bin/sh
#
# init script for Plovr
# based on http://gustavostraube.wordpress.com/2009/11/05/writing-an-init-script-for-a-java-application/
#
### BEGIN INIT INFO
#
# Provides:          plovr
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Plovr https server
# Description:       Plovr https server
#
### END INIT INFO

DIR="$(dirname "$(readlink -f "$0")")"
CONFIG_DIR='~/plovr/'
PLOVR=$DIR'/../build/plovr.jar'
LOG='/var/log/plovr'
ARGS='serve --jks '$CONFIG_DIR'/plovr.jks --passphrase <passphrase> '$CONFIG_DIR'/*.json'

# Check the application status
#
# This function checks if the application is running
check_status() {

  # Running ps with some arguments to check if the PID exists
  s=`ps -e -o pid,cmd --sort cmd | grep "java -jar $PLOVR" | grep -v "grep" | tail -n1 | awk '{ print $1 }'`

  # If somethig was returned by the ps command, this function returns the PID
  if [ $s ] ; then
    return $s
  fi

  # In any another case, return 0
  return 0

}

# Starts the application
start() {

  # At first checks if the application is already started calling the check_status
  # function
  check_status

  # $? is a special variable that hold the "exit status of the most recently executed
  # foreground pipeline"
  pid=$?

  if [ $pid -ne 0 ] ; then
    echo "Plovr is already running"
    exit 1
  fi

  # If the application isn't running, starts it
  echo -n "Starting plovr: "

  # Redirects default and error output to a log file
  java -jar $PLOVR $ARGS  >> $LOG 2>&1 &
  echo "OK"
}

# Stops the application
stop() {

  # Like as the start function, checks the application status
  check_status

  pid=$?

  if [ $pid -eq 0 ] ; then
    echo "Plovr is not running"
    exit 1
  fi

  # Kills the application process
  echo -n "Stopping plovr: "
  kill -9 $pid &
  echo "OK"
}

# Show the application status
status() {

  # The check_status function, again...
  check_status

  # If the PID was returned means the application is running
  if [ $? -ne 0 ] ; then
    echo "Plovr is running"
  else
    echo "Plovr is NOT running"
  fi

}

# Main logic, a simple case to call functions
case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  status)
    status
    ;;
  restart|reload)
    stop
    start
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|reload|status}"
    exit 1
esac

exit 0

