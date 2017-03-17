var lat = Activity.getLatitude();
var lng = Activity.getLongitude();
var radius = Activity.getRadius();
var sLat = Activity.getStartLatitude();
var sLng = Activity.getStartLongitude();
var dLat = Activity.getDestLatitude();
var dLng = Activity.getDestLongitude();
var pinStart = Activity.isPinStart();
var map = L.map('map', {zoomControl: false}).setView([lat, lng], 15);
var tileLayer = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a>',
    maxZoom: 16,
    noWrap: true,
    updateWhenIdle: false
}).addTo(map);

var markerIcon = L.divIcon({
    className: 'icon-location icon-size-standart curr-loc-marker',
    iconSize: new L.Point(50, 50),
    iconAnchor: new L.Point(27, 48),
    html: '<div style="width:25px;height:25px;background-color:#ffcc00;border-radius: 50%;margin-left:15px;margin-top: -49px;"></div'
});
L.marker([lat, lng], {icon: markerIcon}).addTo(map);

var locPinned = false;
var pinMarker;
var pinIcon;
var sPinIcon = L.divIcon({
    className: 'icon-location icon-size-standart',
    iconSize: new L.Point(56, 95),
    html: '<div style="width:25px;height:25px;background-color:#00cc00'
            + ';border-radius: 50%;margin-left: 15px;margin-top: -49px;"></div'
});
var dPinIcon = L.divIcon({
    className: 'icon-flag-checkered icon-size-standart',
    iconSize: new L.Point(56, 95),
    iconAnchor: new L.Point(11, 46)
});

var pinMarkerRadius;

/*
 * Set start and destination points if they have been set previously
 */
if (sLat !== 200 && sLng !== 200) {
    if (pinStart) {
        setPinMarker(sLat, sLng, sPinIcon);
        locPinned = true;
    } else {
        L.marker([sLat, sLng], {icon: sPinIcon}).addTo(map);
        L.circle([sLat, sLng], radius).addTo(map);
    }
}

if (dLat !== 200 && dLng !== 200) {
    if (!pinStart) {
        setPinMarker(dLat, dLng, dPinIcon);
        locPinned = true;
    } else {
        L.marker([dLat, dLng], {icon: dPinIcon}).addTo(map);
        L.circle([dLat, dLng], radius).addTo(map);
    }
}

if (pinStart) {
    // Pin starting point
    pinIcon = sPinIcon;
} else {
    // Pin destination point
    pinIcon = dPinIcon;
}

map.on('click', function (e) {
    if (locPinned === false) {
        pinMarker = L.marker(e.latlng, {icon: pinIcon, draggable: true});
        pinMarker.addTo(map).setZIndexOffset(100);
        pinMarkerRadius = L.circle(e.latlng, radius).addTo(map);
        locPinned = true;
        Activity.setCoordinates(e.latlng.lat, e.latlng.lng);

        pinMarker.on('move', function (e) {
            pinMarkerRadius.setLatLng(e.latlng);
            Activity.setCoordinates(e.latlng.lat, e.latlng.lng);
        });
    } else {
        pinMarker.setLatLng(e.latlng).update();
        pinMarkerRadius.setLatLng(e.latlng).update();
        Activity.setCoordinates(e.latlng.lat, e.latlng.lng);
    }
});

function setPinMarker(lat, lng, pinIcon) {
    pinMarker = L.marker([lat, lng], {icon: pinIcon, draggable: true});
    pinMarker.addTo(map).setZIndexOffset(100);
    pinMarkerRadius = L.circle([lat, lng], radius).addTo(map);
    locPinned = true;

    pinMarker.on('move', function (e) {
        pinMarkerRadius.setLatLng(e.latlng);
        Activity.setCoordinates(e.latlng.lat, e.latlng.lng);
    });
}
