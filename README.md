WeiboNearbyParser
by @CAQ9 at Sina Weibo
Mar 2014

This project fetches the information of a location, its nearby users, messages, popular places etc.

Please first login using 
  WeiboLogin.updateCookie("username", "password");
Check the return value if true or false.

Then you can use
  WeiboNearbyParser.getHome(longitude, latitude)
  WeiboNearbyParser.getCheckin(longitude, latitude)
  WeiboNearbyParser.getRelateWeibo(longitude, latitude)
  WeiboNearbyParser.getNearby(longitude, latitude)
to get the information.

For the WeiboLocation, WeiboMessage and WeiboUser classes, please refer to the comments inside to know what member variables are for.


License:
Use it at your own risk.


Third-party libraries:
- org.json
- simplejson
- Apache commons codec, commons logging, http components etc.
- JSoup
