package rpa.backend.main.service;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rpa.backend.main.entity.Auth;
import rpa.backend.main.entity.Login;
import rpa.backend.main.entity.User;
import rpa.backend.main.exception.UniqueConstraintViolationException;
import rpa.backend.main.repository.LoginRepository;
import rpa.backend.main.repository.UserRepository;

import javax.mail.MessagingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

@Service
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final LoginRepository loginRepository;
    private final MailService mailService;
    private final SecureRandom secureRandom;

    @Autowired
    public RegistrationServiceImpl(UserRepository userRepository, LoginRepository loginRepository, MailService mailService, SecureRandom secureRandom) {
        this.userRepository = userRepository;
        this.loginRepository = loginRepository;
        this.mailService = mailService;
        this.secureRandom = secureRandom;
    }

    @Override
    //회원가입처리
    public boolean register(String name, String upw, String email, String phone) throws NoSuchAlgorithmException, MessagingException, UniqueConstraintViolationException {
        Optional<User> userOptional = this.userRepository.findByEmail(email);
        this.secureRandom.setSeed(System.currentTimeMillis());
        if (userOptional.isPresent()) throw new UniqueConstraintViolationException();
        User user = this.userRepository.save(User.builder()
                .email(email)
                .name(name)
                .phoneNum(phone)
                .authenticated(false)
                .authenticationValue(SHA256Algorithm.getHashedValue(String.valueOf(this.secureRandom.nextDouble())))
                .auth(Auth.NORMAL)
                .build());
        this.loginRepository.save(Login.builder()
                .password(SHA256Algorithm.getHashedValue(upw))
                .user(user)
                .build());
        this.mailService.sendMail(email, "http://localhost:3000/user/auth?id="+user.getId()+"&value="+user.getAuthenticationValue());
        return true;
    }


    @Override
    //이메일 인증 처리
    public boolean authenticate(int id, String authenticationValue) {
        Optional<User> userOptional = this.userRepository.findById(id);
        if (userOptional.isEmpty()) return false;
        User user = userOptional.get();
        user.setAuthenticated(user.getAuthenticationValue().equals(authenticationValue));
        this.userRepository.save(user);
        return user.isAuthenticated();
    }
}
