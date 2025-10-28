package br.com.teamss.skillswap.skill_swap.model.config;

import br.com.teamss.skillswap.skill_swap.filters.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    // --- ESTES BEANS DEVEM PERMANECER AQUI ---
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        // Define o idioma padrão, caso não seja possível detectar o do navegador
        slr.setDefaultLocale(new Locale("pt", "BR"));
        return slr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        // O parâmetro 'lang' na URL mudará o idioma (ex: /api/settings?lang=en)
        lci.setParamName("lang");
        return lci;
    }
    // --- FIM DOS BEANS i18n ---

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // --- REGISTRO DOS INTERCEPTORS ---
        registry.addInterceptor(localeChangeInterceptor()); // Garanta que este esteja aqui
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/login", "/api/password-reset/**");
        // --- FIM DO REGISTRO ---
    }
}
