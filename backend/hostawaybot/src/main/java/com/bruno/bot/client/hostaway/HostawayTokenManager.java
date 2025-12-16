package com.bruno.bot.client.hostaway;

import com.bruno.bot.config.HostawayProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class HostawayTokenManager {

    private final HostawayAuthClient authClient;
    private final HostawayProperties props;

    private String token;
    private Instant tokenExpiresAt;

    public HostawayTokenManager(HostawayAuthClient authClient, HostawayProperties props) {
        this.authClient = authClient;
        this.props = props;
    }

    public synchronized String getToken() {
        if (!props.enabled()) {
            throw new IllegalStateException("Hostaway disabled (hostaway.enabled=false)");
        }
        if (props.accountId() == null || props.accountId().isBlank()
                || props.clientSecret() == null || props.clientSecret().isBlank()) {
            throw new IllegalStateException("Missing Hostaway credentials (HOSTAWAY_ACCOUNT_ID / HOSTAWAY_CLIENT_SECRET)");
        }

        if (token == null || tokenExpiresAt == null || Instant.now().isAfter(tokenExpiresAt.minusSeconds(60))) {
            var res = authClient.getAccessToken(props.accountId(), props.clientSecret());
            token = res.access_token();
            tokenExpiresAt = Instant.now().plusSeconds(res.expires_in());
        }
        return token;
    }

    public synchronized void invalidate() {
        token = null;
        tokenExpiresAt = null;
    }
}
