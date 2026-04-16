package com.eduardo.accounts_service.controller;

import com.eduardo.accounts_service.dto.AccountDto;
import com.eduardo.accounts_service.dto.CustomerDto;
import com.eduardo.accounts_service.dto.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountsController {

    @PostMapping
    public ResponseEntity<ResponseDto> create (@RequestBody CustomerDto customerDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto("201", "Account created successfully"));
    }


}
