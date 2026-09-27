import type { Map as MapLibreMap, MapMouseEvent } from 'maplibre-gl';
import { maplibregl } from '../../map/maplibre';
import { useEffect, useRef, useState } from 'react';
import type { WorldMapCountry } from '../types';

interface WorldScratchMapProps {
  countries: WorldMapCountry[];
  selectedCountryCode: string;
  onCountrySelect: (countryCode: string, countryName: string) => void;
}

const countryBoundariesUrl = 'https://raw.githubusercontent.com/nvkelso/natural-earth-vector/refs/heads/master/geojson/ne_110m_admin_0_countries.geojson';

function countryCode(feature: maplibregl.MapGeoJSONFeature) {
  const code = feature.properties?.ISO_A2_EH ?? feature.properties?.ISO_A2;
  return typeof code === 'string' && code.length === 2 ? code : '';
}

export function WorldScratchMap({ countries, selectedCountryCode, onCountrySelect }: WorldScratchMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<MapLibreMap | null>(null);
  const onCountrySelectRef = useRef(onCountrySelect);
  const [mapUnavailable, setMapUnavailable] = useState(false);
  const visitedCodes = countries.filter((country) => country.status === 'VISITED').map((country) => country.countryCode);
  const plannedCodes = countries.filter((country) => country.status === 'PLANNED').map((country) => country.countryCode);
  const visitedKey = visitedCodes.join(',');
  const plannedKey = plannedCodes.join(',');
  const initialVisitedCodesRef = useRef(visitedCodes);
  const initialPlannedCodesRef = useRef(plannedCodes);
  const initialSelectedCodeRef = useRef(selectedCountryCode);

  useEffect(() => {
    onCountrySelectRef.current = onCountrySelect;
  }, [onCountrySelect]);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;
    try {
      const map = new maplibregl.Map({
        container: containerRef.current,
        style: import.meta.env.VITE_WORLD_MAP_STYLE_URL
          ?? import.meta.env.VITE_MAP_STYLE_URL
          ?? 'https://tiles.openfreemap.org/styles/positron',
        center: [12, 24],
        zoom: 1.15,
        minZoom: 0.7,
        maxZoom: 6,
        attributionControl: false,
      });
      mapRef.current = map;
      map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');
      map.addControl(new maplibregl.AttributionControl({ compact: true }), 'bottom-right');

      map.on('load', () => {
        map.addSource('world-countries', {
          type: 'geojson',
          data: countryBoundariesUrl,
        });
        map.addLayer({
          id: 'world-country-fill',
          type: 'fill',
          source: 'world-countries',
          paint: {
            'fill-color': [
              'case',
              ['in', ['get', 'ISO_A2_EH'], ['literal', initialVisitedCodesRef.current]], '#3f705a',
              ['in', ['get', 'ISO_A2_EH'], ['literal', initialPlannedCodesRef.current]], '#e2a16f',
              '#dfe2da',
            ],
            'fill-opacity': [
              'case',
              ['in', ['get', 'ISO_A2_EH'], ['literal', initialVisitedCodesRef.current]], 0.86,
              ['in', ['get', 'ISO_A2_EH'], ['literal', initialPlannedCodesRef.current]], 0.78,
              0.47,
            ],
          },
        });
        map.addLayer({
          id: 'world-country-selected',
          type: 'line',
          source: 'world-countries',
          filter: ['==', ['get', 'ISO_A2_EH'], initialSelectedCodeRef.current],
          paint: { 'line-color': '#ed7058', 'line-width': 3.2, 'line-opacity': 1 },
        });
        map.addLayer({
          id: 'world-country-outline',
          type: 'line',
          source: 'world-countries',
          paint: { 'line-color': '#ffffff', 'line-width': 0.7, 'line-opacity': 0.78 },
        });

        map.on('mousemove', 'world-country-fill', () => { map.getCanvas().style.cursor = 'pointer'; });
        map.on('mouseleave', 'world-country-fill', () => { map.getCanvas().style.cursor = ''; });
        map.on('click', 'world-country-fill', (event: MapMouseEvent) => {
          const feature = map.queryRenderedFeatures(event.point, { layers: ['world-country-fill'] })[0];
          if (!feature) return;
          const code = countryCode(feature);
          const name = String(feature.properties?.NAME_EN ?? feature.properties?.ADMIN ?? 'Unknown country');
          onCountrySelectRef.current(code, name);
        });
      });
      map.on('error', (event) => {
        if (String(event.error?.message ?? '').toLowerCase().includes('geojson')) setMapUnavailable(true);
      });

      return () => {
        map.remove();
        mapRef.current = null;
      };
    } catch {
      const timer = window.setTimeout(() => setMapUnavailable(true), 0);
      return () => window.clearTimeout(timer);
    }
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    if (!map?.getLayer('world-country-selected')) return;
    map.setFilter('world-country-selected', ['==', ['get', 'ISO_A2_EH'], selectedCountryCode]);
  }, [selectedCountryCode]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map?.getLayer('world-country-fill')) return;
    map.setPaintProperty('world-country-fill', 'fill-color', [
      'case',
      ['in', ['get', 'ISO_A2_EH'], ['literal', visitedKey ? visitedKey.split(',') : []]], '#3f705a',
      ['in', ['get', 'ISO_A2_EH'], ['literal', plannedKey ? plannedKey.split(',') : []]], '#e2a16f',
      '#dfe2da',
    ]);
    map.setPaintProperty('world-country-fill', 'fill-opacity', [
      'case',
      ['in', ['get', 'ISO_A2_EH'], ['literal', visitedKey ? visitedKey.split(',') : []]], 0.86,
      ['in', ['get', 'ISO_A2_EH'], ['literal', plannedKey ? plannedKey.split(',') : []]], 0.78,
      0.47,
    ]);
  }, [plannedKey, visitedKey]);

  if (mapUnavailable) {
    return (
      <div className="world-map-fallback">
        <strong>Map boundaries are unavailable</strong>
        <span>The country list and all of your travel totals are still available.</span>
      </div>
    );
  }

  return <div ref={containerRef} className="world-scratch-map" aria-label="Interactive world map of visited and planned countries" />;
}
