#!/bin/sh

if [ $# -gt 0 ]; then
	if [ "$1" = "remove" ]; then
		sudo service plovr stop > /dev/null
		sudo update-rc.d -f  plovr remove > /dev/null

		sudo rm /etc/init.d/plovr
		sudo rm /var/log/plovr
	else
		echo "Usage: ./autorun.sh [remove]"
	fi
else
	sudo ln -s `pwd`/autorun-initd.sh /etc/init.d/plovr
	sudo chmod 755 /etc/init.d/plovr
	sudo touch /var/log/plovr

	sudo update-rc.d plovr defaults > /dev/null
	sudo service plovr reload > /dev/null
fi

