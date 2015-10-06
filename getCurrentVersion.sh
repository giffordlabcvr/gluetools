#!/bin/bash
gradle currentVersion | grep "Project version: " | sed "s/Project version: //"
