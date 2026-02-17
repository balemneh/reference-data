#!/usr/bin/env bash
export https_proxy=http://proxy.cbp.dhs.gov:80
export no_proxy=localhost,.dhs.gov
echo "[google-chrome]" > /etc/yum.repos.d/google-chrome.repo
echo "name=google-chrome" >> /etc/yum.repos.d/google-chrome.repo
echo "baseurl=https://dl.google.com/linux/chrome/rpm/stable/x86_64" >> /etc/yum.repos.d/google-chrome.repo
echo "enabled=1" >> /etc/yum.repos.d/google-chrome.repo
echo "gpgcheck=1" >> /etc/yum.repos.d/google-chrome.repo
echo "gpgkey=https://dl-ssl.google.com/linux/linux_signing_key.pub" >> /etc/yum.repos.d/google-chrome.repo

cat /etc/yum.repos.d/google-chrome.repo
rm -rf /var/cache/yum/*
yum clean all
yum -y install google-chrome-stable --enablerepo=rockylinux-8* --nogpgcheck &

npm ci &

wait
