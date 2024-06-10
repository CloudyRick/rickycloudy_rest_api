package dev.rickcloudy.restapi.dto;

import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Getter
@Setter
@AllArgsConstructor
@ToString
public class ResponseDTO<T> {
	private boolean success;
	private T data;
	private String message;
	
	public static <T> ResponseDTO<T> success(T data, String message) {
		return new ResponseDTO<T>(true, data, message);
	}
	public static <T> ResponseDTO<T> fail(T data, String message) {
		return new ResponseDTO<T>(false, data, message);
	}
}
