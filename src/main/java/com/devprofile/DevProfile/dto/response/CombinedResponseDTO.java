package com.devprofile.DevProfile.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class CombinedResponseDTO {

    private ApiResponse<UserDTO> userDTO;
    private ApiResponse<List<RepositoryEntityDTO>> apiResponse;


}
