import { ApiError, apiClient } from '../../../services/apiClient';
import type {
  AcceptInviteResult,
  CreatedTripInvite,
  CreateInviteInput,
  InvitePreview,
  TripInvite,
  TripMember,
} from '../types';
import type { TripRole } from '../../trips/types';
import { demoTrips } from '../../trips/data/demoTrips';

const demoMode = import.meta.env.VITE_DEMO_MODE !== 'false';
const wait = (milliseconds = 140) =>
  new Promise<void>((resolve) => window.setTimeout(resolve, milliseconds));

const demoMembers: Record<string, TripMember[]> = {
  'rome-2026': [
    {
      memberId: 'member-owner',
      userId: 'demo-owner',
      displayName: 'Iliya Petrov',
      avatarUrl: null,
      role: 'OWNER',
      joinedAt: '2026-02-10T09:00:00Z',
    },
    {
      memberId: 'member-maya',
      userId: 'demo-maya',
      displayName: 'Maya Stoyanova',
      avatarUrl: null,
      role: 'EDITOR',
      joinedAt: '2026-02-11T11:30:00Z',
    },
    {
      memberId: 'member-alex',
      userId: 'demo-alex',
      displayName: 'Alex Marinov',
      avatarUrl: null,
      role: 'VIEWER',
      joinedAt: '2026-02-12T15:10:00Z',
    },
  ],
};

let demoInvites: Record<string, TripInvite[]> = { 'rome-2026': [] };
const demoInviteTokens: Record<string, string> = {};
const demoPreview: InvitePreview = {
  tripId: 'rome-2026',
  tripTitle: 'A Roman Holiday',
  country: 'Italy',
  city: 'Rome',
  startDate: '2026-09-12',
  endDate: '2026-09-16',
  invitedBy: 'Iliya Petrov',
  role: 'EDITOR',
  expiresAt: '2026-09-01T18:00:00Z',
};
const demoPreviews: Record<string, InvitePreview> = { 'demo-editor-rome': demoPreview };

function requireDemoInvite(token: string) {
  const preview = demoPreviews[token];
  if (!preview) {
    throw new ApiError('This invitation link is invalid or no longer exists.', 404, 'INVITE_NOT_FOUND');
  }
  return preview;
}

export const collaborationApi = {
  async listMembers(tripId: string): Promise<TripMember[]> {
    if (!demoMode) return apiClient<TripMember[]>(`/trips/${tripId}/members`);
    await wait();
    return [...(demoMembers[tripId] ?? [])];
  },

  async updateMemberRole(tripId: string, memberId: string, role: TripRole): Promise<TripMember> {
    if (!demoMode) {
      return apiClient<TripMember>(`/trips/${tripId}/members/${memberId}`, {
        method: 'PATCH',
        body: JSON.stringify({ role }),
      });
    }
    await wait();
    const member = (demoMembers[tripId] ?? []).find(({ memberId: id }) => id === memberId);
    if (!member) throw new ApiError('This trip member does not exist.', 404, 'TRIP_MEMBER_NOT_FOUND');
    member.role = role;
    return { ...member };
  },

  async removeMember(tripId: string, memberId: string): Promise<void> {
    if (!demoMode) return apiClient<void>(`/trips/${tripId}/members/${memberId}`, { method: 'DELETE' });
    await wait();
    demoMembers[tripId] = (demoMembers[tripId] ?? []).filter(({ memberId: id }) => id !== memberId);
  },

  async leaveTrip(tripId: string): Promise<void> {
    if (!demoMode) return apiClient<void>(`/trips/${tripId}/members/me`, { method: 'DELETE' });
    await wait();
  },

  async listInvites(tripId: string): Promise<TripInvite[]> {
    if (!demoMode) return apiClient<TripInvite[]>(`/trips/${tripId}/invites`);
    await wait();
    return [...(demoInvites[tripId] ?? [])];
  },

  async createInvite(tripId: string, input: CreateInviteInput): Promise<CreatedTripInvite> {
    if (!demoMode) {
      return apiClient<CreatedTripInvite>(`/trips/${tripId}/invites`, {
        method: 'POST',
        body: JSON.stringify(input),
      });
    }
    await wait(220);
    const createdAt = new Date();
    const token = `demo-${input.role.toLowerCase()}-${crypto.randomUUID()}`;
    const invite: CreatedTripInvite = {
      id: crypto.randomUUID(),
      token,
      role: input.role,
      expiresAt: new Date(createdAt.getTime() + input.expiresInDays * 86_400_000).toISOString(),
      maxUses: input.maxUses,
      useCount: 0,
      status: 'ACTIVE',
      createdAt: createdAt.toISOString(),
    };
    const listedInvite: TripInvite = {
      id: invite.id,
      role: invite.role,
      expiresAt: invite.expiresAt,
      maxUses: invite.maxUses,
      useCount: invite.useCount,
      status: invite.status,
      createdAt: invite.createdAt,
    };
    demoInvites = { ...demoInvites, [tripId]: [listedInvite, ...(demoInvites[tripId] ?? [])] };
    demoInviteTokens[invite.id] = token;
    const trip = demoTrips.find(({ id }) => id === tripId);
    demoPreviews[token] = {
      tripId,
      tripTitle: trip?.title ?? 'Shared journey',
      country: trip?.country ?? 'Unknown destination',
      city: trip?.city ?? 'Somewhere new',
      startDate: trip?.startDate ?? createdAt.toISOString().slice(0, 10),
      endDate: trip?.endDate ?? createdAt.toISOString().slice(0, 10),
      invitedBy: 'Iliya Petrov',
      role: input.role,
      expiresAt: invite.expiresAt,
    };
    return invite;
  },

  async revokeInvite(tripId: string, inviteId: string): Promise<void> {
    if (!demoMode) return apiClient<void>(`/trips/${tripId}/invites/${inviteId}`, { method: 'DELETE' });
    await wait();
    demoInvites[tripId] = (demoInvites[tripId] ?? []).map((invite) =>
      invite.id === inviteId ? { ...invite, status: 'REVOKED' as const } : invite,
    );
    const revokedToken = demoInviteTokens[inviteId];
    if (revokedToken) {
      delete demoPreviews[revokedToken];
      delete demoInviteTokens[inviteId];
    }
  },

  async previewInvite(token: string): Promise<InvitePreview> {
    if (!demoMode) return apiClient<InvitePreview>(`/invites/${encodeURIComponent(token)}`);
    await wait();
    return requireDemoInvite(token);
  },

  async acceptInvite(token: string): Promise<AcceptInviteResult> {
    if (!demoMode) {
      return apiClient<AcceptInviteResult>(`/invites/${encodeURIComponent(token)}/accept`, { method: 'POST' });
    }
    await wait(260);
    const preview = requireDemoInvite(token);
    return { tripId: preview.tripId, memberId: 'demo-accepted-member', role: preview.role };
  },
};
