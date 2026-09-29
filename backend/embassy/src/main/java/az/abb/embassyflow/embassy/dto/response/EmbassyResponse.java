package az.abb.embassyflow.embassy.dto.response;

public record EmbassyResponse(
        Long id,
        String name,
        String country,
        String city,
        String language) {
}