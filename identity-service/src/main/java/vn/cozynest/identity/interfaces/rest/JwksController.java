package vn.cozynest.identity.interfaces.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.identity.infrastructure.security.RsaKeyProvider;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
public class JwksController {

    private final RsaKeyProvider rsaKeyProvider;

    public JwksController(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @GetMapping({"/api/v1/auth/jwks", "/.well-known/jwks.json"})
    public Map<String, Object> getJwks() {
        RSAPublicKey publicKey = rsaKeyProvider.getPublicKey();
        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", rsaKeyProvider.getKeyId(),
                "n", encodeBigInt(publicKey.getModulus()),
                "e", encodeBigInt(publicKey.getPublicExponent())
        );
        return Map.of("keys", List.of(jwk));
    }

    private static String encodeBigInt(BigInteger bigInt) {
        byte[] bytes = bigInt.toByteArray();
        if (bytes.length > 0 && bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

