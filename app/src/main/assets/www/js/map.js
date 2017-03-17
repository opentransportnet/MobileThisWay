var lat = Activity.getLatitude();
var lng = Activity.getLongitude();
var iconNames = JSON.parse(Activity.getIconNames());
var map = L.map('map', {zoomControl: false}).setView([lat, lng], 11);
var attribution = map.attributionControl;
attribution.setPrefix('');
var timeFromLastTouch = 0;
var locationZoom = 16;
var maxZoom = 19;
var markerLat = 0;
var markerLng = 0;
var centerMapView = true;
var percentage = 0;
var zooming = false;
var routePolyline;
var isRecording = false;
var drawingTrack = false;
var routeStrokeColor = "#3fb4fb";
var light_green = "00cc00";
var cyan = "66ffff";
var markerIcon = L.divIcon({
    className: 'icon-location icon-size-standart curr-loc-marker',
    iconSize: new L.Point(56, 95),
    html: '<div style="width:25px;height:25px;background-color:#ffcc00;border-radius: 50%;margin-left: 15px;margin-top: -49px;"></div'
});
var marker = L.marker([lat, lng], {icon: markerIcon});
// Stack with all created change points
var changePoints = new Array();
var makeNewRoute = false;
var showMyLocRunning = false;

var tileLayer = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
//    attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a>',
    attribution: '',
    maxZoom: maxZoom
}).addTo(map);

marker.addTo(map);

function detectTouch() {
    // If user holding set time how long don't center map
    if (Activity.isHoldingMap()) {
        timeFromLastTouch = 20000;
    }
    // Update time from last touch
    else if (timeFromLastTouch > 0) {
        timeFromLastTouch -= 200;
    }
}
setInterval(detectTouch, 200);

function continueShowLoc() {
    var latitude = Activity.getLatitude();
    var longitude = Activity.getLongitude();

    marker.setLatLng([latitude, longitude]).update();
    lat = latitude;
    lng = longitude;

    map.setView([latitude, longitude], locationZoom, {pan: {animate: false}});

    timeFromLastTouch = 0;


    showMyLocRunning = false;

    setTimeout(function () {
        if (showMyLocRunning === false) {
            showMyLocation();
        }
    }, 900);
}

function centerMyLocation() {
    var latitude = Activity.getLatitude();
    var longitude = Activity.getLongitude();
    map.setView([latitude, longitude], locationZoom, {pan: {animate: true}});

    timeFromLastTouch = 0;
}

document.getElementById("current_location")
        .addEventListener("click", centerMyLocation);

function showMyLocation() {
    showMyLocRunning = true;
    centerMapView = true;
    markerLat = lat;
    markerLng = lng;
    lat = Activity.getLatitude();
    lng = Activity.getLongitude();
    isRecording = Activity.isRecording();

    if (markerLat !== lat || markerLng !== lng) {
        centerMapView = false;
        percentage = 0.1;
        var tmpRoutePolyline = null;
        if (isRecording) {
            tmpRoutePolyline = L.polyline([[markerLat, markerLng]],
                    {color: routeStrokeColor, opacity: 1})
                    .addTo(map);
        }

        function animateMarker() {
            if (percentage > 1) {
                makeNewRoute = false;
                // Draw large static route
                drawCurrentTrack(lat, lng);
                if (tmpRoutePolyline !== null) {
                    // Remove animated route line
                    map.removeLayer(tmpRoutePolyline);
                }

                showMyLocation();
            } else {
                if (!zooming) {
                    var pLat = markerLat + percentage * (lat - markerLat);
                    var pLng = markerLng + percentage * (lng - markerLng);
                    // Update marker location
                    marker.setLatLng([pLat, pLng]).update();

                    if (!makeNewRoute && tmpRoutePolyline !== null) {
                        // Draw animated route line segment
                        tmpRoutePolyline.addLatLng([pLat, pLng]);
                    }

                    if (timeFromLastTouch <= 0) {
                        // Pan map view
                        map.setView([pLat, pLng], locationZoom, {
                            pan: {animate: false}
                        });
                    }
                }
                percentage = percentage + 0.1;
                setTimeout(animateMarker, 50);
            }
        }
        animateMarker();
    } else {
        makeNewRoute = false;
        if (timeFromLastTouch <= 0) {
            // Pan map view
            map.setView([lat, lng], locationZoom, {
                pan: {animate: true}
            });
        }
        setTimeout(showMyLocation, 1000);
    }
}

map.on('zoomstart', function (e) {
    zooming = true;
});

map.on('zoomend', function (e) {
    zooming = false;
});

function drawCurrentTrack(lat, lng) {
    if (isRecording) {
        if (routePolyline !== null) {
            routePolyline.addLatLng([lat, lng]);
        }
    }
}

function makeNewTrack() {
    makeNewRoute = true;
    var lat = Activity.getLatitude();
    var lng = Activity.getLongitude();
    routePolyline = L.polyline([[lat, lng]],
            {color: routeStrokeColor, opacity: 1})
            .addTo(map);
}

function rmCurrTrack() {
    if (routePolyline !== null) {
        map.removeLayer(routePolyline);
        routePolyline = null;

        while (changePoints.length > 0) {
            map.removeLayer(changePoints.pop());
        }
    }
}

function addChangePoint(lat, lng, iconId, startPoint, comment) {
    var markerBg = cyan;
    if (startPoint) {
        markerBg = light_green;
    }
    var changePointIcon = L.divIcon({
        className: iconNames[iconId - 1] + ' icon-size-standart',
        iconSize: new L.Point(56, 95),
        html: '<div style="width:25px;height:25px;background-color:#' + markerBg
                + ';border-radius: 50%;margin-left: 15px;margin-top: -49px;"></div',
        popupAnchor: new L.Point(1, -33)
    });

    var marker = L.marker([lat, lng], {icon: changePointIcon});
    marker.addTo(map);

    if (comment !== "") {
        marker.bindPopup(window.atob(comment));
    }
    changePoints.push(marker);
}

function loadScript(url, callback) {
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = url;
    script.onreadystatechange = callback;
    script.onload = callback;
    head.appendChild(script);
}

function loadStyleSheet(url, callback) {
    var head = document.getElementsByTagName('head')[0];
    var styleSheet = document.createElement('link');
    styleSheet.rel = 'stylesheet';
    styleSheet.href = url;
    styleSheet.onreadystatechange = callback;
    styleSheet.onload = callback;
    head.appendChild(styleSheet);
}

var localPoiIcon = L.icon({
    iconUrl: 'img/local_poi_marker.png',
    iconSize: [40, 40], // size of the icon
    iconAnchor: [20, 20], // point of the icon which will correspond to marker's location
});

function addLocalPoi(lat, lng) {
    L.marker([lat, lng], {icon: localPoiIcon}).addTo(map);
}