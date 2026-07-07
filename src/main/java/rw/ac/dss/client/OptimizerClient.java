package rw.ac.dss.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import rw.ac.dss.dto.optimizer.FeedbackRequestDto;
import rw.ac.dss.dto.optimizer.FeedbackResponseDto;
import rw.ac.dss.dto.optimizer.OptimizationResponseDto;
import rw.ac.dss.dto.optimizer.OptimizeRequestDto;
import rw.ac.dss.exception.OptimizerUnavailableException;

/**
 * HTTP client for the Python FastAPI optimizer service (see optimizer/main.py).
 */
@Slf4j
@Component
public class OptimizerClient {

    private final RestClient restClient;

    public OptimizerClient(RestClient.Builder restClientBuilder,
                            @Value("${app.optimizer.url}") String optimizerBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(optimizerBaseUrl).build();
    }

    public OptimizationResponseDto optimize(OptimizeRequestDto request) {
        try {
            return restClient.post()
                    .uri("/optimize")
                    .body(request)
                    .retrieve()
                    .body(OptimizationResponseDto.class);
        } catch (RestClientException ex) {
            log.error("Optimizer service call failed for delayEventId={}", request.getDelayEventId(), ex);
            throw new OptimizerUnavailableException(
                    "Unable to reach the optimizer service at the configured URL.", ex);
        }
    }

    public FeedbackResponseDto sendFeedback(FeedbackRequestDto request) {
        try {
            return restClient.post()
                    .uri("/feedback")
                    .body(request)
                    .retrieve()
                    .body(FeedbackResponseDto.class);
        } catch (RestClientException ex) {
            log.error("Optimizer feedback call failed for action={}", request.getChosenAction(), ex);
            throw new OptimizerUnavailableException(
                    "Unable to reach the optimizer service to record feedback.", ex);
        }
    }
}
