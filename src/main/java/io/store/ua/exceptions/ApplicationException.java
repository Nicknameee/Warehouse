package io.store.ua.exceptions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.store.ua.utility.RegularObjectMapper;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonSerialize(using = ApplicationException.ApplicationExceptionSerializer.class)
@FieldNameConstants
public class ApplicationException extends RuntimeException {
    private HttpStatus status;

    public ApplicationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    static class ApplicationExceptionSerializer extends JsonSerializer<ApplicationException> {
        @Override
        public void serialize(
                ApplicationException exception,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider)
                throws IOException {
            ObjectNode objectNode = RegularObjectMapper.INSTANCE.createObjectNode();

            objectNode.put(ApplicationException.Fields.status, exception.getStatus().name());
            objectNode.put("timestamp", ZonedDateTime.now(Clock.systemUTC()).toString());
            objectNode.put("message", exception.getMessage());

            if (!Objects.isNull(exception.getCause())) {
                objectNode.put("cause", exception.getCause().getClass().getName());
            }

            jsonGenerator.writeTree(objectNode);
        }
    }
}
