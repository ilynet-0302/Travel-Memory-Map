import { Activity, BrainCircuit, Compass, Sparkles } from 'lucide-react';
import { useTripDna } from '../hooks/useDna';
import type { DnaTrait } from '../types';
import { orderedDnaScores } from '../utils';

const traitLabels: Record<DnaTrait, string> = {
  explorer: 'Explorer',
  foodie: 'Foodie',
  culture: 'Culture',
  nightlife: 'Nightlife',
  relaxation: 'Relaxation',
  nature: 'Nature',
  adventure: 'Adventure',
};

const traitCopy: Record<DnaTrait, string> = {
  explorer: 'A varied route with plenty left to discover.',
  foodie: 'The destination is best understood one table at a time.',
  culture: 'Museums, landmarks and local history lead this story.',
  nightlife: 'The route comes alive when the sun goes down.',
  relaxation: 'This journey knows how to leave room for slow moments.',
  nature: 'Open landscapes and the outdoors shape the itinerary.',
  adventure: 'Movement, activity and a full day are part of the thrill.',
};

export function TripDnaPanel({ tripId }: { tripId: string }) {
  const dna = useTripDna(tripId);

  if (dna.isLoading) return <section className="trip-dna-panel trip-dna-panel--loading" aria-label="Calculating Trip DNA" />;
  if (!dna.data || dna.error) {
    return (
      <section className="trip-dna-panel trip-dna-panel--error">
        <BrainCircuit size={24} />
        <div><strong>Trip DNA could not be calculated</strong><p>{dna.error?.message ?? 'Try again in a moment.'}</p></div>
      </section>
    );
  }

  const dominant = dna.data.dominantTrait.toLowerCase() as DnaTrait;
  const scores = orderedDnaScores(dna.data.scores);

  return (
    <section className="trip-dna-panel">
      <header className="trip-dna-panel__heading">
        <div><span className="eyebrow">TRIP DNA</span><h2>What this journey is made of</h2></div>
        <span className="trip-dna-panel__method"><Activity size={13} /> Deterministic, not AI</span>
      </header>
      <div className="trip-dna-layout">
        <article className="trip-dna-dominant">
          <span><Compass size={23} /></span>
          <small>DOMINANT TRAIT</small>
          <h3>{traitLabels[dominant]}</h3>
          <p>{traitCopy[dominant]}</p>
          <strong>{dna.data.scores[dominant]}%</strong>
        </article>
        <div className="trip-dna-scores">
          {scores.map(([trait, score]) => (
            <div className="trip-dna-score" key={trait}>
              <span><strong>{traitLabels[trait]}</strong><em>{score}%</em></span>
              <div><i style={{ width: `${score}%` }} /></div>
            </div>
          ))}
        </div>
        <aside className="trip-dna-signals">
          <span className="eyebrow">WHY THIS RESULT</span>
          {dna.data.signals.map((signal) => <p key={signal}><Sparkles size={13} /> {signal}</p>)}
        </aside>
      </div>
    </section>
  );
}
