import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tripKeys } from '../../trips/hooks/useTrips';
import { collaborationApi } from '../api/collaborationApi';
import type { CreateInviteInput } from '../types';
import type { TripRole } from '../../trips/types';

export const collaborationKeys = {
  members: (tripId: string) => ['trip-members', tripId] as const,
  invites: (tripId: string) => ['trip-invites', tripId] as const,
  invitePreview: (token: string) => ['invite-preview', token] as const,
};

export function useTripMembers(tripId: string) {
  return useQuery({
    queryKey: collaborationKeys.members(tripId),
    queryFn: () => collaborationApi.listMembers(tripId),
  });
}

export function useUpdateMemberRole(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ memberId, role }: { memberId: string; role: TripRole }) =>
      collaborationApi.updateMemberRole(tripId, memberId, role),
    onSuccess: (member) => {
      queryClient.setQueryData(
        collaborationKeys.members(tripId),
        (members: Awaited<ReturnType<typeof collaborationApi.listMembers>> | undefined) =>
          members?.map((candidate) => (candidate.memberId === member.memberId ? member : candidate)),
      );
    },
  });
}

export function useRemoveMember(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (memberId: string) => collaborationApi.removeMember(tripId, memberId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: collaborationKeys.members(tripId) }),
        queryClient.invalidateQueries({ queryKey: tripKeys.detail(tripId) }),
        queryClient.invalidateQueries({ queryKey: tripKeys.all }),
      ]);
    },
  });
}

export function useLeaveTrip(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => collaborationApi.leaveTrip(tripId),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: tripKeys.all }),
  });
}

export function useTripInvites(tripId: string, enabled: boolean) {
  return useQuery({
    queryKey: collaborationKeys.invites(tripId),
    queryFn: () => collaborationApi.listInvites(tripId),
    enabled,
  });
}

export function useCreateInvite(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateInviteInput) => collaborationApi.createInvite(tripId, input),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: collaborationKeys.invites(tripId) }),
  });
}

export function useRevokeInvite(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (inviteId: string) => collaborationApi.revokeInvite(tripId, inviteId),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: collaborationKeys.invites(tripId) }),
  });
}

export function useInvitePreview(token: string) {
  return useQuery({
    queryKey: collaborationKeys.invitePreview(token),
    queryFn: () => collaborationApi.previewInvite(token),
    retry: false,
  });
}

export function useAcceptInvite(token: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => collaborationApi.acceptInvite(token),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: tripKeys.all }),
  });
}
