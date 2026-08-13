package com.travelmemory.auth;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String displayName) {
}
