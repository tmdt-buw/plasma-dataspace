package de.buw.tmdt.plasma.services.dps.rest.controller;

import de.buw.tmdt.plasma.services.dps.api.ErrorDTO;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	@SuppressWarnings("FieldNameHidesFieldInSuperclass - this allows explicit configuration for this logger as part of the de.buw.tmdt name space")
	private static final Logger logger = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

	@ExceptionHandler(value = {ResponseStatusException.class})
	public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
		logger.info("A Response Status Exception occurred {} {}", ex.getStatusCode(), ex.getMessage());
		ErrorDTO errorDTO = new ErrorDTO(ex.getStatusCode().value(), ex.getReason(), ex.getMessage());
		return new ResponseEntity<>(errorDTO, HttpStatus.valueOf(errorDTO.getStatus()));
	}

	@ExceptionHandler(value = {Exception.class})
	public ResponseEntity<Object> handleGenericException(Exception ex) {
		logger.warn("Unhandled Exception", ex);
		ErrorDTO errorDTO = new ErrorDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unknown Exception", ex.getMessage());
		return new ResponseEntity<>(errorDTO, HttpStatus.valueOf(errorDTO.getStatus()));
	}
}