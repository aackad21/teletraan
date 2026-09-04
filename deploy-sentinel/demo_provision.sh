#!/bin/bash -ex
#
# This script takes a standard ubuntu box and installs all the software needed to run teletraan locally.

echo "Install Teletraan runtime dependencies..."

apt-get update

echo "Install openjdk 21..."
apt-get install -y openjdk-21-jre-headless
echo "Successfully installed openjdk 21"

echo "Install python and related tools..."
apt-get install -y python python-pip python-virtualenv python-dev
echo "Successfully installed python"

echo "Successfully completed Teletraan dependencies install!"

su -c "source /home/vagrant/teletraan/deploy-sentinel/demo_run.sh" vagrant
