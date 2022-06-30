package com.gurudev.aircnc.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.gurudev.aircnc.exception.AircncRuntimeException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class CustomControllerAdvice {

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(IllegalArgumentException.class)
  public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
    return ErrorResponse.of(BAD_REQUEST, ex.getMessage());
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({AircncRuntimeException.class,
      TypeMismatchException.class, HttpMessageNotReadableException.class,
      MissingServletRequestParameterException.class, NoHandlerFoundException.class})
  public ErrorResponse handleBadRequestException(RuntimeException ex) {
    log.debug(ex.getMessage(), ex);

    return ErrorResponse.of(BAD_REQUEST, "잘못된 요청입니다");
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(RuntimeException.class)
  public ErrorResponse handleServerException(RuntimeException ex) {
    log.error(ex.getMessage(), ex);

    // TODO: 2022/06/25 이메일 혹은 슬랙으로 에러메시지 보내기

    return ErrorResponse.of(INTERNAL_SERVER_ERROR, "알 수 없는 에러가 발생했습니다. 고객센터에 문의하세요.");
  }

  @Getter
  @AllArgsConstructor
  private static class ErrorResponse {

    private int code;
    private String message;

    static ErrorResponse of(HttpStatus status, String message) {
      return new ErrorResponse(status.value(), message);
    }
  }
}
