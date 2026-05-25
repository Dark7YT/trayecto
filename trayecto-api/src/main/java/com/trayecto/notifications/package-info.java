@org.springframework.modulith.ApplicationModule(
    displayName = "Notifications",
    allowedDependencies = {"shared", "iam::api", "trips::api", "sharing::api"}
)
package com.trayecto.notifications;
