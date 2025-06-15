package organizationmanagement.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "survey-service", url = "${survey-service.url}")
public interface SurveyServiceClient {

    @GetMapping("/api/surveys/{surveyId}/exists")
    ResponseEntity<Boolean> surveyExists(@PathVariable("surveyId") UUID surveyId);
}