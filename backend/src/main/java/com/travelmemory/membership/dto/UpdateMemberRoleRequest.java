package com.travelmemory.membership.dto;

import com.travelmemory.membership.entity.TripRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull TripRole role) {
}
