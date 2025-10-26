package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

/**
 * Controlador para gerenciar as configurações do usuário, incluindo perfil, conta,
 * privacidade e internacionalização.
 */
@Controller
@RequestMapping("/api/settings")
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @Autowired
    private UserServiceDTO userServiceDTO;

    @Autowired
    private MessageSource messageSource;

    /**
     * Método auxiliar para obter o usuário autenticado e adicioná-lo ao Model.
     * Isso evita a repetição de código e torna os dados do usuário disponíveis para as views.
     * @param model O objeto Model para adicionar o atributo do usuário.
     */
    private void addAuthenticatedUserToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            UserDTO user = userServiceDTO.findByUsernameDTO(username);
            model.addAttribute("user", user); // Adiciona o objeto UserDTO completo.
        }
    }

    /**
     * Exibe a página principal de configurações, que é a página de edição de perfil.
     * @param model O Model para a view.
     * @param locale O locale do usuário, detectado automaticamente para i18n.
     * @return O caminho para a view de perfil.
     */
    @GetMapping
    public String showProfilePage(Model model, Locale locale) {
        try {
            addAuthenticatedUserToModel(model);
            model.addAttribute("pageTitle", messageSource.getMessage("settings.profileLink", null, locale));
            return "settings/profile"; // Retorna a view específica de perfil
        } catch (Exception e) {
            logger.error("Erro ao carregar a página de perfil do usuário.", e);
            return "redirect:/api/login?error";
        }
    }

    /**
     * Processa a submissão do formulário para atualizar os dados do perfil do usuário.
     * @param userDTO Os dados do usuário vindos do formulário.
     * @param redirectAttributes Usado para enviar mensagens de feedback (sucesso/erro) após o redirect.
     * @param locale O locale do usuário para traduzir as mensagens de feedback.
     * @return Um redirect para a página de perfil.
     */
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") UserDTO userDTO, RedirectAttributes redirectAttributes, Locale locale) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            // A lógica para atualizar o usuário deve estar no seu service.
            // Supondo que você tenha um método para isso no seu DTO de serviço.
            // userServiceDTO.updateUser(currentUsername, userDTO);
            
            String successMessage = messageSource.getMessage("profile.update.success", null, locale);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            logger.info("Perfil do usuário '{}' atualizado com sucesso.", currentUsername);

        } catch (Exception e) {
            logger.error("Erro ao tentar atualizar o perfil do usuário '{}'.", userDTO.getUsername(), e);
            String errorMessage = messageSource.getMessage("profile.update.error", null, locale);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        }
        return "redirect:/api/settings"; // Padrão Post-Redirect-Get
    }

    /**
     * Exibe a página de configurações da conta.
     */
    @GetMapping("/account")
    public String showAccountPage(Model model, Locale locale) {
        addAuthenticatedUserToModel(model);
        model.addAttribute("pageTitle", messageSource.getMessage("settings.accountLink", null, locale));
        return "settings/account";
    }

    /**
     * Exibe a página de configurações de privacidade.
     */
    @GetMapping("/privacy")
    public String showPrivacyPage(Model model, Locale locale) {
        addAuthenticatedUserToModel(model);
        model.addAttribute("pageTitle", messageSource.getMessage("settings.privacyLink", null, locale));
        return "settings/privacy";
    }

    /**
     * Exibe a página de notificações.
     */
    @GetMapping("/notifications")
    public String showNotificationsPage(Model model, Locale locale) {
        addAuthenticatedUserToModel(model);
        model.addAttribute("pageTitle", messageSource.getMessage("settings.notificationsLink", null, locale));
        return "settings/notifications";
    }
    
    /**
     * Exibe a página de centro de atividade.
     */
    @GetMapping("/activity")
    public String showActivity(Model model, Locale locale) {
        addAuthenticatedUserToModel(model);
        model.addAttribute("pageTitle", messageSource.getMessage("settings.activityLink", null, locale));
        return "settings/activity";
    }
    
    /**
     * Exibe a página de acessibilidade.
     */
    @GetMapping("/accessibility")
    public String showAccessibility(Model model, Locale locale) {
        addAuthenticatedUserToModel(model);
        model.addAttribute("pageTitle", messageSource.getMessage("settings.accessibilityLink", null, locale));
        return "settings/accessibility";
    }
    
    /**
     * Exibe a página de Apoio & Sobre.
     */
    @GetMapping("/support")
    public String showSupport(Model model, Locale locale) {
        addAuthenticatedUserToModel(model);
        model.addAttribute("pageTitle", messageSource.getMessage("settings.supportLink", null, locale));
        return "settings/support";
    }
    
    /**
     * Exibe a página para relatar um problema.
     */
    @GetMapping("/report")
    public String showReport(Model model, Locale locale) {
        addAuthenticatedUserToModel(model);
        model.addAttribute("pageTitle", messageSource.getMessage("settings.reportLink", null, locale));
        return "settings/report";
    }
    
    /**
     * Exibe a página de termos e políticas.
     */
    @GetMapping("/terms")
    public String showTerms(Model model, Locale locale) {
        addAuthenticatedUserToModel(model);
        model.addAttribute("pageTitle", messageSource.getMessage("settings.termsLink", null, locale));
        return "settings/terms";
    }

    /**
     * Realiza o logout do usuário, invalidando a sessão.
     * @param request A requisição HTTP para invalidar a sessão.
     * @return Um redirect para a página de login com um parâmetro de confirmação.
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        logger.info("Usuário '{}' deslogado com sucesso.", username);
        return "redirect:/api/login?logout";
    }
}