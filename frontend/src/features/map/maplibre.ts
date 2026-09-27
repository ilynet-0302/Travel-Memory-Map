import * as maplibregl from 'maplibre-gl';
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url';

// Let Vite bundle the worker and resolve its URL under the deployment base path.
maplibregl.setWorkerUrl(workerUrl);

export { maplibregl };
