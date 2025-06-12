package br.com.teamss.skillswap.skill_swap.model.mapper;

import br.com.teamss.skillswap.skill_swap.dto.RegisterDTO;
// A importação de VerifyDTO foi removida daqui
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
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "verificationCode", ignore = true)
    @Mapping(target = "verified", ignore = true)
    @Mapping(target = "verificationCodeExpiry", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    // A linha para "updatedAt" foi removida daqui
    @Mapping(target = "verifiedAt", ignore = true)
    User registerDTOToUser(RegisterDTO registerDTO);

    // Map User to SuccessResponse
    @Mapping(target = "message", source = "message")
    SuccessResponse toSuccessResponse(String message);

    // Map String to ErrorResponse
    @Mapping(target = "message", source = "errorMessage")
    ErrorResponse toErrorResponse(String errorMessage);
}