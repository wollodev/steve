package de.rwth.idsg.steve.stanApi.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created by Wolfgang Kluth on 11/11/15.
 */

@Getter
@Builder
public class UserDTO {
    private String idTag;
    private String note;
    private List<TransactionDTO> transactions;
}
