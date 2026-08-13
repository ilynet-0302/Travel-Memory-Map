import { ChevronLeft, ChevronRight, Pause, Play, RotateCcw } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { TripStop } from '../../trips/types';

interface ReplayControlsProps {
  stops: TripStop[];
  activeIndex: number;
  onActiveIndexChange: (index: number) => void;
}

const speeds = [1, 2, 4] as const;

export function ReplayControls({ stops, activeIndex, onActiveIndexChange }: ReplayControlsProps) {
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState<(typeof speeds)[number]>(1);
  const activeStop = stops[activeIndex];

  useEffect(() => {
    if (!playing || stops.length < 2) return;
    const timer = window.setInterval(() => {
      if (activeIndex >= stops.length - 1) {
        setPlaying(false);
        return;
      }
      onActiveIndexChange(activeIndex + 1);
    }, 2_400 / speed);
    return () => window.clearInterval(timer);
  }, [activeIndex, onActiveIndexChange, playing, speed, stops.length]);

  const replay = () => {
    if (activeIndex >= stops.length - 1) onActiveIndexChange(0);
    setPlaying((current) => !current);
  };

  return (
    <section className="replay-panel" aria-label="Trip replay controls">
      <div className="replay-panel__now">
        <span className="replay-panel__day">DAY {activeStop?.day ?? 1}</span>
        <strong>{activeStop?.arrivalTime}</strong>
        <span>{activeStop?.name}</span>
      </div>

      <div className="replay-panel__progress" aria-hidden="true">
        <span style={{ width: `${((activeIndex + 1) / Math.max(stops.length, 1)) * 100}%` }} />
      </div>

      <div className="replay-panel__controls">
        <button
          className="icon-button icon-button--light"
          type="button"
          onClick={() => onActiveIndexChange(Math.max(0, activeIndex - 1))}
          disabled={activeIndex === 0}
          aria-label="Previous stop"
        >
          <ChevronLeft size={19} />
        </button>
        <button className="replay-button" type="button" onClick={replay}>
          {activeIndex >= stops.length - 1 && !playing ? (
            <RotateCcw size={19} />
          ) : playing ? (
            <Pause size={19} fill="currentColor" />
          ) : (
            <Play size={19} fill="currentColor" />
          )}
          {playing ? 'Pause' : activeIndex >= stops.length - 1 ? 'Replay' : 'Replay trip'}
        </button>
        <button
          className="icon-button icon-button--light"
          type="button"
          onClick={() => onActiveIndexChange(Math.min(stops.length - 1, activeIndex + 1))}
          disabled={activeIndex >= stops.length - 1}
          aria-label="Next stop"
        >
          <ChevronRight size={19} />
        </button>
        <div className="speed-control" aria-label="Replay speed">
          {speeds.map((value) => (
            <button
              key={value}
              type="button"
              className={speed === value ? 'is-active' : ''}
              onClick={() => setSpeed(value)}
            >
              {value}×
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}
