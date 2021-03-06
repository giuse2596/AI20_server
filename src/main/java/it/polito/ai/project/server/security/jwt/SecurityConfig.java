package it.polito.ai.project.server.security.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig  extends WebSecurityConfigurerAdapter {
    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //@formatter:off
        http
            .httpBasic().disable()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/auth/login", "/register/**", "/notification/**").permitAll()
            .antMatchers(HttpMethod.GET,"/API/students/**").hasAnyRole("STUDENT", "TEACHER")
            .antMatchers(HttpMethod.GET, "/API/students/{id}/assignments/{assignmentid}").hasRole("STUDENT")
            .antMatchers(HttpMethod.POST,"/API/students").hasRole("TEACHER")
            .antMatchers(HttpMethod.GET, "/API/courses/**").hasAnyRole("STUDENT", "TEACHER")
            .antMatchers(HttpMethod.GET, "/API/courses/teacher_courses").hasRole("TEACHER")
            .antMatchers(HttpMethod.GET, "/API/courses/{name}/assignments/{assignmentid}/image").hasRole("TEACHER")
            .antMatchers(HttpMethod.GET, "/API/courses/{name}/virtual_machine_model").hasRole("TEACHER")
            .antMatchers(HttpMethod.GET, "/API/courses/{name}/enabled_teams").hasRole("TEACHER")
            .antMatchers(HttpMethod.GET, "/API/courses/{name}/student_not_enabled_teams").hasRole("STUDENT")
            .antMatchers(HttpMethod.GET, "/API/courses/{name}/student_enabled_teams").hasRole("STUDENT")
            .antMatchers(HttpMethod.POST, "/API/courses/**").hasRole("TEACHER")
            .antMatchers(HttpMethod.PUT, "/API/courses/**").hasRole("TEACHER")
            .antMatchers(HttpMethod.DELETE, "/API/courses/**").hasRole("TEACHER")
            .anyRequest().authenticated()
            .and()
            .apply(new JwtConfigurer(jwtTokenProvider));
        //@formatter:on
    }

}
