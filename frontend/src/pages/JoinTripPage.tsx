import { ArrowRight, CalendarDays, Globe2, LockKeyhole, MapPin, ShieldCheck, UserRoundPlus } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../features/auth/context/AuthContext';
import { useAcceptInvite, useInvitePreview } from '../features/collaboration/hooks/useCollaboration';
import { formatDateRange } from '../utils/date';

export function JoinTripPage() {
  const { inviteToken = '' } = useParams();
  const { demoMode, loading: authLoading, session } = useAuth();
  const invitation = useInvitePreview(inviteToken);
  const acceptInvite = useAcceptInvite(inviteToken);
  const authenticated = demoMode || Boolean(session);

  if (invitation.isLoading || authLoading) {
    return <div className="auth-loading"><span className="auth-loading__mark">◎</span><span>Reading the invitation…</span></div>;
  }

  if (!invitation.data || invitation.error) {
    return (
      <main className="join-page">
        <section className="join-card join-card--error">
          <span className="join-card__mark"><LockKeyhole size={25} /></span>
          <span className="eyebrow">INVITATION UNAVAILABLE</span>
          <h1>This route has closed.</h1>
          <p>{invitation.error?.message ?? 'This invitation may have expired, been revoked or reached its usage limit.'}</p>
          <Link className="button button--dark" to="/">Return to your trips</Link>
        </section>
      </main>
    );
  }

  const preview = invitation.data;
  const accepted = acceptInvite.data;

  return (
    <main className="join-page">
      <Link className="join-brand" to="/">
        <span><Globe2 size={21} /></span>
        <strong>Travel <small>memory map</small></strong>
      </Link>
      <section className="join-card">
        <div className="join-card__art" aria-hidden="true"><span /><span /><span /></div>
        <div className="join-card__content">
          {accepted ? (
            <>
              <span className="join-card__mark join-card__mark--success"><ShieldCheck size={26} /></span>
              <span className="eyebrow">WELCOME ABOARD</span>
              <h1>You’re part of the journey.</h1>
              <p>You joined as an {accepted.role.toLowerCase()}. The trip is now waiting in your travel journal.</p>
              <Link className="button button--coral button--full" to={`/trips/${accepted.tripId}`}>
                Open the trip <ArrowRight size={16} />
              </Link>
            </>
          ) : (
            <>
              <span className="join-card__mark"><UserRoundPlus size={26} /></span>
              <span className="eyebrow">YOU’RE INVITED</span>
              <h1>Join “{preview.tripTitle}”</h1>
              <p><strong>{preview.invitedBy}</strong> invited you to help shape this travel story.</p>

              <div className="join-details">
                <span><MapPin size={17} /><small>DESTINATION</small><strong>{preview.city}, {preview.country}</strong></span>
                <span><CalendarDays size={17} /><small>TRAVEL DATES</small><strong>{formatDateRange(preview.startDate, preview.endDate)}</strong></span>
                <span><ShieldCheck size={17} /><small>YOUR ACCESS</small><strong>{preview.role.toLowerCase()}</strong></span>
              </div>

              {acceptInvite.error && <p className="join-error">{acceptInvite.error.message}</p>}
              {authenticated ? (
                <button
                  className="button button--coral button--full"
                  type="button"
                  disabled={acceptInvite.isPending}
                  onClick={() => acceptInvite.mutate()}
                >
                  {acceptInvite.isPending ? 'Joining…' : 'Join this journey'} <ArrowRight size={16} />
                </button>
              ) : (
                <Link className="button button--coral button--full" to="/login" state={{ from: `/join/${inviteToken}` }}>
                  Sign in to join <ArrowRight size={16} />
                </Link>
              )}
              <small className="join-expiry">This limited invitation expires {new Date(preview.expiresAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })}.</small>
            </>
          )}
        </div>
      </section>
    </main>
  );
}
