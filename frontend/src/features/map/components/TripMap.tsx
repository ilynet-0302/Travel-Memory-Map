import maplibregl, { type GeoJSONSource, type Map as MapLibreMap, type Marker } from 'maplibre-gl';
import { useEffect, useRef, useState } from 'react';
import type { TripStop } from '../../trips/types';

interface TripMapProps {
  stops: TripStop[];
  activeStopId?: string;
  onStopSelect?: (stopId: string) => void;
}

export function TripMap({ stops, activeStopId, onStopSelect }: TripMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<MapLibreMap | null>(null);
  const markersRef = useRef<Marker[]>([]);
  const [mapUnavailable, setMapUnavailable] = useState(false);

  useEffect(() => {
    if (!containerRef.current || stops.length === 0 || mapRef.current) return;

    try {
      const map = new maplibregl.Map({
        container: containerRef.current,
        style: import.meta.env.VITE_MAP_STYLE_URL ?? 'https://demotiles.maplibre.org/style.json',
        center: stops[1]?.coordinates ?? stops[0].coordinates,
        zoom: 11.2,
        attributionControl: false,
      });
      mapRef.current = map;
      map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');
      map.addControl(new maplibregl.AttributionControl({ compact: true }), 'bottom-right');

      map.on('load', () => {
        const initialCoordinates = [stops[0].coordinates, stops[0].coordinates];
        map.addSource('trip-route', {
          type: 'geojson',
          data: {
            type: 'Feature',
            properties: {},
            geometry: {
              type: 'LineString',
              coordinates: initialCoordinates,
            },
          },
        });
        map.addLayer({
          id: 'trip-route-shadow',
          type: 'line',
          source: 'trip-route',
          paint: {
            'line-color': '#ffffff',
            'line-width': 7,
            'line-opacity': 0.85,
          },
        });
        map.addLayer({
          id: 'trip-route',
          type: 'line',
          source: 'trip-route',
          paint: {
            'line-color': '#ee7259',
            'line-width': 3,
            'line-dasharray': [1.6, 1.4],
          },
        });
      });

      markersRef.current = stops.map((stop, index) => {
        const element = document.createElement('button');
        element.type = 'button';
        element.className = 'map-marker';
        element.dataset.stopId = stop.id;
        element.textContent = String(index + 1);
        element.title = stop.name;
        element.addEventListener('click', () => onStopSelect?.(stop.id));
        return new maplibregl.Marker({ element, anchor: 'bottom' })
          .setLngLat(stop.coordinates)
          .addTo(map);
      });

      return () => {
        markersRef.current.forEach((marker) => marker.remove());
        markersRef.current = [];
        map.remove();
        mapRef.current = null;
      };
    } catch {
      const fallbackTimer = window.setTimeout(() => setMapUnavailable(true), 0);
      return () => window.clearTimeout(fallbackTimer);
    }
  }, [onStopSelect, stops]);

  useEffect(() => {
    markersRef.current.forEach((marker) => {
      marker
        .getElement()
        .classList.toggle('map-marker--active', marker.getElement().dataset.stopId === activeStopId);
    });

    const activeStop = stops.find(({ id }) => id === activeStopId);
    if (activeStop && mapRef.current) {
      const activeIndex = stops.findIndex(({ id }) => id === activeStopId);
      const visibleCoordinates = stops.slice(0, activeIndex + 1).map(({ coordinates }) => coordinates);
      if (visibleCoordinates.length === 1) visibleCoordinates.push(visibleCoordinates[0]);
      const routeSource = mapRef.current.getSource('trip-route') as GeoJSONSource | undefined;
      routeSource?.setData({
        type: 'Feature',
        properties: {},
        geometry: { type: 'LineString', coordinates: visibleCoordinates },
      });
      mapRef.current.flyTo({
        center: activeStop.coordinates,
        zoom: 14.2,
        duration: 1_100,
        essential: true,
      });
    }
  }, [activeStopId, stops]);

  if (mapUnavailable) {
    return (
      <div className="map-fallback">
        <strong>Map preview unavailable</strong>
        <span>Your timeline is still available alongside it.</span>
      </div>
    );
  }

  return <div ref={containerRef} className="trip-map" aria-label="Interactive route map" />;
}
