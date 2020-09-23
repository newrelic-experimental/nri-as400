#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

OHI_NAME="nri-as400"
OHI_EXECUTABLE="$OHI_NAME.jar" # Remove or change .jar if a different exectuable type

echo "New Relic Integration Installer"

if [ "$EUID" -ne 0 ]
  then echo "Please run with sudo or as root"
  exit
fi

echo "Stopping NR Infrastructure Agent"
if [ -f /etc/systemd/system/newrelic-infra.service ]; then
    echo "Using SYSTEMD"
    service newrelic-infra stop
fi
if [ -f /etc/init/newrelic-infra.conf ]; then
    echo "Using INITCTL"
    initctl stop newrelic-infra
fi

echo "Copying Files"
cp $DIR/$OHI_NAME-config.yml /etc/newrelic-infra/integrations.d/
cp $DIR/$OHI_NAME-definition.yml /var/db/newrelic-infra/custom-integrations/$OHI_NAME-definition.yml
mkdir -p /var/db/newrelic-infra/custom-integrations/bin
cp $DIR/$OHI_EXECUTABLE /var/db/newrelic-infra/custom-integrations/bin
# Add other custom files here, i.e.
# cp $DIR/$OHI_NAME-custom-config.yml /var/db/newrelic-infra/custom-integrations/

echo "Starting NR Infrastructure Agent"
if [ -f /etc/systemd/system/newrelic-infra.service ]; then
    service newrelic-infra start
fi
if [ -f /etc/init/newrelic-infra.conf ]; then
    initctl start newrelic-infra
fi
