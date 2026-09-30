package com.exemplo.authservice.service;

import com.exemplo.authservice.dto.LoginRequest;
import com.exemplo.authservice.dto.LoginResponse;
import com.exemplo.authservice.dto.UsuarioRequest;
import com.exemplo.authservice.model.Usuario;
import com.exemplo.authservice.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtToken jwt;
    private final RefreshTokens refreshTokens;

    public Usuario cadastrar(UsuarioRequest request) {
        validarSenha(request.getSenha());
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ja cadastrado");
        }

        Usuario usuario = new Usuario(
                request.getNome(),
                request.getEmail(),
                passwordEncoder.encode(request.getSenha()));

        return usuarioRepository.save(usuario);
    }

    public Usuario autenticar(LoginRequest request){
        validarSenha(request.getSenha());
        Usuario usuario = this.usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(()-> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"E-mail ou senha inválidos"));
    
            if(!passwordEncoder.matches(request.getSenha(),usuario.getSenha())){
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"E-mail ou senha inválidos");
            }

            return usuario;
    
        }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        return tokens(autenticar(request));
    }

    @Transactional
    public LoginResponse refresh(String token) {
        Long id = refreshTokens.consume(token).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido"));
        Usuario user = usuarioRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido"));
        return tokens(user);
    }

    private LoginResponse tokens(Usuario user) {
        return new LoginResponse(jwt.gerarToken(user), refreshTokens.issue(user.getId()),
                "Bearer", jwt.expiresIn());
    }

    private void validarSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha longa demais");
        }
    }
}
