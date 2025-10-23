package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.TwoFactorService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/settings/security/two-factor")
public class TwoFactorController {

    @Autowired
    private UserServiceDTO userServiceDTO;

    @Autowired
    private TwoFactorService twoFactorService;

    @GetMapping
    public String showTwoFactorSettings(Model model) {
        UserDTO user = userServiceDTO.findByIdDTO(getCurrentUserId());
        model.addAttribute("username", user.getUsername());
        return "two-factor";
    }

    @GetMapping("/activate/{method}")
    public String activateTwoFactor(@PathVariable String method, Model model) {
        UserDTO user = userServiceDTO.findByIdDTO(getCurrentUserId());
        model.addAttribute("username", user.getUsername());
        UUID userId = user.getUserId();

        String secret = twoFactorService.generateSecret();
        switch (method) {
            case "google":
                try {
                    String qrCodeBase64 = twoFactorService.generateQRCodeImage(user.getUsername(), secret);
                    model.addAttribute("qrCodeUrl", "data:image/png;base64," + qrCodeBase64);
                    model.addAttribute("setupCode", secret);
                    user.setTwoFactorSecret(secret);
                    userServiceDTO.saveUserDTO(user); // Salvar o segredo no banco
                } catch (Exception e) {
                    model.addAttribute("error", "Erro ao gerar QR Code");
                    return "two-factor";
                }
                return "two-factor-google";
            case "sms":
                String smsCode = twoFactorService.generateVerificationCode();
                twoFactorService.sendSMS(user.getPhoneNumber(), smsCode, userId);
                return "two-factor-sms";
            case "email":
                String emailCode = twoFactorService.generateVerificationCode();
                twoFactorService.sendEmail(user.getEmail(), emailCode, userId);
                return "two-factor-email";
            default:
                return "redirect:/settings/security/two-factor";
        }
    }

    @PostMapping("/verify/{method}")
    public String verifyTwoFactor(@PathVariable String method, @RequestParam String code, Model model) {
        UserDTO user = userServiceDTO.findByIdDTO(getCurrentUserId());
        boolean isValid;

        String methodType;
        switch (method) {
            case "google":
                methodType = "GOOGLE_AUTH";
                break;
            case "sms":
                methodType = "SMS";
                break;
            case "email":
                methodType = "EMAIL";
                break;
            default:
                model.addAttribute("error", "Método de autenticação inválido");
                return "redirect:/settings/security/two-factor";
        }

        // Usa a assinatura correta do método verifyCode
        isValid = twoFactorService.verifyCode(user.getUserId(), methodType, code);

        if (isValid) {
            userServiceDTO.updateVerificationStatus(user.getUserId(), true);
            model.addAttribute("success", "Autenticação em duas etapas ativada com sucesso!");
            return "two-factor";
        } else {
            model.addAttribute("error", "Código inválido");
            return "redirect:/settings/security/two-factor/activate/" + method;
        }
    }

    /**
     * Obtém o ID do usuário atualmente autenticado a partir do contexto de segurança do Spring.
     * @return O UUID do usuário logado.
     * @throws IllegalStateException se nenhum usuário estiver autenticado.
     */
    private UUID getCurrentUserId() {
        // Pega o objeto principal da autenticação (que contém os detalhes do usuário)
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            // Converte o principal para UserDetails para obter o username
            String username = ((UserDetails) principal).getUsername();
            // Usa o serviço para encontrar o usuário pelo username e obter seu DTO
            UserDTO user = userServiceDTO.findByUsernameDTO(username);
            // Retorna o UUID do usuário encontrado
            return user.getUserId();
        } else {
            // Se não houver um usuário autenticado, lança uma exceção.
            // Em uma aplicação real, o Spring Security já bloquearia o acesso não autenticado a este endpoint.
            throw new IllegalStateException("Usuário não autenticado.");
        }
    }
}