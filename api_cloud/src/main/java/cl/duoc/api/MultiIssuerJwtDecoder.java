package cl.duoc.api;

import com.nimbusds.jwt.JWTParser;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Valida tokens de varios emisores de Microsoft Entra: el tenant de la organización
 * (administradores) y el tenant de cuentas personales Microsoft (clientes).
 * Elige el decodificador según el claim "iss" y exige el audience de la API.
 */
public class MultiIssuerJwtDecoder implements JwtDecoder {

    private final Map<String, JwtDecoder> decoders = new HashMap<>();

    public MultiIssuerJwtDecoder(List<String> issuers, String audience) {
        for (String issuer : issuers) {
            // Las claves públicas de Microsoft están en /discovery/v2.0/keys del mismo tenant (se descargan al primer uso)
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withJwkSetUri(issuer.replace("/v2.0", "/discovery/v2.0/keys"))
                    .build();

            OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD, aud -> aud != null && aud.contains(audience));

            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
            decoders.put(issuer, decoder);
        }
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String issuer;
        try {
            issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            throw new BadJwtException("Token mal formado", e);
        }
        JwtDecoder decoder = issuer == null ? null : decoders.get(issuer);
        if (decoder == null) {
            throw new BadJwtException("Emisor no confiable: " + issuer);
        }
        return decoder.decode(token);
    }
}
