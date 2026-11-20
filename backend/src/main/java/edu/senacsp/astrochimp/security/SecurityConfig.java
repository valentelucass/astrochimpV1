package edu.senacsp.astrochimp.security;

import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.repository.ProfessorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Optional;

import static org.springframework.security.config.Customizer.withDefaults;

// @Configuration faz o Spring ler esta classe para configurar o acesso às rotas e o login do professor.
@Configuration
public class SecurityConfig {



    // Primeiro método serve para definir quais rotas exigem autenticação e ativar o login HTTP Basic.

    
    @Bean // @Bean registra o filtro SecurityFilterChain no Spring para definir o acesso às rotas e o login HTTP Basic.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { // Define quais requisições exigem autenticação e ativa o HTTP Basic.
        http.cors(withDefaults());
        // Customizer entrega ao Spring um método de configuração.
        http.csrf(new Customizer<CsrfConfigurer<HttpSecurity>>() {
            // @Override implementa o método de configuração esperado pelo Spring.
            @Override
            public void customize(CsrfConfigurer<HttpSecurity> csrf) { // CSRF (Cross-Site Request Forgery): pedido forjado por outro site em nome do usuário.
                csrf.disable(); // Desativa a proteção CSRF contra pedidos forjados em nome do usuário
            }
        });
        http.authorizeHttpRequests(new Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>() {
            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
                auth.requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/professores").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/professores/publico/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/salas/entrar").permitAll()
                    // Os services conferem o token do aluno ou o professor dono nas rotas de salas.
                    .requestMatchers("/api/salas/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/duvidas/*/votos").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/materiais/*/arquivo").permitAll()
                    .anyRequest().authenticated(); // As demais requisições exigem autenticação.
            }
        });
        http.httpBasic(withDefaults()); // Ativa HTTP Basic: recebe e-mail e senha no cabeçalho para autenticar.
        return http.build(); // Monta e entrega ao Spring a cadeia de filtros de segurança.
    }




    // Segundo método serve para definir como o Spring vai buscar o professor no banco de dados pelo e-mail usado no login HTTP Basic.

    // Método de entrada do Spring para buscar o professor pelo e-mail usado no login HTTP Basic.
    // Este método é chamado pelo Spring Security quando um usuário tenta se autenticar com HTTP Basic.
    // O e-mail é passado como parâmetro e o método retorna um objeto UserDetails com as informações do professor.

    @Bean
    public UserDetailsService userDetailsService(ProfessorRepository professorRepository) {
        // Este UserDetailsService busca o professor pelo e-mail usado no login HTTP Basic.
        // O User devolvido contém o e-mail, a senha protegida no banco e o papel PROFESSOR para o Spring conferir o login.
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String email) {
                // Optional guarda o resultado da busca; isEmpty indica que o e-mail não está cadastrado.
                Optional<Professor> resultado = professorRepository.findByEmail(email);
                if (resultado.isEmpty()) {
                    throw new UsernameNotFoundException("Professor não encontrado");
                }
                Professor professor = resultado.get();
                return User.withUsername(professor.getEmail())
                    .password(professor.getSenha()).roles("PROFESSOR").build(); // build() monta o User com e-mail, senha e papel PROFESSOR para o Spring conferir o login.
            }
        };
    }





    // Terceiro método serve para definir como o Spring vai gerar e conferir o hash da senha do professor.
    // Configura o BCrypt usado para gerar e conferir o hash da senha.

    @Bean
    public PasswordEncoder passwordEncoder() {
        // O BCrypt gera um hash da senha no cadastro e compara a senha recebida no login com o hash salvo no banco.
        return new BCryptPasswordEncoder();
    }






    // Quarto método serve para definir de quais endereços a versão web pode acessar a API pelo navegador.
    // Este método configura o CORS (Cross-Origin Resource Sharing) para permitir que a versão web do Expo acesse a API.

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        

        // corsConfigurationSource() permite que a prévia web do Expo, na porta 8081, faça requisições à API, na porta 8080. 
        // Para o navegador, portas diferentes significam origens diferentes.

        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(List.of("http://localhost:8081", "http://127.0.0.1:8081"));
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Aluno-Token"));
        configuracao.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", configuracao);
        return fonte;
    }
}
