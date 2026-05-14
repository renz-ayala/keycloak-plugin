package gg.renz.DataTransferObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TurnstileResponse(boolean success) {
}
