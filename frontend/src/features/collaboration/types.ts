import type { TripRole } from '../trips/types';

export interface TripMember {
  memberId: string;
  userId: string;
  displayName: string;
  avatarUrl: string | null;
  role: TripRole;
  joinedAt: string;
}

export type InviteStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED' | 'USAGE_LIMIT_REACHED';

export interface TripInvite {
  id: string;
  role: Exclude<TripRole, 'OWNER'>;
  expiresAt: string;
  maxUses: number;
  useCount: number;
  status: InviteStatus;
  createdAt: string;
}

export interface CreatedTripInvite extends TripInvite {
  token: string;
}

export interface CreateInviteInput {
  role: Exclude<TripRole, 'OWNER'>;
  expiresInDays: number;
  maxUses: number;
}

export interface InvitePreview {
  tripId: string;
  tripTitle: string;
  country: string;
  city: string;
  startDate: string;
  endDate: string;
  invitedBy: string;
  role: Exclude<TripRole, 'OWNER'>;
  expiresAt: string;
}

export interface AcceptInviteResult {
  tripId: string;
  memberId: string;
  role: Exclude<TripRole, 'OWNER'>;
}
