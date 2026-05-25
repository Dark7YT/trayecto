@org.springframework.modulith.ApplicationModule(
    displayName = "Analytics",
    // sharing::api se usa para validar permisos en la descarga de reportes
    // del owner por parte de un grantee autorizado.
    allowedDependencies = {"shared", "iam::api", "trips::api", "sharing::api"}
)
package com.trayecto.analytics;
