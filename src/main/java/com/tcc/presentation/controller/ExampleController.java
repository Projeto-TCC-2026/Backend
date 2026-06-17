package com.tcc.presentation.controller;

import com.tcc.application.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/example")
@Tag(name = "Exemplo", description = "Endpoints de exemplo para demonstrar o Swagger UI")
public class ExampleController {

    @GetMapping
    @Operation(
        summary = "Listar exemplos",
        description = "Retorna uma lista de exemplos para demonstrar a documentação da API"
    )
    public ResponseEntity<ApiResponse<List<String>>> getExamples() {
        List<String> examples = List.of("Exemplo 1", "Exemplo 2", "Exemplo 3");
        return ResponseEntity.ok(ApiResponse.success(examples));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar exemplo por ID",
        description = "Retorna um exemplo específico baseado no ID fornecido"
    )
    public ResponseEntity<ApiResponse<String>> getExampleById(
            @Parameter(description = "ID do exemplo a ser buscado", example = "1", required = true)
            @PathVariable Long id
    ) {
        if (id <= 0 || id > 3) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success("Exemplo " + id));
    }

    @PostMapping
    @Operation(
        summary = "Criar novo exemplo",
        description = "Cria um novo exemplo com os dados fornecidos"
    )
    public ResponseEntity<ApiResponse<String>> createExample(
            @Parameter(description = "Nome do exemplo a ser criado", required = true)
            @RequestParam String name
    ) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Nome não pode ser vazio"));
        }
        return ResponseEntity.ok(ApiResponse.success("Exemplo '" + name + "' criado com sucesso"));
    }
}