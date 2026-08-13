import { ChevronLeft, ChevronRight, Pause, Play, RotateCcw } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import type { TripStop } from '../../trips/types';
import { advanceReplayProgress } from '../replayPlayback';
import type { ReplayFrame } from '../types';

interface ReplayControlsProps {
  stops: TripStop[];
  frames?: ReplayFrame[];
  activeIndex: number;
  routeProgress: number;
  onActiveIndexChange: (index: number) => void;
  onRouteProgressChange: (progress: number) => void;
  loading?: boolean;
  errorMessage?: string;
}
const speeds = [1, 2, 4] as const;

export function ReplayControls({
  stops,
  frames,
  activeIndex,
  routeProgress,
  onActiveIndexChange,
  onRouteProgressChange,
  loading,
  errorMessage,
}: ReplayControlsProps) {
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState<(typeof speeds)[number]>(1);
  const progressRef = useRef(routeProgress);
  const activeIndexRef = useRef(activeIndex);
  const activeStop = stops[activeIndex];
  const activeFrame = frames?.[activeIndex];

  useEffect(() => {
    progressRef.current = routeProgress;
  }, [routeProgress]);

  useEffect(() => {
    activeIndexRef.current = activeIndex;
  }, [activeIndex]);

  useEffect(() => {
    if (!playing || stops.length < 2) return;
    let animationFrame = 0;
    let previousTimestamp: number | undefined;
    const tick = (timestamp: number) => {
      if (previousTimestamp === undefined) previousTimestamp = timestamp;
      const elapsed = timestamp - previousTimestamp;
      previousTimestamp = timestamp;
      const nextProgress = advanceReplayProgress(progressRef.current, elapsed, speed, stops.length);
      progressRef.current = nextProgress;
      onRouteProgressChange(nextProgress);
      const nextIndex = Math.min(stops.length - 1, Math.floor(nextProgress + 0.0001));
      if (nextIndex !== activeIndexRef.current) {
        activeIndexRef.current = nextIndex;
        onActiveIndexChange(nextIndex);
      }
      if (nextProgress >= stops.length - 1) {
        setPlaying(false);
        return;
      }
      animationFrame = window.requestAnimationFrame(tick);
    };
    animationFrame = window.requestAnimationFrame(tick);
    return () => window.cancelAnimationFrame(animationFrame);
  }, [onActiveIndexChange, onRouteProgressChange, playing, speed, stops.length]);

  const goTo = (index: number) => {
    setPlaying(false);
    progressRef.current = index;
    activeIndexRef.current = index;
    onRouteProgressChange(index);
    onActiveIndexChange(index);
  };

  const replay = () => {
    if (stops.length < 2) return;
    if (routeProgress >= stops.length - 1) goTo(0);
    setPlaying((current) => !current);
  };

  const progressPercent = stops.length <= 1
    ? (stops.length ? 100 : 0)
    : (routeProgress / (stops.length - 1)) * 100;

  return (
    <section className="replay-panel" aria-label="Trip replay controls">
      <div className="replay-panel__now">
        <span className="replay-panel__day">DAY {activeStop?.day ?? 1}</span>
        <strong>{activeStop?.arrivalTime}</strong>
        <span>{activeStop?.name}</span>
        {activeFrame?.photos.length ? <small>{activeFrame.photos.length} memor{activeFrame.photos.length === 1 ? 'y' : 'ies'}</small> : null}
      </div>

      <div className="replay-panel__progress" role="progressbar" aria-label="Replay progress" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progressPercent)}>
        <span style={{ width: `${progressPercent}%` }} />
      </div>

      <div className="replay-panel__controls">
        <button className="icon-button icon-button--light" type="button" onClick={() => goTo(Math.max(0, activeIndex - 1))} disabled={activeIndex === 0} aria-label="Previous stop">
          <ChevronLeft size={19} />
        </button>
        <button className="replay-button" type="button" onClick={replay} disabled={stops.length < 2}>
          {routeProgress >= stops.length - 1 && !playing ? (
            <RotateCcw size={19} />
          ) : playing ? (
            <Pause size={19} fill="currentColor" />
          ) : (
            <Play size={19} fill="currentColor" />
          )}
          {playing ? 'Pause' : routeProgress >= stops.length - 1 ? 'Replay' : 'Replay trip'}
        </button>
        <button className="icon-button icon-button--light" type="button" onClick={() => goTo(Math.min(stops.length - 1, activeIndex + 1))} disabled={activeIndex >= stops.length - 1} aria-label="Next stop">
          <ChevronRight size={19} />
        </button>
        <div className="speed-control" aria-label="Replay speed">
          {speeds.map((value) => (
            <button key={value} type="button" className={speed === value ? 'is-active' : ''} onClick={() => setSpeed(value)}>{value}×</button>
          ))}
        </div>
      </div>
      {loading && <small className="replay-panel__status">Preparing memories…</small>}
      {errorMessage && <small className="replay-panel__status replay-panel__status--error">The route still works, but synchronized memories could not be loaded.</small>}
    </section>
  );
}
