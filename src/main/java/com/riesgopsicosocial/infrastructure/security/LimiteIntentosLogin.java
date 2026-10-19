package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.shared.exception.DemasiadosIntentosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno a la fuerza bruta en el inicio de sesión: cuenta los fallos por usuario y por IP y, al llegar
 * al máximo, bloquea durante {@code bloqueo} desde el último fallo. Los fallos se olvidan si pasa ese
 * mismo tiempo sin otro. En memoria: se pierde al reiniciar y vale para una sola instancia.
 */
@Component
public class LimiteIntentosLogin {

    /** Tamaño desde el que se borran las entradas vencidas para que el mapa no crezca sin límite. */
    static final int LIMPIAR_DESDE = 10_000;

    private final int maxIntentosUsuario;
    private final int maxIntentosIp;
    private final Duration bloqueo;
    private final Clock reloj;
    private final Map<String, Fallos> fallos = new ConcurrentHashMap<>();

    @Autowired
    public LimiteIntentosLogin(@Value("${app.seguridad.login.max-intentos-usuario:5}") int maxIntentosUsuario,
                               @Value("${app.seguridad.login.max-intentos-ip:20}") int maxIntentosIp,
                               @Value("${app.seguridad.login.bloqueo:15m}") Duration bloqueo) {
        this(maxIntentosUsuario, maxIntentosIp, bloqueo, Clock.systemUTC());
    }

    LimiteIntentosLogin(int maxIntentosUsuario, int maxIntentosIp, Duration bloqueo, Clock reloj) {
        this.maxIntentosUsuario = maxIntentosUsuario;
        this.maxIntentosIp = maxIntentosIp;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    /** Lanza {@link DemasiadosIntentosException} (429) si el usuario o la IP están bloqueados. */
    public void verificar(String usuario, String ip) {
        Instant ahora = reloj.instant();
        Duration porUsuario = espera(claveUsuario(usuario), maxIntentosUsuario, ahora);
        Duration porIp = espera(claveIp(ip), maxIntentosIp, ahora);
        Duration espera = porUsuario.compareTo(porIp) >= 0 ? porUsuario : porIp;
        if (!espera.isZero()) {
            throw new DemasiadosIntentosException(espera);
        }
    }

    public void registrarFallo(String usuario, String ip) {
        Instant ahora = reloj.instant();
        sumarFallo(claveUsuario(usuario), ahora);
        sumarFallo(claveIp(ip), ahora);
        if (fallos.size() > LIMPIAR_DESDE) {
            fallos.values().removeIf(f -> vencido(f, ahora));
        }
    }

    /** Solo se olvida el usuario: con una cuenta válida no se puede reiniciar el contador de la IP. */
    public void registrarExito(String usuario) {
        fallos.remove(claveUsuario(usuario));
    }

    private void sumarFallo(String clave, Instant ahora) {
        fallos.compute(clave, (k, f) -> f == null || vencido(f, ahora)
                ? new Fallos(1, ahora) : new Fallos(f.cantidad() + 1, ahora));
    }

    private Duration espera(String clave, int maximo, Instant ahora) {
        Fallos f = fallos.get(clave);
        if (f == null || f.cantidad() < maximo || vencido(f, ahora)) {
            return Duration.ZERO;
        }
        return Duration.between(ahora, f.ultimo().plus(bloqueo));
    }

    private boolean vencido(Fallos f, Instant ahora) {
        return !f.ultimo().plus(bloqueo).isAfter(ahora);
    }

    private static String claveUsuario(String usuario) {
        return "u:" + usuario.trim().toLowerCase(Locale.ROOT);
    }

    private static String claveIp(String ip) {
        return "ip:" + ip;
    }

    private record Fallos(int cantidad, Instant ultimo) {
    }

}
