package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.AccessLogDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.AccessLog;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessLogRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AccessLogService;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccessLogServiceImpl implements AccessLogService {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogServiceImpl.class);

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    private DatabaseReader geoIpDatabaseReader;

    // Carrega o banco de dados GeoIP ao iniciar o serviço
    @Autowired
    public AccessLogServiceImpl(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        try {
            Resource resource = resourceLoader.getResource("classpath:geoip/GeoLite2-City.mmdb");
            this.geoIpDatabaseReader = new DatabaseReader.Builder(resource.getInputStream()).build();
            logger.info("GeoIP database loaded successfully from classpath:geoip/GeoLite2-City.mmdb");
        } catch (IOException e) {
            logger.error("Failed to load GeoIP database: " + e.getMessage(), e);
            this.geoIpDatabaseReader = null; // Define como null para indicar que GeoIP não está disponível
        }
    }

    @Override
    public void logAccess(UUID userId, HttpServletRequest request) {
        // Obtém o IP do cliente
        String ipAddress = request.getRemoteAddr();

        // Determina a localização usando MaxMind GeoIP
        LocationInfo locationInfo = determineLocation(ipAddress);

        // Cria e salva o registro de acesso
        AccessLog accessLog = new AccessLog(
            userId,
            ipAddress,
            locationInfo.getFormattedLocation(), // "Cidade, Estado, País"
            locationInfo.getCity(),
            locationInfo.getSubdivision(),
            locationInfo.getCountry()
        );
        accessLogRepository.save(accessLog);
    }

    @Override
    public List<AccessLogDTO> getAccessHistory(UUID userId) {
        List<AccessLog> accessLogs = accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId);
        return accessLogs.stream()
                .map(log -> new AccessLogDTO(
                    log.getAccessTime(),
                    log.getIpAddress(),
                    log.getLocation(),
                    log.getCity(),
                    log.getSubdivision(),
                    log.getCountry()
                ))
                .collect(Collectors.toList());
    }

    // Método auxiliar para determinar a localização usando MaxMind GeoIP
    private LocationInfo determineLocation(String ipAddress) {
        // Verifica se o GeoIP está disponível
        if (geoIpDatabaseReader == null) {
            logger.warn("GeoIP database not available. Returning default location for IP: " + ipAddress);
            return new LocationInfo("GeoIP Não Disponível", null, null, null);
        }

        try {
            // Ignora IPs locais para evitar erros no lookup
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                return new LocationInfo("Localhost", null, null, null);
            }

            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            CityResponse response = geoIpDatabaseReader.city(inetAddress);

            String city = response.getCity().getName();
            String subdivision = response.getMostSpecificSubdivision().getIsoCode();
            String country = response.getCountry().getName();

            // Formata a localização como "Cidade, Estado, País"
            StringBuilder formattedLocation = new StringBuilder();
            if (city != null && !city.isEmpty()) {
                formattedLocation.append(city);
            }
            if (subdivision != null && !subdivision.isEmpty()) {
                if (formattedLocation.length() > 0) {
                    formattedLocation.append(", ");
                }
                formattedLocation.append(subdivision);
            }
            if (country != null && !country.isEmpty()) {
                if (formattedLocation.length() > 0) {
                    formattedLocation.append(", ");
                }
                formattedLocation.append(country);
            }

            return new LocationInfo(
                formattedLocation.length() > 0 ? formattedLocation.toString() : "Localização Desconhecida",
                city,
                subdivision,
                country
            );
        } catch (IOException | GeoIp2Exception e) {
            logger.error("Error determining location for IP " + ipAddress + ": " + e.getMessage(), e);
            return new LocationInfo("Localização Desconhecida (IP: " + ipAddress + ")", null, null, null);
        }
    }

    // Classe interna para encapsular as informações de localização
    private static class LocationInfo {
        private final String formattedLocation;
        private final String city;
        private final String subdivision;
        private final String country;

        public LocationInfo(String formattedLocation, String city, String subdivision, String country) {
            this.formattedLocation = formattedLocation;
            this.city = city;
            this.subdivision = subdivision;
            this.country = country;
        }

        public String getFormattedLocation() { return formattedLocation; }
        public String getCity() { return city; }
        public String getSubdivision() { return subdivision; }
        public String getCountry() { return country; }
    }
}