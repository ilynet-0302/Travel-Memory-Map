import { zodResolver } from '@hookform/resolvers/zod';
import {
  CalendarDays,
  Camera,
  Check,
  CircleDollarSign,
  Flag,
  Globe2,
  MapPin,
  PlaneTakeoff,
  Route,
  Sparkles,
  Trophy,
} from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useProfile, useUpdateProfile } from '../features/profile/hooks/useProfile';

const profileSchema = z.object({
  displayName: z.string().trim().min(2, 'Enter at least 2 characters.').max(80, 'Use at most 80 characters.'),
});

type ProfileForm = z.infer<typeof profileSchema>;

function initials(displayName: string) {
  return displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'TM';
}

export function ProfilePage() {
  const profile = useProfile();
  const updateProfile = useUpdateProfile();
  const form = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { displayName: '' },
  });

  useEffect(() => {
    if (profile.data) form.reset({ displayName: profile.data.displayName });
  }, [form, profile.data]);

  if (profile.isLoading) {
    return <div className="page"><div className="profile-skeleton" /></div>;
  }

  if (!profile.data || profile.error) {
    return (
      <div className="page">
        <div className="empty-state">
          <span><Globe2 size={28} /></span>
          <h2>We could not unfold your profile</h2>
          <p>{profile.error?.message ?? 'Try again in a moment.'}</p>
          <button className="button button--dark" type="button" onClick={() => void profile.refetch()}>Try again</button>
        </div>
      </div>
    );
  }

  const { statistics } = profile.data;
  const { personality } = profile.data;
  const personalityScores = Object.entries(personality.scores)
    .sort((left, right) => right[1] - left[1])
    .slice(0, 5);
  const stats = [
    { label: 'Countries', value: statistics.countriesVisited, icon: Flag, tone: 'sage' },
    { label: 'Cities', value: statistics.citiesVisited, icon: MapPin, tone: 'coral' },
    { label: 'Trips', value: statistics.trips, icon: PlaneTakeoff, tone: 'sand' },
    { label: 'Places', value: statistics.placesVisited, icon: Route, tone: 'blue' },
    { label: 'Travel days', value: statistics.travelDays, icon: CalendarDays, tone: 'sage' },
    { label: 'Photos', value: statistics.photosUploaded, icon: Camera, tone: 'coral' },
  ];

  const submit = form.handleSubmit(async (values) => {
    await updateProfile.mutateAsync(values);
    form.reset(values);
  });

  return (
    <div className="page page--profile">
      <header className="page-header">
        <div>
          <span className="eyebrow">TRAVEL PROFILE</span>
          <h1>Your journey, in numbers</h1>
          <p>A profile that grows from the places you actually save.</p>
        </div>
      </header>

      <section className="profile-layout">
        <article className="profile-identity-card">
          <div className="profile-avatar-wrap">
            {profile.data.avatarUrl ? (
              <img className="profile-avatar" src={profile.data.avatarUrl} alt="" />
            ) : (
              <span className="profile-avatar profile-avatar--initials">{initials(profile.data.displayName)}</span>
            )}
            <span className="profile-avatar-action" title="Secure photo upload arrives with Supabase Storage">
              <Camera size={15} />
            </span>
          </div>
          <div className="profile-identity-copy">
            <span className="eyebrow">TRAVELLER SINCE {new Date(profile.data.createdAt).getFullYear()}</span>
            <h2>{profile.data.displayName}</h2>
            <p>{profile.data.email}</p>
          </div>
          <div className="profile-personality-preview">
            <Sparkles size={18} />
            <div>
              <small>TRAVEL PERSONALITY</small>
              <strong>{personality.type}</strong>
              <p>{personality.description}</p>
            </div>
          </div>
        </article>

        <article className="profile-settings-card">
          <div className="profile-card-heading">
            <div>
              <span className="eyebrow">PERSONAL DETAILS</span>
              <h2>How travellers see you</h2>
            </div>
          </div>
          <form onSubmit={(event) => void submit(event)}>
            <label className="field">
              <span>Display name</span>
              <input {...form.register('displayName')} />
              {form.formState.errors.displayName && <small className="field__error">{form.formState.errors.displayName.message}</small>}
            </label>
            <label className="field">
              <span>Email address</span>
              <input value={profile.data.email} readOnly />
              <small>Managed by your Supabase account.</small>
            </label>
            {updateProfile.error && <p className="members-error">{updateProfile.error.message}</p>}
            {updateProfile.isSuccess && !form.formState.isDirty && <p className="profile-save-success"><Check size={14} /> Profile saved</p>}
            <button className="button button--coral" type="submit" disabled={updateProfile.isPending || !form.formState.isDirty}>
              {updateProfile.isPending ? 'Saving…' : 'Save profile'}
            </button>
          </form>
        </article>
      </section>

      <section className="profile-dna-section">
        <article className="profile-dna-copy">
          <span className="eyebrow">YOUR TRAVEL DNA</span>
          <h2>{personality.revealed ? personality.type : 'A personality in progress'}</h2>
          <p>{personality.description}</p>
          <span className="profile-dna-progress">
            <Sparkles size={14} /> {personality.completedTrips} / {personality.completedTripsRequired} completed trips
          </span>
        </article>
        <div className="profile-dna-scores">
          {personalityScores.map(([trait, score]) => (
            <div key={trait}>
              <span><strong>{trait}</strong><em>{score}%</em></span>
              <i><b style={{ width: `${score}%` }} /></i>
            </div>
          ))}
        </div>
      </section>

      <section className="profile-stats-section">
        <div className="section-heading">
          <div>
            <span className="eyebrow">YOUR FOOTPRINT</span>
            <h2>Travel statistics</h2>
          </div>
          <small>Calculated by the backend from your accessible trips.</small>
        </div>
        <div className="profile-stats-grid">
          {stats.map(({ label, value, icon: Icon, tone }) => (
            <article className="stat-card" key={label}>
              <span className={`stat-card__icon stat-card__icon--${tone}`}><Icon size={19} /></span>
              <span className="stat-card__value">{value}</span>
              <span className="stat-card__label">{label}</span>
            </article>
          ))}
        </div>
      </section>

      <section className="profile-analytics-section">
        <div className="section-heading">
          <div><span className="eyebrow">TRAVEL INTELLIGENCE</span><h2>Patterns across your journeys</h2></div>
          <small>Ratings and currencies are never mixed into misleading totals.</small>
        </div>
        <div className="profile-analytics-grid">
          {statistics.spending.map((spending) => (
            <article className="profile-spending-card" key={spending.currency}>
              <span className="profile-analytics-icon"><CircleDollarSign size={19} /></span>
              <small>TOTAL SPENT · {spending.currency}</small>
              <strong>{new Intl.NumberFormat('en', { style: 'currency', currency: spending.currency }).format(spending.totalSpent)}</strong>
              <div><span>Per trip <b>{new Intl.NumberFormat('en', { style: 'currency', currency: spending.currency }).format(spending.averageCostPerTrip)}</b></span><span>Per day <b>{new Intl.NumberFormat('en', { style: 'currency', currency: spending.currency }).format(spending.averageCostPerDay)}</b></span></div>
              <p>Highest: {spending.mostExpensiveTrip.title}</p>
            </article>
          ))}
          <article className="profile-highlights-card">
            <span className="profile-analytics-icon profile-analytics-icon--coral"><Trophy size={19} /></span>
            <small>JOURNEY HIGHLIGHTS</small>
            <dl>
              <div><dt>Favourite country</dt><dd>{statistics.favouriteCountry?.name ?? 'Add trip ratings'}</dd></div>
              <div><dt>Favourite city</dt><dd>{statistics.favouriteCity?.name ?? 'Add trip ratings'}</dd></div>
              <div><dt>Most visited</dt><dd>{statistics.mostVisitedCountry?.name ?? 'No journeys yet'}</dd></div>
              <div><dt>Longest trip</dt><dd>{statistics.longestTrip ? `${statistics.longestTrip.title} · ${statistics.longestTrip.travelDays} days` : 'No completed trips'}</dd></div>
              <div><dt>Shortest trip</dt><dd>{statistics.shortestTrip ? `${statistics.shortestTrip.title} · ${statistics.shortestTrip.travelDays} days` : 'No completed trips'}</dd></div>
            </dl>
          </article>
        </div>
      </section>
    </div>
  );
}
