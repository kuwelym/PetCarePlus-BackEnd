package petitus.petcareplus.security.jwt;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import petitus.petcareplus.model.User;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@ToString
public final class JwtUserDetails implements UserDetails {
    private final UUID id;

    private final String username;

    private final String password;

    private final String email;

    private Collection<? extends GrantedAuthority> authorities;

    public JwtUserDetails(UUID id, String username, String password, String email, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.authorities = authorities;
    }

    public static UserDetails build(User user) {
        List<GrantedAuthority> authorities = List.of((GrantedAuthority) () -> user.getRole().getName().getValue());

        return new JwtUserDetails(user.getId(), user.getEmail(), user.getPassword(), user.getEmail(), authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
