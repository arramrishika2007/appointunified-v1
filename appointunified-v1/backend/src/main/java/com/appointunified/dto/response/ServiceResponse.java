package com.appointunified.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponse {

    public static class Summary {
        private UUID id;
        private String name;
        private String description;
        private Integer durationMinutes;
        private BigDecimal price;
        private Boolean isActive;
        private Boolean requiresDocuments;
        private Boolean isVirtual;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean active) { isActive = active; }
        public Boolean getRequiresDocuments() { return requiresDocuments; }
        public void setRequiresDocuments(Boolean requiresDocuments) { this.requiresDocuments = requiresDocuments; }
        public Boolean getIsVirtual() { return isVirtual; }
        public void setIsVirtual(Boolean virtual) { isVirtual = virtual; }
    }
}
