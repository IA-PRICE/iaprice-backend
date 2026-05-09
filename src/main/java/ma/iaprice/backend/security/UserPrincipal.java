package ma.iaprice.backend.security;

import lombok.Getter;
import ma.iaprice.backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adaptateur entre notre entité User et Spring Security.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID orgId;
    private final String email;
    private final String password;
    private final String role;

    public UserPrincipal(User user, UUID orgId, String role) {
        this.id       = user.getId();
        this.orgId    = orgId;
        this.email    = user.getEmail();
        this.password = user.getPasswordHash();
        this.role     = role;
    }

    @Override public String getUsername()  { return email; }
    @Override public boolean isEnabled()   { return true; }
    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }
}
