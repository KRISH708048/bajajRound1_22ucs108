package com.bfhl.webhooktask;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class WebhookTaskApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(WebhookTaskApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
        String jsonBody = """
        {
            "name": "Krish Agarwal",
            "regNo": "22ucs108",
            "email": "22ucs108@lnmiit.ac.in"
        }
        """;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.getBody());
            String webhookUrl = jsonNode.get("webhook").asText();
            String accessToken = jsonNode.get("accessToken").asText();
            System.out.println("Webhook URL: " + webhookUrl);
            System.out.println("Access Token: " + accessToken);

            String sqlQuery = """
            SELECT 
                E1.EMP_ID,
                E1.FIRST_NAME,
                E1.LAST_NAME,
                D.DEPARTMENT_NAME,
                COUNT(E2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
            FROM EMPLOYEE E1
            JOIN DEPARTMENT D ON E1.DEPARTMENT = D.DEPARTMENT_ID
            LEFT JOIN EMPLOYEE E2 
                ON E1.DEPARTMENT = E2.DEPARTMENT 
                AND E2.DOB > E1.DOB
            GROUP BY 
                E1.EMP_ID, E1.FIRST_NAME, E1.LAST_NAME, D.DEPARTMENT_NAME
            ORDER BY E1.EMP_ID DESC
			""";

            HttpHeaders webhookHeaders = new HttpHeaders();
            webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
            webhookHeaders.set("Authorization", accessToken);

            String jsonPayload = """
            {
                "finalQuery": "%s"
            }
            """.formatted(sqlQuery.replace("\"", "\\\"").replace("\n", " "));

            HttpEntity<String> webhookRequest = new HttpEntity<>(jsonPayload, webhookHeaders);
            ResponseEntity<String> webhookResponse = restTemplate.postForEntity(webhookUrl, webhookRequest, String.class);

            System.out.println("submission status: " + webhookResponse.getStatusCode());
            System.out.println("response: " + webhookResponse.getBody());
        } else {
            System.err.println("Failed to get webhook: " + response.getStatusCode());
        }
    }
}
