#!/bin/bash
rm -rf www
mkdir www
clj -A:fig:min
cp -r resources/public/* www
cp -r target/public/* www
