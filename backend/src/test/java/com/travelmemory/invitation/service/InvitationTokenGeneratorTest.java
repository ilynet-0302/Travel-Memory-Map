package com.travelmemory.invitation.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationTokenGeneratorTest {

    private final InvitationTokenGenerator tokenGenerator = new InvitationTokenGenerator();

    @Test
    void generatesStrongUrlSafeTokensAndStableHashes() {
        String first = tokenGenerator.generateRawToken();
        String second = tokenGenerator.generateRawToken();

        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+").isNotEqualTo(second);
        assertThat(tokenGenerator.hashToken(first)).hasSize(64).matches("[a-f0-9]+");
        assertThat(tokenGenerator.hashToken(first)).isEqualTo(tokenGenerator.hashToken(first));
    }
}
