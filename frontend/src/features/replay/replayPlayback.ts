export type Coordinate = [longitude: number, latitude: number];

type ReplayCoordinatePayload = {
  longitude?: unknown;
  latitude?: unknown;
} | unknown[];

export const REPLAY_SEGMENT_DURATION_MS = 3_200;

export function replayCoordinate(payload: ReplayCoordinatePayload): Coordinate | undefined {
  const rawLongitude = Array.isArray(payload) ? payload[0] : payload.longitude;
  const rawLatitude = Array.isArray(payload) ? payload[1] : payload.latitude;
  if (rawLongitude === null || rawLongitude === undefined || rawLatitude === null || rawLatitude === undefined) {
    return undefined;
  }
  const longitude = Number(rawLongitude);
  const latitude = Number(rawLatitude);
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return undefined;
  if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) return undefined;
  return [longitude, latitude];
}

export function replayRoutePaths(
  segments: Array<{ coordinates?: ReplayCoordinatePayload[] }> | undefined,
): Coordinate[][] {
  return segments?.map(({ coordinates }) => coordinates?.flatMap((payload) => {
    const coordinate = replayCoordinate(payload);
    return coordinate ? [coordinate] : [];
  }) ?? []) ?? [];
}

export function clampReplayProgress(progress: number, stopCount: number) {
  return Math.max(0, Math.min(Math.max(0, stopCount - 1), progress));
}

export function advanceReplayProgress(
  progress: number,
  elapsedMilliseconds: number,
  speed: number,
  stopCount: number,
) {
  return clampReplayProgress(
    progress + (elapsedMilliseconds * speed) / REPLAY_SEGMENT_DURATION_MS,
    stopCount,
  );
}

export function coordinateAtProgress(coordinates: Coordinate[], progress: number): Coordinate | undefined {
  if (!coordinates.length) return undefined;
  const bounded = clampReplayProgress(progress, coordinates.length);
  const startIndex = Math.floor(bounded);
  const endIndex = Math.min(coordinates.length - 1, startIndex + 1);
  const segmentProgress = bounded - startIndex;
  const start = coordinates[startIndex];
  const end = coordinates[endIndex];
  return [
    start[0] + (end[0] - start[0]) * segmentProgress,
    start[1] + (end[1] - start[1]) * segmentProgress,
  ];
}

export function routeAtProgress(coordinates: Coordinate[], progress: number): Coordinate[] {
  const cursor = coordinateAtProgress(coordinates, progress);
  if (!cursor) return [];
  const bounded = clampReplayProgress(progress, coordinates.length);
  const completed = coordinates.slice(0, Math.floor(bounded) + 1);
  const route = [...completed];
  const last = route.at(-1);
  if (!last || last[0] !== cursor[0] || last[1] !== cursor[1]) route.push(cursor);
  if (route.length === 1) route.push(route[0]);
  return route;
}

function sameCoordinate(left: Coordinate, right: Coordinate) {
  return left[0] === right[0] && left[1] === right[1];
}

function appendCoordinates(target: Coordinate[], coordinates: Coordinate[]) {
  coordinates.forEach((coordinate) => {
    if (!target.length || !sameCoordinate(target.at(-1)!, coordinate)) target.push(coordinate);
  });
}

function coordinateDistanceKm(from: Coordinate, to: Coordinate) {
  return haversineDistanceKm([from, to]);
}

export function coordinateAlongPath(path: Coordinate[], fraction: number): Coordinate | undefined {
  if (!path.length) return undefined;
  if (path.length === 1 || fraction <= 0) return path[0];
  if (fraction >= 1) return path.at(-1);

  const distances = path.slice(1).map((coordinate, index) => coordinateDistanceKm(path[index], coordinate));
  const totalDistance = distances.reduce((sum, distance) => sum + distance, 0);
  if (totalDistance === 0) return path[0];
  const targetDistance = totalDistance * fraction;
  let travelled = 0;
  for (let index = 0; index < distances.length; index += 1) {
    const segmentDistance = distances[index];
    if (travelled + segmentDistance >= targetDistance) {
      const segmentFraction = segmentDistance === 0 ? 0 : (targetDistance - travelled) / segmentDistance;
      const from = path[index];
      const to = path[index + 1];
      return [
        from[0] + (to[0] - from[0]) * segmentFraction,
        from[1] + (to[1] - from[1]) * segmentFraction,
      ];
    }
    travelled += segmentDistance;
  }
  return path.at(-1);
}

function pathAtFraction(path: Coordinate[], fraction: number) {
  const cursor = coordinateAlongPath(path, fraction);
  if (!cursor) return [];
  if (fraction <= 0) return [cursor];
  if (fraction >= 1) return [...path];

  const distances = path.slice(1).map((coordinate, index) => coordinateDistanceKm(path[index], coordinate));
  const targetDistance = distances.reduce((sum, distance) => sum + distance, 0) * fraction;
  let travelled = 0;
  let endIndex = 0;
  for (let index = 0; index < distances.length; index += 1) {
    if (travelled + distances[index] >= targetDistance) {
      endIndex = index;
      break;
    }
    travelled += distances[index];
    endIndex = index + 1;
  }
  const result = path.slice(0, endIndex + 1);
  if (!result.length || !sameCoordinate(result.at(-1)!, cursor)) result.push(cursor);
  return result;
}

function validRoutePaths(routePaths: Coordinate[][], stopCoordinates: Coordinate[]) {
  return routePaths.length === Math.max(0, stopCoordinates.length - 1)
    && routePaths.every((path) => path.length >= 2);
}

export function fullReplayRoute(routePaths: Coordinate[][], stopCoordinates: Coordinate[]) {
  if (!validRoutePaths(routePaths, stopCoordinates)) {
    if (stopCoordinates.length === 1) return [stopCoordinates[0], stopCoordinates[0]];
    return stopCoordinates;
  }
  const route: Coordinate[] = [];
  routePaths.forEach((path) => appendCoordinates(route, path));
  return route.length === 1 ? [route[0], route[0]] : route;
}

export function coordinateAtReplayProgress(
  routePaths: Coordinate[][],
  stopCoordinates: Coordinate[],
  progress: number,
): Coordinate | undefined {
  if (!validRoutePaths(routePaths, stopCoordinates)) return coordinateAtProgress(stopCoordinates, progress);
  const bounded = clampReplayProgress(progress, stopCoordinates.length);
  if (bounded >= routePaths.length) return routePaths.at(-1)?.at(-1);
  const segmentIndex = Math.floor(bounded);
  return coordinateAlongPath(routePaths[segmentIndex], bounded - segmentIndex);
}

export function routeAtReplayProgress(
  routePaths: Coordinate[][],
  stopCoordinates: Coordinate[],
  progress: number,
) {
  if (!validRoutePaths(routePaths, stopCoordinates)) return routeAtProgress(stopCoordinates, progress);
  const bounded = clampReplayProgress(progress, stopCoordinates.length);
  const route: Coordinate[] = [];
  const completedSegments = Math.min(routePaths.length, Math.floor(bounded));
  for (let index = 0; index < completedSegments; index += 1) appendCoordinates(route, routePaths[index]);
  if (completedSegments < routePaths.length) {
    appendCoordinates(route, pathAtFraction(routePaths[completedSegments], bounded - completedSegments));
  }
  if (route.length === 1) route.push(route[0]);
  return route;
}

export function haversineDistanceKm(coordinates: Coordinate[]) {
  const earthRadiusKm = 6371.0088;
  let distance = 0;
  for (let index = 1; index < coordinates.length; index += 1) {
    const from = coordinates[index - 1];
    const to = coordinates[index];
    const fromLatitude = from[1] * Math.PI / 180;
    const toLatitude = to[1] * Math.PI / 180;
    const latitudeDelta = toLatitude - fromLatitude;
    const longitudeDelta = (to[0] - from[0]) * Math.PI / 180;
    const value = Math.sin(latitudeDelta / 2) ** 2
      + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.sin(longitudeDelta / 2) ** 2;
    distance += 2 * earthRadiusKm * Math.asin(Math.sqrt(value));
  }
  return Math.round(distance * 10) / 10;
}
