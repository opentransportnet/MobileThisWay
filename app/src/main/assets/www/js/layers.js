var layers = [];
//var layerLegendControls = [];

function addWmsLayer(posId, url, layerName) {
    console.log("adding WMS");
    console.log(url);
    console.log(layerName);
    var layer = new L.NonTiledLayer.WMS(url, {
        layers: layerName,
        format: 'image/png',
        transparent: true,
        maxZoom: maxZoom
    });
    
    map.addLayer(layer);
    layers[posId] = layer;
    //var uri2 = url + "&service=WMS&VERSION=1.3.0&SLD_VERSION=1.1.0&request=GetLegendGraphic&format=image/png&layer=" + layerName;
    //var wmsLegendControl = L.wmsLegend(uri2);
    //map.addControl(wmsLegendControl);
}

function addBaseWmsLayer(url, layerName) {
    console.log("adding static WMS");
    var layer = new L.NonTiledLayer.WMS(url, {
        layers: layerName,
        format: 'image/png',
        transparent: true,
        maxZoom: maxZoom
    });
    
    map.addLayer(layer);
}

function removeWmsLayer(posId) {
    var layer = layers[posId];
    map.removeLayer(layer);
    //map.removeControl(layerLegendControls[i]);
}