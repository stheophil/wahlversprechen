#!/bin/sh
cd lib
r.js -o name=../app.js out=../app.min.js baseUrl=. paths.app=.. paths.template=../../template paths.routes=empty:

