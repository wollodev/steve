package de.rwth.idsg.steve.stanApi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by Wolfgang Kluth on 11/11/15.
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String idTag;
    private String note;
    private List<TransactionDTO> transactions;

}
