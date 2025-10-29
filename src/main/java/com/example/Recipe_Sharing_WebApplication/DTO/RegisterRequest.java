package com.example.Recipe_Sharing_WebApplication.DTO;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class RegisterRequest {
    String username;
    String password;
}
