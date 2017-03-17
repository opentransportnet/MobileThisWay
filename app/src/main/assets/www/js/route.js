var iconNames = JSON.parse(Activity.getIconNames());

var map = L.map('map', {
    zoomControl: false,
    attributionControl: false
}).setView([48.8226, 2.2679], 13);

var pointA = L.divIcon({
    className: 'icon-0205-flag icon-size-small icon-green',
    iconSize: new L.Point(50, 50),
    iconAnchor: new L.Point(10, 35)
});

var pointB = L.divIcon({
    className: 'icon-0205-flag icon-size-small icon-white',
    iconSize: new L.Point(50, 50),
    iconAnchor: new L.Point(10, 35)
});

L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {maxZoom: 16, })
        .addTo(map);

var route = new L.KML(Activity.getRouteKmlFile(), {async: true}, iconNames);
route.on("loaded", function (e) {
    map.fitBounds(e.target.getBounds(), {padding: [45, 45]});
});
map.addLayer(route);


var markerIcon = L.divIcon({
    className: 'icon-location icon-size-standart curr-loc-marker',
    iconSize: new L.Point(56, 95),
    html: '<div style="width:25px;height:25px;background-color:#ffcc00;border-radius: 50%;margin-left: 15px;margin-top: -49px;"></div'
});

function addMyMarker(lat, lng) {
    L.marker([lat, lng], {icon: markerIcon}).addTo(map);
}

function addStartAndDestPoints(sLat, sLng, dLat, dLng, radius) {
    L.marker([sLat, sLng], {icon: pointA}).addTo(map);
    L.circle([sLat, sLng], radius).addTo(map);
    L.marker([dLat, dLng], {icon: pointB}).addTo(map);
    L.circle([dLat, dLng], radius).addTo(map);
}