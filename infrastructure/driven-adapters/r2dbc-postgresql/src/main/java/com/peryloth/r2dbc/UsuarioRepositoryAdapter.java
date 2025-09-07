package com.peryloth.r2dbc;

import com.peryloth.model.rol.Rol;
import com.peryloth.model.usuario.Usuario;
import com.peryloth.model.usuario.gateways.UsuarioRepository;
import com.peryloth.r2dbc.entity.UsuarioEntity;
import com.peryloth.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigInteger;

@Repository
public class UsuarioRepositoryAdapter extends ReactiveAdapterOperations<
        Usuario,
        UsuarioEntity,
        BigInteger,
        UsuarioReactiveRepository
        > implements UsuarioRepository {

    private static final Logger log = LoggerFactory.getLogger(UsuarioRepositoryAdapter.class);

    public UsuarioRepositoryAdapter(UsuarioReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Usuario.class));
    }

    @Override
    @Transactional
    public Mono<Usuario> saveUsuario(Usuario usuario) {
        BigInteger rolId = usuario != null && usuario.getRol() != null ? usuario.getRol().getUniqueId() : null;
        log.info("Guardando usuario con email={} y rolId={}",
                usuario != null ? usuario.getEmail() : "null", rolId);

        assert usuario != null;
        UsuarioEntity entity = mapper.map(usuario, UsuarioEntity.class);
        entity.setRolId(rolId);

        return repository.save(entity)
                .doOnNext(saved -> log.debug("Entidad persistida: {}", saved))
                .map(saved -> {
                    Usuario mapped = mapper.map(saved, Usuario.class);
                    if (saved.getRolId() != null) {
                        mapped.setRol(Rol.builder().uniqueId(saved.getRolId()).build());
                    }
                    return mapped;
                })
                .doOnNext(u -> log.info("Usuario guardado con id={}", u.getIdUsuario()))
                .doOnError(e -> log.error("Error al guardar usuario: {}", e.getMessage(), e));
    }

    @Override
    public Mono<Usuario> getUsuarioByEmail(String email) {
        log.info("Buscando usuario por email={}", email);

        UsuarioEntity probe = new UsuarioEntity();
        probe.setEmail(email);

        return findByExampleEntity(probe) // ✅ usamos entity como probe
                .next()
                .map(entity -> {
                    Usuario mapped = mapper.map(entity, Usuario.class);
                    if (entity.getRolId() != null) {
                        mapped.setRol(Rol.builder().uniqueId(entity.getRolId()).build());
                    }
                    return mapped;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No se encontró usuario con email={}", email);
                    return Mono.empty();
                }))
                .doOnError(e -> log.error("Error al buscar usuario por email={}: {}", email, e.getMessage(), e));
    }

    @Override
    public Mono<Usuario> getUsuarioByEmailAndDocument(String email, String document) {
        log.info("Buscando usuario por email={} y documento={}", email, document);

        UsuarioEntity probe = new UsuarioEntity();
        probe.setEmail(email);
        probe.setDocumentoIdentidad(document);

        return findByExampleEntity(probe) // ✅ usamos entity como probe
                .next()
                .map(entity -> {
                    Usuario mapped = mapper.map(entity, Usuario.class);
                    if (entity.getRolId() != null) {
                        mapped.setRol(Rol.builder().uniqueId(entity.getRolId()).build());
                    }
                    return mapped;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No se encontró usuario con email={} y documento={}", email, document);
                    return Mono.empty();
                }))
                .doOnError(e -> log.error("Error al buscar usuario por email={} y documento={}: {}", email, document, e.getMessage(), e));
    }
}
