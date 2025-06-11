package organizationmanagement.security;



import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Component
public class JwtTokenUtil {
    private final Key signingKey;

    public JwtTokenUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractOrganizationId(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String orgId = (String) claims.get("organizationId");
            return orgId != null ? UUID.fromString(orgId) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            List<String> authorities = (List<String>) claims.get("authorities");
            return authorities != null ? authorities : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean hasOrganizationId(String token) {
        UUID orgId = extractOrganizationId(token);
        return orgId != null;
    }
}