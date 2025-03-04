package com.haebang.haebang.configuration;
import com.haebang.haebang.utils.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic().disable()
                .csrf().disable() // cross site 도메인 다를때 허용
                .cors() //cross site
                .and()
                .headers().frameOptions().disable()// h2 사용 iframe 허용
                .and()
                .authorizeRequests()
                .antMatchers("/static/css/**", "/static/js/**", "/static/img/**", "/*").permitAll()
                .antMatchers(HttpMethod.GET, "/chat/room").permitAll()
                .antMatchers(HttpMethod.GET,  "/chat/rooms").authenticated() //채팅 페이지 접근권한 설정
                .antMatchers("/main", "/map").permitAll()
                .antMatchers(HttpMethod.GET, "/api/apt/item/*", "/api/apt/items", "/api/apt/items?**").permitAll()
                .antMatchers("/api/apt/item**").authenticated()
                .antMatchers("/api/member/test").authenticated()
                .antMatchers("/api/member/logout").permitAll()
                .antMatchers("/api/mypage/**").authenticated()
                .antMatchers("/api/member/*").anonymous()
                .antMatchers("/api/bookmark**").authenticated()
                .and()
                .formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
                // UsernamePasswordAuthenticationFilter 필터를 거치기 전에 jwt필터를 거치도록 설정
        return http.build();
    }

    @Bean
    public HttpFirewall defaultHttpFirewall(){
        return new DefaultHttpFirewall();
    }
}
