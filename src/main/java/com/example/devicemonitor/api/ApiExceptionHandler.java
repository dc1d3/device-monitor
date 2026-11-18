package com.example.devicemonitor.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Приводит все ошибки к {@link ApiError}.
 *
 * <p>Наследуемся от {@link ResponseEntityExceptionHandler}: он уже умеет
 * обрабатывать стандартные ошибки Spring MVC (404, 405, 415, ...), и мы лишь
 * подменяем тело ответа, сохраняя исходный HTTP-код. Иначе перехват
 * {@code Exception} превратил бы 404 в 500.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ApiError error = ApiError.of(statusCode.value(), messageFor(ex, statusCode.value()), pathOf(request));
        return new ResponseEntity<>(error, headers, statusCode);
    }

    /** Всё, что не покрыто стандартными обработчиками Spring. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Необработанная ошибка запроса", ex);
        return ResponseEntity.internalServerError()
                .body(ApiError.of(500, "Внутренняя ошибка сервера", pathOf(request)));
    }

    private String messageFor(Exception ex, int status) {
        return switch (status) {
            case 400 -> "Некорректный запрос";
            case 404 -> "Адрес не найден";
            case 405 -> "Метод не поддерживается для этого адреса";
            case 415 -> "Неподдерживаемый тип содержимого";
            default -> ex.getMessage() == null ? "Ошибка запроса" : ex.getMessage();
        };
    }

    /** {@code request.getDescription(false)} возвращает "uri=/api/device". */
    private String pathOf(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
