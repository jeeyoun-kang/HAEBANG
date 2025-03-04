package com.haebang.haebang.utils;

import com.haebang.haebang.constant.CustomErrorCode;
import com.haebang.haebang.entity.Member;
import com.haebang.haebang.exception.CustomException;
import com.haebang.haebang.repository.MemberRepository;
import com.haebang.haebang.service.RedisService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {
    private final Key key;
    private final RedisService redisService;
    private final MemberRepository memberRepository;

    public JwtProvider(@Value("${jwt.secret}") String secretKey,
                       RedisService redisService, MemberRepository memberRepository){
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(keyBytes);
        this.redisService = redisService;
        this.memberRepository = memberRepository;
    }

    /**
     * @param dto 로그인 한 사람의 정보
     * @param typ ATK, RFT 타입
     * @param duration 1시간 단위 -> 2분 단위로 잠시 바꿈
     * @return 토큰
     */
    public String createToken(Member dto, String typ, Long duration){
        Claims claims = Jwts.claims();
        claims.put("username", dto.getUsername());
        claims.put("auth", dto.getRole());
        claims.put("email", dto.getEmail());

        String token = Jwts.builder()
                .setClaims(claims)
                .setHeaderParam("typ", typ)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + duration * 1000*60*60L))// 한시간단위
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return token;
    }

    public String getUsername(String token){
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody().get("username", String.class);
    }
    public String getEmail(String token){
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody().get("email", String.class);
    }

    public UserDetails getUserDetails(String username){
        return memberRepository.findByUsername(username).get();
    }

    public boolean validateToken(String token){
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("유효하지 않은 Jwt Token");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT Token", e);
            new CustomException(CustomErrorCode.EXPIRED_ACCESS_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.info("지원하지않는 JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("토큰이 비어있는 예외", e);
            new CustomException(CustomErrorCode.EMPTY_ACCESS_TOKEN);
        }
        return false;
    }

    public Long getExpireTime(String token){
        Date expire = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
                .getBody().getExpiration();
        return expire.getTime();
    }
    public String getTokenType(String token){
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getHeader().getType();
    }

    public String getValueFromToken(String token) throws CustomException{
        String value = redisService.getStringValue(token);

        if(getTokenType(token).equals("RTK")){
            if(value==null) throw new CustomException(CustomErrorCode.EXPIRED_REFRESH_TOKEN);
        }
        else if(getTokenType(token).equals("ATK")){
            if(value=="logout") throw new CustomException(CustomErrorCode.EXPIRED_ACCESS_TOKEN);
        }
        return value;
    }

    /**
     * @param token ATK, RTK
     * @param value logout, email
     * @apiNote 로그아웃시 atk저장, 로그인시 rtk 저장
     */
    public void setTokenValueAndTime(String token, String value){
        Long duration = getExpireTime(token) - new Date(System.currentTimeMillis()).getTime();
        redisService.setStringValueExpire(token, value, Duration.ofMillis(duration));
    }

}
