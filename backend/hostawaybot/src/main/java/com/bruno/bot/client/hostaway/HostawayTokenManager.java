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

    // margen para renovar antes de expirar (recomendado 90s)
    private static final long REFRESH_SKEW_SECONDS = 90L;

    public HostawayTokenManager(HostawayAuthClient authClient, HostawayProperties props) {
        this.authClient = authClient;
        this.props = props;
    }

    /**
     * Indica si Hostaway está habilitado y con credenciales mínimas presentes.
     * Útil para que BotService decida DEMO vs HOSTAWAY sin depender de excepciones.
     */
    public boolean isReady() {
        return props.enabled()
                && props.accountId() != null && !props.accountId().isBlank()
                && props.clientSecret() != null && !props.clientSecret().isBlank();
    }

    public synchronized String getToken() {
        if (!props.enabled()) {
            throw new IllegalStateException("Hostaway disabled (hostaway.enabled=false)");
        }
        if (props.accountId() == null || props.accountId().isBlank()
                || props.clientSecret() == null || props.clientSecret().isBlank()) {
            throw new IllegalStateException("Missing Hostaway credentials (HOSTAWAY_ACCOUNT_ID / HOSTAWAY_CLIENT_SECRET)");
        }

        Instant now = Instant.now();

        // renovar si no existe o si expira pronto
        if (token == null || tokenExpiresAt == null || now.isAfter(tokenExpiresAt.minusSeconds(REFRESH_SKEW_SECONDS))) {
            var res = authClient.getAccessToken(props.accountId(), props.clientSecret());

            if (res == null || res.access_token() == null || res.access_token().isBlank()) {
                throw new IllegalStateException("Hostaway auth returned empty access_token");
            }

            long expiresIn = res.expires_in();
            // defensa: si Hostaway devuelve algo raro, no dejamos token "eterno" ni "ya vencido"
            if (expiresIn <= 0) {
                // fallback conservador: 15 minutos
                expiresIn = 15 * 60L;
            }

            token = res.access_token().trim();
            tokenExpiresAt = now.plusSeconds(expiresIn);
        }

        return token;
    }

    public synchronized void invalidate() {
        token = null;
        tokenExpiresAt = null;
    }
}
