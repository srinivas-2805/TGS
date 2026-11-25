package com.tgs.school.auth.security;

import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(usernameOrEmail)
                .or(() -> userRepository.findByEmailAndDeletedAtIsNull(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return new CustomUserDetails(user);
    }
}
