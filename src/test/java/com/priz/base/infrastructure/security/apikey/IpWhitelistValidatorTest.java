package com.priz.base.infrastructure.security.apikey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpWhitelistValidatorTest {

    private IpWhitelistValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IpWhitelistValidator();
    }

    @Test
    void isAllowed_shouldReturnTrue_whenAllowIpsIsNull() {
        assertThat(validator.isAllowed("1.2.3.4", null)).isTrue();
    }

    @Test
    void isAllowed_shouldReturnTrue_whenAllowIpsIsBlank() {
        assertThat(validator.isAllowed("1.2.3.4", "   ")).isTrue();
    }

    @Test
    void isAllowed_shouldReturnTrue_whenExactIpMatches() {
        assertThat(validator.isAllowed("192.168.1.10", "192.168.1.10")).isTrue();
    }

    @Test
    void isAllowed_shouldReturnFalse_whenExactIpDoesNotMatch() {
        assertThat(validator.isAllowed("192.168.1.11", "192.168.1.10")).isFalse();
    }

    @Test
    void isAllowed_shouldReturnTrue_whenIpInCommaSeparatedList() {
        assertThat(validator.isAllowed("10.0.0.5", "10.0.0.1,10.0.0.5,10.0.0.9")).isTrue();
    }

    @Test
    void isAllowed_shouldReturnFalse_whenIpNotInList() {
        assertThat(validator.isAllowed("10.0.0.99", "10.0.0.1,10.0.0.5")).isFalse();
    }

    @Test
    void isAllowed_shouldReturnTrue_whenIpMatchesCidrBlock() {
        assertThat(validator.isAllowed("192.168.1.50", "192.168.1.0/24")).isTrue();
    }

    @Test
    void isAllowed_shouldReturnFalse_whenIpOutsideCidrBlock() {
        assertThat(validator.isAllowed("192.168.2.1", "192.168.1.0/24")).isFalse();
    }

    @Test
    void isAllowed_shouldReturnTrue_whenIpMatchesCidr16() {
        assertThat(validator.isAllowed("10.20.99.1", "10.20.0.0/16")).isTrue();
    }

    @Test
    void isAllowed_shouldReturnFalse_whenIpOutsideCidr16() {
        assertThat(validator.isAllowed("10.21.0.1", "10.20.0.0/16")).isFalse();
    }

    @Test
    void isAllowed_shouldReturnTrue_whenIpMatchesOneCidrInMixedList() {
        assertThat(validator.isAllowed("172.16.0.5", "192.168.1.0/24,172.16.0.0/12")).isTrue();
    }

    @Test
    void isAllowed_shouldHandleSpacesInList() {
        assertThat(validator.isAllowed("10.0.0.1", "10.0.0.1 , 10.0.0.2")).isTrue();
    }
}
