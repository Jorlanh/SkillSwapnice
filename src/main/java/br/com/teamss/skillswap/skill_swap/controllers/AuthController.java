package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.SuccessResponse;
import br.com.teamss.skillswap.skill_swap.model.config.JwtTokenUtil;
import br.com.teamss.skillswap.skill_swap.model.services.JwtBlocklistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Date;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private JwtBlocklistService jwtBlocklistService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        final String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            final String token = header.substring(7);
            
            Date expirationDate = jwtTokenUtil.getExpirationDateFromToken(token);
            long remainingMillis = expirationDate.getTime() - System.currentTimeMillis();

            if (remainingMillis > 0) {
                jwtBlocklistService.blockToken(token, Duration.ofMillis(remainingMillis));
            }
        }

        return ResponseEntity.ok(new SuccessResponse("Logout realizado com sucesso."));
    }
}