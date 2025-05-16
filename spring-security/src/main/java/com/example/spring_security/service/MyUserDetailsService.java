package com.example.spring_security.service;

import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepo userRepo;
    
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepo.findUserByUsername(usernameOrEmail);
        
        if (user == null) {
            user = userRepo.findByEmail(usernameOrEmail);
        }
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
        }
        
        return new UserPrincipal(user);
    }
}
