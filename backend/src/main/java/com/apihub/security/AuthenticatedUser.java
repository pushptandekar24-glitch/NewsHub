package com.apihub.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Spring's default UserDetails has no id field, but almost every service needs
 * the user id. Subclassing avoids an extra DB lookup on every request.
 */
public class AuthenticatedUser extends User {

    private final Long id;

    public AuthenticatedUser(Long id, String email, String passwordHash,
                             Collection<? extends GrantedAuthority> authorities) {
        super(email, passwordHash, authorities);
        this.id = id;
    }

    public Long getId() { return id; }
}
