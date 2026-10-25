package edu.senacsp.astrochimp.security;

import edu.senacsp.astrochimp.repository.ProfessorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/professores").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/professores/publico/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/salas/entrar").permitAll()
                .requestMatchers("/api/salas/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/duvidas/*/votos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/materiais/*/arquivo").permitAll()
                .anyRequest().authenticated())
            .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(ProfessorRepository professorRepository) {
        return email -> professorRepository.findByEmail(email)
            .map(professor -> User.withUsername(professor.getEmail())
                .password(professor.getSenha()).roles("PROFESSOR").build())
            .orElseThrow(() -> new UsernameNotFoundException("Professor não encontrado"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
