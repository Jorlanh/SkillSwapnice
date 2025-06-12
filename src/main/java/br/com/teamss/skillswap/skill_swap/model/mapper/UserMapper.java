package br.com.teamss.skillswap.skill_swap.model.mapper;

import br.com.teamss.skillswap.skill_swap.dto.RegisterDTO;
import br.com.teamss.skillswap.skill_swap.dto.VerifyDTO;
import br.com.teamss.skillswap.skill_swap.dto.ErrorResponse;
import br.com.teamss.skillswap.skill_swap.dto.SuccessResponse;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", imports = { java.sql.Date.class, java.time.LocalDate.class })
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    // Map RegisterDTO to User
    @Mapping(target = "birthDate", expression = "java(registerDTO.getBirthDate() != null ? Date.valueOf(registerDTO.getBirthDate()) : null)")
    @Mapping(target = "password", ignore = true) // Password is encoded separately
    @Mapping(target = "verificationCode", ignore = true) // Set manually
    @Mapping(target = "verified", ignore = true) // Set manually
    @Mapping(target = "verificationCodeExpiry", ignore = true) // Set manually
    @Mapping(target = "createdAt", ignore = true) // Set manually
    @Mapping(target = "updatedAt", ignore = true) // Set manually
    @Mapping(target = "verifiedAt", ignore = true) // Set manually
    User registerDTOToUser(RegisterDTO registerDTO);

    // Map User to SuccessResponse
    @Mapping(target = "message", source = "message")
    SuccessResponse toSuccessResponse(String message);

    // Map String to ErrorResponse
    @Mapping(target = "message", source = "errorMessage")
    ErrorResponse toErrorResponse(String errorMessage);
}