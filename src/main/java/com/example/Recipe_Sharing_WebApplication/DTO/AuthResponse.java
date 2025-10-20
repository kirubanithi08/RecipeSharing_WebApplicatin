package com.example.Recipe_Sharing_WebApplication.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    String accessToken;
    String RefreshToken;


}
