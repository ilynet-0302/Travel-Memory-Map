import type { GeoJSONSource, Map as MapLibreMap, Marker } from 'maplibre-gl';
import { maplibregl } from '../maplibre';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  clampReplayProgress,
  coordinateAtReplayProgress,
  fullReplayRoute,
  replayRoutePaths,
  routeAtReplayProgress,
  type Coordinate,
} from '../../replay/replayPlayback';
import type { ReplayRouteSegment } from '../../replay/types';
import type { TripStop } from '../../trips/types';

interface TripMapProps {
  stops: TripStop[];
  activeStopId?: string;
  routeProgress?: number;
  routeSegments?: ReplayRouteSegment[];
  onStopSelect?: (stopId: string) => void;
}

function routeFeature(coordinates: Coordinate[]) {
  return {
    type: 'Feature' as const,
    properties: {},
    geometry: { type: 'LineString' as const, coordinates },
  };
}

function fitFullRoute(map: MapLibreMap, coordinates: Coordinate[]) {
  if (!coordinates.length) return;
  const bounds = coordinates.reduce(
    (current, coordinate) => current.extend(coordinate),
    new maplibregl.LngLatBounds(coordinates[0], coordinates[0]),
  );
  map.fitBounds(bounds, { padding: 52, maxZoom: 14, duration: 0 });
}

export function TripMap({ stops, activeStopId, routeProgress = 0, routeSegments, onStopSelect }: TripMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<MapLibreMap | null>(null);
  const markersRef = useRef<Marker[]>([]);
  const replayMarkerRef = useRef<Marker | null>(null);
  const progressRef = useRef(routeProgress);
  const [mapUnavailable, setMapUnavailable] = useState(false);
  const stopCoordinates = useMemo(() => stops.map(({ coordinates }) => coordinates), [stops]);
  const routePaths = useMemo(
    () => replayRoutePaths(routeSegments),
    [routeSegments],
  );
  const fullRoute = useMemo(
    () => fullReplayRoute(routePaths, stopCoordinates),
    [routePaths, stopCoordinates],
  );
  const stopCoordinatesRef = useRef(stopCoordinates);
  const routePathsRef = useRef(routePaths);
  const fullRouteRef = useRef(fullRoute);

  useEffect(() => {
    stopCoordinatesRef.current = stopCoordinates;
    routePathsRef.current = routePaths;
    fullRouteRef.current = fullRoute;
  }, [fullRoute, routePaths, stopCoordinates]);

  useEffect(() => {
    progressRef.current = routeProgress;
  }, [routeProgress]);

  useEffect(() => {
    if (!containerRef.current || stops.length === 0 || mapRef.current) return;

    try {
      const map = new maplibregl.Map({
        container: containerRef.current,
        style: import.meta.env.VITE_MAP_STYLE_URL ?? 'https://tiles.openfreemap.org/styles/liberty',
        center: stops[0].coordinates,
        zoom: 13,
        attributionControl: false,
      });
      mapRef.current = map;
      map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');
      map.addControl(new maplibregl.AttributionControl({ compact: true }), 'bottom-right');

      map.on('load', () => {
        const currentStopCoordinates = stopCoordinatesRef.current;
        const currentRoutePaths = routePathsRef.current;
        const currentFullRoute = fullRouteRef.current;
        const initialRoute = routeAtReplayProgress(
          currentRoutePaths,
          currentStopCoordinates,
          progressRef.current,
        );
        map.addSource('trip-route-guide', {
          type: 'geojson',
          data: routeFeature(currentFullRoute),
        });
        map.addLayer({
          id: 'trip-route-guide',
          type: 'line',
          source: 'trip-route-guide',
          layout: {
            'line-cap': 'round',
            'line-join': 'round',
          },
          paint: {
            'line-color': '#3d5c4e',
            'line-width': 4,
            'line-opacity': 0.48,
          },
        });
        map.addSource('trip-route-progress', {
          type: 'geojson',
          data: routeFeature(initialRoute),
        });
        map.addLayer({
          id: 'trip-route-shadow',
          type: 'line',
          source: 'trip-route-progress',
          layout: {
            'line-cap': 'round',
            'line-join': 'round',
          },
          paint: {
            'line-color': '#ffffff',
            'line-width': 10,
            'line-opacity': 0.9,
          },
        });
        map.addLayer({
          id: 'trip-route-progress',
          type: 'line',
          source: 'trip-route-progress',
          layout: {
            'line-cap': 'round',
            'line-join': 'round',
          },
          paint: {
            'line-color': '#ee7259',
            'line-width': 5,
            'line-opacity': 0.96,
          },
        });
        fitFullRoute(map, currentFullRoute);
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

      const replayElement = document.createElement('div');
      replayElement.className = 'map-replay-cursor';
      replayElement.innerHTML = '<span>✦</span>';
      replayMarkerRef.current = new maplibregl.Marker({ element: replayElement, anchor: 'center' })
        .setLngLat(stops[0].coordinates)
        .addTo(map);

      return () => {
        markersRef.current.forEach((marker) => marker.remove());
        markersRef.current = [];
        replayMarkerRef.current?.remove();
        replayMarkerRef.current = null;
        map.remove();
        mapRef.current = null;
      };
    } catch {
      const fallbackTimer = window.setTimeout(() => setMapUnavailable(true), 0);
      return () => window.clearTimeout(fallbackTimer);
    }
  }, [onStopSelect, stops]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    const guideSource = map.getSource('trip-route-guide') as GeoJSONSource | undefined;
    const progressSource = map.getSource('trip-route-progress') as GeoJSONSource | undefined;
    if (!guideSource || !progressSource) return;
    guideSource.setData(routeFeature(fullRoute));
    progressSource.setData(routeFeature(routeAtReplayProgress(routePaths, stopCoordinates, progressRef.current)));
    fitFullRoute(map, fullRoute);
  }, [fullRoute, routePaths, stopCoordinates]);

  useEffect(() => {
    const completedIndex = Math.floor(clampReplayProgress(routeProgress, stops.length) + 0.0001);
    markersRef.current.forEach((marker, index) => {
      const element = marker.getElement();
      element.classList.toggle('map-marker--active', element.dataset.stopId === activeStopId);
      element.classList.toggle('map-marker--visited', index <= completedIndex);
    });
  }, [activeStopId, routeProgress, stops.length]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !stops.length) return;
    const cursor = coordinateAtReplayProgress(routePaths, stopCoordinates, routeProgress);
    if (!cursor) return;
    const routeSource = map.getSource('trip-route-progress') as GeoJSONSource | undefined;
    routeSource?.setData(routeFeature(routeAtReplayProgress(routePaths, stopCoordinates, routeProgress)));
    replayMarkerRef.current?.setLngLat(cursor);
  }, [routePaths, routeProgress, stopCoordinates, stops.length]);

  if (mapUnavailable) {
    return (
      <div className="map-fallback">
        <strong>Map preview unavailable</strong>
        <span>Your timeline is still available alongside it.</span>
      </div>
    );
  }

  return <div ref={containerRef} className="trip-map" aria-label="Interactive animated road route map" />;
}
