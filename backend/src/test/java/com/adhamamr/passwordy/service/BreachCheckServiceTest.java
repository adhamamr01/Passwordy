package com.adhamamr.passwordy.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BreachCheckServiceTest {

    @Mock PwnedRangeClient rangeClient;

    // SHA-1("password") = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8 → suffix after the 5-char prefix.
    private static final String PASSWORD_SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

    @Test
    void breachedPassword_isFlagged() {
        when(rangeClient.fetchRange(any())).thenReturn(Optional.of(
                "00000000000000000000000000000000001:3\r\n" + PASSWORD_SUFFIX + ":52579"));
        BreachCheckService service = new BreachCheckService(rangeClient, true);

        assertThat(service.isBreached("password")).isTrue();
    }

    @Test
    void cleanPassword_isNotFlagged() {
        when(rangeClient.fetchRange(any())).thenReturn(Optional.of(
                "00000000000000000000000000000000001:3\r\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA:1"));
        BreachCheckService service = new BreachCheckService(rangeClient, true);

        assertThat(service.isBreached("password")).isFalse();
    }

    @Test
    void failsOpenWhenHibpUnreachable() {
        when(rangeClient.fetchRange(any())).thenReturn(Optional.empty());
        BreachCheckService service = new BreachCheckService(rangeClient, true);

        assertThat(service.isBreached("password")).isFalse();
    }

    @Test
    void disabled_skipsLookupEntirely() {
        BreachCheckService service = new BreachCheckService(rangeClient, false);

        assertThat(service.isBreached("password")).isFalse();
        verifyNoInteractions(rangeClient);
    }
}
