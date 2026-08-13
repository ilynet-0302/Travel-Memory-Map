import {
  Check,
  Copy,
  Link2,
  LogOut,
  ShieldCheck,
  Trash2,
  UserRound,
  UserRoundCog,
  UsersRound,
  XCircle,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { TripRole } from '../../trips/types';
import {
  useCreateInvite,
  useLeaveTrip,
  useRemoveMember,
  useRevokeInvite,
  useTripInvites,
  useTripMembers,
  useUpdateMemberRole,
} from '../hooks/useCollaboration';
import type { CreatedTripInvite, CreateInviteInput } from '../types';

interface TripMembersPanelProps {
  tripId: string;
  currentUserRole: TripRole;
}

const roleDescription: Record<TripRole, string> = {
  OWNER: 'Full control of trip, people and invitations',
  EDITOR: 'Can add and edit stops, memories and expenses',
  VIEWER: 'Can explore the trip without making changes',
};

function initials(name: string) {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
}

export function TripMembersPanel({ tripId, currentUserRole }: TripMembersPanelProps) {
  const isOwner = currentUserRole === 'OWNER';
  const members = useTripMembers(tripId);
  const invites = useTripInvites(tripId, isOwner);
  const updateRole = useUpdateMemberRole(tripId);
  const removeMember = useRemoveMember(tripId);
  const leaveTrip = useLeaveTrip(tripId);
  const createInvite = useCreateInvite(tripId);
  const revokeInvite = useRevokeInvite(tripId);
  const navigate = useNavigate();
  const [inviteInput, setInviteInput] = useState<CreateInviteInput>({
    role: 'EDITOR',
    expiresInDays: 7,
    maxUses: 1,
  });
  const [createdInvite, setCreatedInvite] = useState<CreatedTripInvite | null>(null);
  const [copied, setCopied] = useState(false);

  const inviteUrl = useMemo(() => {
    if (!createdInvite) return '';
    const basePath = import.meta.env.BASE_URL.endsWith('/')
      ? import.meta.env.BASE_URL
      : `${import.meta.env.BASE_URL}/`;
    return `${window.location.origin}${basePath}join/${createdInvite.token}`;
  }, [createdInvite]);

  async function submitInvite(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const invitation = await createInvite.mutateAsync(inviteInput);
    setCreatedInvite(invitation);
    setCopied(false);
  }

  async function copyInvite() {
    await navigator.clipboard.writeText(inviteUrl);
    setCopied(true);
  }

  async function confirmRemove(memberId: string, displayName: string) {
    if (window.confirm(`Remove ${displayName} from this trip?`)) {
      await removeMember.mutateAsync(memberId);
    }
  }

  async function confirmLeave() {
    if (window.confirm('Leave this trip? You will need a new invitation to return.')) {
      await leaveTrip.mutateAsync();
      navigate('/trips');
    }
  }

  return (
    <section className="members-layout">
      <div className="members-card">
        <header className="members-card__header">
          <div>
            <span className="eyebrow">TRAVEL COMPANIONS</span>
            <h2>People on this journey</h2>
          </div>
          <span className="member-count"><UsersRound size={15} /> {members.data?.length ?? 0}</span>
        </header>

        {members.isLoading ? (
          <div className="members-loading">Gathering the travellers…</div>
        ) : members.error ? (
          <p className="members-error">{members.error.message}</p>
        ) : (
          <div className="member-list">
            {members.data?.map((member) => (
              <article className="member-row" key={member.memberId}>
                <span className="member-avatar">{initials(member.displayName)}</span>
                <span className="member-copy">
                  <strong>{member.displayName}</strong>
                  <small>{roleDescription[member.role]}</small>
                </span>
                {isOwner && member.role !== 'OWNER' ? (
                  <select
                    aria-label={`Role for ${member.displayName}`}
                    value={member.role}
                    disabled={updateRole.isPending}
                    onChange={(event) =>
                      updateRole.mutate({ memberId: member.memberId, role: event.target.value as TripRole })
                    }
                  >
                    <option value="EDITOR">Editor</option>
                    <option value="VIEWER">Viewer</option>
                  </select>
                ) : (
                  <span className={`role-badge role-badge--${member.role.toLowerCase()}`}>
                    {member.role === 'OWNER' && <ShieldCheck size={13} />}{member.role.toLowerCase()}
                  </span>
                )}
                {isOwner && member.role !== 'OWNER' && (
                  <button
                    className="member-remove"
                    type="button"
                    aria-label={`Remove ${member.displayName}`}
                    disabled={removeMember.isPending}
                    onClick={() => void confirmRemove(member.memberId, member.displayName)}
                  >
                    <Trash2 size={16} />
                  </button>
                )}
              </article>
            ))}
          </div>
        )}

        {!isOwner && (
          <footer className="members-card__footer">
            <button className="button button--ghost" type="button" onClick={() => void confirmLeave()}>
              <LogOut size={16} /> Leave trip
            </button>
          </footer>
        )}
      </div>

      <aside className="invite-card">
        {isOwner ? (
          <>
            <span className="invite-card__icon"><Link2 size={21} /></span>
            <span className="eyebrow">SECURE INVITATIONS</span>
            <h2>Invite a traveller</h2>
            <p>Create a limited link. The secret link is shown once and only its secure fingerprint is stored.</p>

            <form className="invite-form" onSubmit={(event) => void submitInvite(event)}>
              <label>
                <span>Access</span>
                <select
                  value={inviteInput.role}
                  onChange={(event) =>
                    setInviteInput((current) => ({ ...current, role: event.target.value as 'EDITOR' | 'VIEWER' }))
                  }
                >
                  <option value="EDITOR">Editor</option>
                  <option value="VIEWER">Viewer</option>
                </select>
              </label>
              <label>
                <span>Expires</span>
                <select
                  value={inviteInput.expiresInDays}
                  onChange={(event) =>
                    setInviteInput((current) => ({ ...current, expiresInDays: Number(event.target.value) }))
                  }
                >
                  <option value={1}>In 1 day</option>
                  <option value={7}>In 7 days</option>
                  <option value={14}>In 14 days</option>
                  <option value={30}>In 30 days</option>
                </select>
              </label>
              <label>
                <span>Maximum joins</span>
                <input
                  type="number"
                  min={1}
                  max={100}
                  value={inviteInput.maxUses}
                  onChange={(event) =>
                    setInviteInput((current) => ({ ...current, maxUses: Number(event.target.value) }))
                  }
                />
              </label>
              {createInvite.error && <p className="members-error">{createInvite.error.message}</p>}
              <button className="button button--coral button--full" type="submit" disabled={createInvite.isPending}>
                <UserRoundCog size={16} /> {createInvite.isPending ? 'Creating…' : 'Create invitation'}
              </button>
            </form>

            {createdInvite && (
              <div className="invite-result" role="status">
                <strong>Copy this link now</strong>
                <small>It will not be available again after you leave this page.</small>
                <code>{inviteUrl}</code>
                <button className="button button--dark button--full" type="button" onClick={() => void copyInvite()}>
                  {copied ? <Check size={16} /> : <Copy size={16} />}{copied ? 'Copied' : 'Copy invite link'}
                </button>
              </div>
            )}

            <div className="invite-list">
              <h3>Recent invitations</h3>
              {invites.isLoading ? (
                <small>Loading invitations…</small>
              ) : invites.data?.length ? (
                invites.data.map((invite) => (
                  <article key={invite.id}>
                    <span><UserRound size={16} /></span>
                    <div>
                      <strong>{invite.role.toLowerCase()} link</strong>
                      <small>{invite.useCount}/{invite.maxUses} joined · {invite.status.toLowerCase().replaceAll('_', ' ')}</small>
                    </div>
                    {invite.status === 'ACTIVE' && (
                      <button
                        type="button"
                        aria-label="Revoke invitation"
                        disabled={revokeInvite.isPending}
                        onClick={() => revokeInvite.mutate(invite.id)}
                      >
                        <XCircle size={17} />
                      </button>
                    )}
                  </article>
                ))
              ) : (
                <small>No invitations yet.</small>
              )}
            </div>
          </>
        ) : (
          <div className="invite-card__restricted">
            <span><ShieldCheck size={25} /></span>
            <h2>Invitations are owner-managed</h2>
            <p>You can see everyone on the trip. Only the owner can create invitation links or change roles.</p>
          </div>
        )}
      </aside>
    </section>
  );
}
