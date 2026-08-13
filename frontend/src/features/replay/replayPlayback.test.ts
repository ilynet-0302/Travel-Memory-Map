import { describe, expect, it } from 'vitest';
import {
  advanceReplayProgress,
  coordinateAtProgress,
  coordinateAtReplayProgress,
  fullReplayRoute,
  replayCoordinate,
  replayRoutePaths,
  routeAtProgress,
  routeAtReplayProgress,
} from './replayPlayback';

describe('replay playback', () => {
  const route: [number, number][] = [[10, 40], [12, 42], [14, 43]];

  it('interpolates the moving coordinate and gradually reveals the route', () => {
    expect(coordinateAtProgress(route, 0.5)).toEqual([11, 41]);
    expect(routeAtProgress(route, 1.5)).toEqual([[10, 40], [12, 42], [13, 42.5]]);
  });

  it('advances according to speed and clamps at the final stop', () => {
    expect(advanceReplayProgress(0, 1600, 1, 3)).toBe(0.5);
    expect(advanceReplayProgress(1.8, 1600, 4, 3)).toBe(2);
  });

  it('moves along detailed road geometry while progress remains stop based', () => {
    const stops: [number, number][] = [[0, 0], [2, 0], [3, 1]];
    const roadSegments: [number, number][][] = [
      [[0, 0], [1, 0], [2, 0]],
      [[2, 0], [2, 1], [3, 1]],
    ];

    expect(coordinateAtReplayProgress(roadSegments, stops, 0.5)).toEqual([1, 0]);
    expect(routeAtReplayProgress(roadSegments, stops, 1.5)).toEqual([
      [0, 0], [1, 0], [2, 0], [2, 1],
    ]);
    expect(fullReplayRoute(roadSegments, stops)).toEqual([
      [0, 0], [1, 0], [2, 0], [2, 1], [3, 1],
    ]);
  });

  it('normalizes backend route objects and drops invalid coordinates', () => {
    expect(replayCoordinate({ longitude: 21.2382, latitude: 45.7304 })).toEqual([21.2382, 45.7304]);
    expect(replayCoordinate([21.25, 45.74])).toEqual([21.25, 45.74]);
    expect(replayCoordinate({ longitude: null, latitude: 45.74 })).toBeUndefined();
    expect(replayRoutePaths([{
      coordinates: [
        { longitude: 21.2382, latitude: 45.7304 },
        { longitude: null, latitude: 45.735 },
        { longitude: 21.25, latitude: 45.74 },
      ],
    }])).toEqual([[[21.2382, 45.7304], [21.25, 45.74]]]);
  });
});
