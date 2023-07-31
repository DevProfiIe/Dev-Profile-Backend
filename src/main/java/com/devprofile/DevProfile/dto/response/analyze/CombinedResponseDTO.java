package com.devprofile.DevProfile.dto.response.analyze;

import com.devprofile.DevProfile.dto.response.ApiResponse;
import lombok.Data;

import java.util.List;

@Data
public class CombinedResponseDTO {

    private ApiResponse<UserDTO> userDTO;
    private ApiResponse<List<RepositoryEntityDTO>> apiResponse;


}
