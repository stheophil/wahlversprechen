import json
import urllib.request
import random
import time

url = "http://192.168.56.102:9000"

def listFromJSON(path, transform):
	print("Reading '" + url + urllib.request.pathname2url(path) + "'")
	return map( 
		transform, 
		json.loads( urllib.request.urlopen(url + urllib.request.pathname2url(path)).read().decode("utf-8") )
	)

Name = lambda x: x['name']
tags = listFromJSON("/json/tags", Name)
categories = listFromJSON("/json/categories", Name)
authors = listFromJSON("/json/authors", Name)

items = []
for author in authors:
	items.extend( listFromJSON("/json/items/" + author, lambda item: item['id'] ) )

paths = ["/", "/aktuell", "/top", "/alle"]
paths.extend( map(lambda tag: urllib.request.pathname2url("/tag/" + tag), tags) )
paths.extend( map(lambda c: urllib.request.pathname2url("/category/" + c), categories) )
paths.extend( map(lambda id: urllib.request.pathname2url("/item/" + str(id)), items) )

while True:
	path = random.choice( paths )
	try:
		request = urllib.request.urlopen(url + path)
		print("Request %s [%d, %s]" % (path, request.status, request.reason))
	except urllib.error.HTTPError as e:
		print("Request %s: %s" % (path, e))

	time.sleep(1)