package com.example.compassapp.ui

/**
 * Leaflet map with a chain of FREE, no-API-key tile providers.
 * If one provider is blocked or fails, it automatically falls back
 * to the next. The red pin (L.divIcon) stays glued to the coordinates.
 */
fun buildMapHtml(latitude: Double, longitude: Double, zoom: Int = 16): String {
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css"/>
<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
<style>
  *{margin:0;padding:0;}
  html,body{width:100%;height:100%;overflow:hidden;}
  #map{width:100%;height:100%;background:#1a1a2e;}
  .leaflet-control-attribution{font-size:9px;opacity:.7;}
  #info{position:absolute;top:12%;left:50%;transform:translateX(-50%);z-index:1000;color:#fff;
        font-family:sans-serif;font-size:14px;text-align:center;background:rgba(0,0,0,.65);
        padding:10px 16px;border-radius:8px;pointer-events:none;}
  .red-pin{position:relative;width:34px;height:46px;}
  .red-pin-body{position:absolute;top:0;left:3px;width:28px;height:28px;background:#E50000;
        border:3px solid #fff;border-radius:50% 50% 50% 0;transform:rotate(-45deg);
        box-shadow:0 3px 8px rgba(0,0,0,.55);}
  .red-pin-dot{position:absolute;top:9px;left:12px;width:10px;height:10px;background:#fff;border-radius:50%;}
</style>
</head>
<body>
<div id="map"></div>
<div id="info">Loading map…</div>
<script>
  var map = L.map('map',{center:[$latitude,$longitude],zoom:$zoom,zoomControl:true});

  /* FREE tile providers — no API key required, tried in order */
  var providers = [
    { url:'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
      attr:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' },
    { url:'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png',
      attr:'&copy; OpenStreetMap contributors, SRTM | &copy; <a href="https://opentopomap.org">OpenTopoMap</a>' },
    { url:'https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png',
      attr:'&copy; OpenStreetMap contributors, Tiles &copy; <a href="https://www.hotosm.org">HOT</a>' }
  ];

  var loaded = false;
  var tileLayer = null;
  var switchTimer = null;

  function useProvider(i) {
    if (loaded) return;
    if (i >= providers.length) {
      if (switchTimer) clearTimeout(switchTimer);
      var el = document.getElementById('info');
      if (el) el.innerHTML = 'Map tiles unavailable.<br>Please check connection.';
      return;
    }
    if (tileLayer) { map.removeLayer(tileLayer); tileLayer = null; }
    if (switchTimer) { clearTimeout(switchTimer); switchTimer = null; }

    var p = providers[i];
    tileLayer = L.tileLayer(p.url, { maxZoom:19, attribution:p.attr }).addTo(map);

    tileLayer.on('load', function() {
      if (loaded) return;
      loaded = true;
      if (switchTimer) { clearTimeout(switchTimer); switchTimer = null; }
      var el = document.getElementById('info');
      if (el) el.remove();
    });

    var errors = 0;
    tileLayer.on('tileerror', function() {
      if (loaded) return;
      errors++;
      if (errors >= 3) useProvider(i + 1);   /* blocked/broken -> next provider */
    });

    /* if nothing loads within 8s, proactively try the next provider */
    switchTimer = setTimeout(function() {
      if (!loaded) useProvider(i + 1);
    }, 8000);
  }
  useProvider(0);

  var redIcon = L.divIcon({
    className:'',
    html:'<div class="red-pin"><div class="red-pin-body"></div><div class="red-pin-dot"></div></div>',
    iconSize:[34,46],
    iconAnchor:[17,46],
    popupAnchor:[0,-46]
  });
  L.marker([$latitude,$longitude],{icon:redIcon}).addTo(map)
    .bindPopup('<b>You are here</b>').openPopup();
</script>
</body>
</html>
""".trimIndent()
}