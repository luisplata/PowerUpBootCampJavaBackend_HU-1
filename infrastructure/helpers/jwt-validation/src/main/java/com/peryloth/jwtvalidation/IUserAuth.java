package com.peryloth.jwtvalidation;

import com.peryloth.model.rol.Rol;

public interface IUserAuth {
    boolean canUserDoing(Rol rol);

    void loginExitoso();

    void registrarIntentoFallido(int maxIntentos);

    boolean puedeIntentarLogin();
}
