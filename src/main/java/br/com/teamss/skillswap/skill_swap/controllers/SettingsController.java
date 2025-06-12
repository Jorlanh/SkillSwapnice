package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private UserServiceDTO userServiceDTO;

    @GetMapping
    public String showSettings(Model model) {
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = userDetails.getUsername();
            UserDTO user = userServiceDTO.findByUsernameDTO(username);
            model.addAttribute("username", user.getUsername());
            
            // O texto agora é fixo no idioma original (Português).
            // O Widget do Google fará a tradução no navegador do usuário.
            model.addAttribute("welcomeMessage", "Bem-vindo ao SkillSwap! Configure suas preferências.");
            model.addAttribute("settingsTitle", "Configurações");
            model.addAttribute("profileLink", "Perfil");
            model.addAttribute("accountLink", "Configurações da Conta");
            model.addAttribute("privacyLink", "Privacidade da Conta");
            model.addAttribute("notificationsLink", "Notificações");
            model.addAttribute("activityLink", "Centro de Atividade");
            model.addAttribute("accessibilityLink", "Acessibilidade");
            model.addAttribute("supportLink", "Apoio & Sobre");
            model.addAttribute("reportLink", "Relatar um Problema");
            model.addAttribute("termsLink", "Termos e Políticas");
            model.addAttribute("sessionLink", "Início de Sessão");
            model.addAttribute("switchAccountLink", "Mudar de Conta");
            model.addAttribute("logoutLink", "Sair da Conta");

            return "configuracoes";
        } catch (Exception e) {
            return "redirect:/api/login";
        }
    }

    // Manter os demais métodos de navegação...
    @GetMapping("/profile")
    public String showProfile() { return "redirect:/api/settings"; }

    @GetMapping("/account")
    public String showAccount() { return "redirect:/api/settings"; }

    @GetMapping("/privacy")
    public String showPrivacy() { return "redirect:/api/settings"; }
    
    @GetMapping("/contact")
    public String showContact() { return "redirect:/api/settings"; }

    @GetMapping("/notifications")
    public String showNotifications() { return "redirect:/api/settings"; }
    
    @GetMapping("/activity")
    public String showActivity() { return "redirect:/api/settings"; }
    
    @GetMapping("/accessibility")
    public String showAccessibility() { return "redirect:/api/settings"; }
    
    @GetMapping("/support")
    public String showSupport() { return "redirect:/api/settings"; }
    
    @GetMapping("/report")
    public String showReport() { return "redirect:/api/settings"; }
    
    @GetMapping("/terms")
    public String showTerms() { return "redirect:/api/settings"; }
    
    @GetMapping("/session")
    public String showSession() { return "redirect:/api/settings"; }
    
    @GetMapping("/switch-account")
    public String switchAccount() { return "redirect:/api/settings"; }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return "redirect:/api/login";
    }
}