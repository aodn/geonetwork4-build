# Get setting via API

You should go to [swagger page](https://geonetwork-edge.edge.aodn.org.au/geonetwork/doc/api/index.html) to get the
geonetwork API. 

# How to update harvesters
You can get the geonetwork4 system setting via this api [setting](https://geonetwork-edge.edge.aodn.org.au/geonetwork/srv/api/site/settings/details)

However, there are settings that you cannot get it via normal API for example:
* For harvesters, use this [get harvesters call](https://geonetwork-edge.edge.aodn.org.au/geonetwork/srv/api/aodn/setup/harvesters), it returns the whole json config and you just need to copy and paste the portion you need and update the relevant harvesters json file.


