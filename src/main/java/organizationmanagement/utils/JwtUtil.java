package organizationmanagement.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
    private final String secret;
    private final long expiration;
    private final Key signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.secret = secret;
        this.expiration = expiration;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }


    public String generateToken(UserDetails userDetails, UUID organizationId) {
        Map<String, Object> claims = new HashMap<>();


        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));



        if (organizationId != null) {
            claims.put("organizationId", organizationId.toString());
        }

        return createToken(claims, userDetails.getUsername());
    }




    public String generateTokenWithOrganization(UserDetails userDetails, UUID organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        return generateToken(userDetails, organizationId);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
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


    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }


    public Boolean validateTokenWithOrganization(String token, UserDetails userDetails, UUID expectedOrgId) {
        if (!validateToken(token, userDetails)) {
            return false;
        }

        UUID tokenOrgId = extractOrganizationId(token);
        return Objects.equals(tokenOrgId, expectedOrgId);
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    public Boolean hasOrganizationId(String token) {
        UUID orgId = extractOrganizationId(token);
        return orgId != null;
    }
}