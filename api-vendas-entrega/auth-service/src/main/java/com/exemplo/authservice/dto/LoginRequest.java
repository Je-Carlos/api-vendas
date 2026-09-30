package com.exemplo.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class LoginRequest {
    
    @NotBlank @Email @Size(max = 255) private String email;
    @NotBlank @Size(max = 72) private String senha;

}
