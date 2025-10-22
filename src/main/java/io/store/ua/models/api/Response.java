package io.store.ua.models.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.store.ua.exceptions.ApplicationException;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@JsonSerialize(using = Response.ResponseSerializer.class)
@FieldNameConstants
public class Response {
    private static Map<Long, Response> instance = new ConcurrentHashMap<>();
    private ApplicationException exception;
    private Object data;
    private HttpStatus httpStatus;

    private Response() {
        this.httpStatus = HttpStatus.OK;
    }

    private Response(Object data) {
        this.httpStatus = HttpStatus.OK;
        this.data = data;
    }

    public static Response ok() {
        return new Response();
    }

    public static Response ok(Object data) {
        return new Response(data);
    }

    public static Response of(HttpStatus httpStatus) {
        var response =
                instance.computeIfAbsent(Thread.currentThread().threadId(), (ignored) -> new Response());
        response.setHttpStatus(httpStatus);

        return response;
    }

    public static Response data(Object data) {
        var response =
                instance.computeIfAbsent(Thread.currentThread().threadId(), (ignored) -> new Response());
        response.setData(data);

        return response;
    }

    public Response build() {
        Response response = instance.get(Thread.currentThread().threadId());
        instance.remove(Thread.currentThread().threadId());

        return response;
    }

    public Response exception(ApplicationException exception) {
        var response =
                instance.computeIfAbsent(Thread.currentThread().threadId(), (ignored) -> new Response());
        response.setException(exception);

        return response;
    }

    static class ResponseSerializer extends JsonSerializer<Response> {
        @Override
        public void serialize(Response value, JsonGenerator generator, SerializerProvider serializers)
                throws IOException {
            generator.writeStartObject();

            if (value.getHttpStatus().isError()) {
                generator.writeFieldName(Response.Fields.exception);
                generator.writeObject(value.getException());
            }

            if (value.getData() != null) {
                generator.writeFieldName(Response.Fields.data);
                generator.writeObject(value.getData());
            }

            generator.writeFieldName(Response.Fields.httpStatus);
            generator.writeObject(value.getHttpStatus());

            generator.writeEndObject();
        }
    }
}
