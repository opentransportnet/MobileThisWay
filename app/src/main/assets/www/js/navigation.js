var iconNames = JSON.parse(Activity.getIconNames());
var route = new L.KML(NavigationActivity.getRouteKmlFile(), {async: true}, iconNames);
route.on("loaded", function (e) {
//    map.fitBounds(e.target.getBounds(), {padding: [45, 45]});
});
map.addLayer(route);