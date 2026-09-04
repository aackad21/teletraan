#!/bin/bash -ex
#
# This script takes a standard ubuntu box and installs all the software needed to run teletraan locally.

apt-get update

# Install OpenJDK 21
apt-get install -y openjdk-21-jdk

# Install dev env
apt-get install -y maven python python-setuptools python-pip python-virtualenv python-dev

# Install mysql and create tables
export DEBIAN_FRONTEND=noninteractive
apt-get install -q -y mysql-server mysql-client
mysql -u root < /home/vagrant/teletraan/deploy-service/common/src/main/resources/sql/deploy.sql

# Create virtualenv
virtualenv /home/vagrant/venv
source /home/vagrant/venv/bin/activate
pip install -r /home/vagrant/teletraan/deploy-board/requirements.txt

echo "Completed initializing the VM".
