package com.api.pgc.core.APIRestPGC.config.security;

import com.api.pgc.core.APIRestPGC.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static com.api.pgc.core.APIRestPGC.config.security.SecurityConstants.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
@EnableWebSecurity
public class WebSecurity  extends WebSecurityConfigurerAdapter {

    @Autowired
    @Qualifier("usuarioService")
    private UsuarioService usuarioDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint entryPoint;

    @Autowired
    private CustomLoginFailureHandler loginFailureHandler;


    @Bean
    public PasswordEncoder passwordEncoder(){
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //System.out.println("******* En WebSecurity 1 ******************  " + auth);
        auth.userDetailsService(usuarioDetailsService)
                .passwordEncoder(passwordEncoder()); //PassWord Encoder
    }


    /*@Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET","POST", "OPTIONS", "PUT", "DELETE"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Content-type","Authorization"));
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }*/


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //Crea los Querys de Autenticacion
        http.csrf().disable().authorizeRequests()
                .antMatchers(LOGIN_URL, "/rest/estados",
                        "/rest/registro",
                        "/v2/api-docs", "/configuration/ui", "/swagger-resources", "/configuration/security",
                        "/swagger-ui.html", "/webjars/**", "/swagger-resources/configuration/ui", "/swagger-ui.html",
                        "/swagger-resources/configuration/security")
                    .permitAll() //permitimos el acceso a /login a cualquiera
                    .anyRequest().authenticated() //cualquier otra peticion requiere autenticacion
                .and()
                //path del login
                .formLogin()
                    //.failureHandler(loginFailureHandler)
                    .loginPage(LOGIN_URL)
                //.defaultSuccessUrl("/", true)
                //.failureUrl("/login?error")
                .permitAll()
                .and()
                .exceptionHandling().authenticationEntryPoint(entryPoint)
                .and()
                // Las peticiones /login pasaran previamente por este filtro
                .addFilterBefore(new LoginFilter(LOGIN_URL, authenticationManager()),
                        UsernamePasswordAuthenticationFilter.class)

                // Las demás peticiones pasarán por este filtro para validar el token
                .addFilterBefore(new JwtFilter(),
                        UsernamePasswordAuthenticationFilter.class)
                .headers()
                // the headers you want here. This solved all my CORS problems!
                .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Origin", "*"))
                .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Methods", "GET","POST", "OPTIONS", "PUT", "DELETE"))
                .addHeaderWriter(new StaticHeadersWriter("Access-Control-Max-Age", "3600"))
                .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Credentials", "true"))
                .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Headers", "Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization"));
    }

    private AuthenticationSuccessHandler successHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
                httpServletResponse.getWriter().append("OK");
                httpServletResponse.setStatus(200);
            }
        };
    }

    private AuthenticationFailureHandler failureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
                httpServletResponse.getWriter().append("Authentication failure");
                httpServletResponse.setStatus(401);
            }
        };
    }

    @Bean
    public CustomLoginFailureHandler authenticationHandlerBean() {
        return new CustomLoginFailureHandler();
    }

    //Creamos una mascara para El PassWordEncoder
    private PasswordEncoder getPasswordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence charSequence) {
                return charSequence.toString();
            }

            @Override
            public boolean matches(CharSequence charSequence, String s) {
                return true;
            }
        };
    }


}
