import { zodResolver } from '@hookform/resolvers/zod';
import {
  CalendarDays,
  Camera,
  Check,
  Flag,
  Globe2,
  MapPin,
  PlaneTakeoff,
  Route,
  Sparkles,
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
              <strong>Still taking shape</strong>
              <p>Complete more trips to reveal your deterministic Travel DNA.</p>
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
    </div>
  );
}
