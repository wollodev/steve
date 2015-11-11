package de.rwth.idsg.steve.stanApi.dto;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.List;

/**
 * A DTO for the ChargePoint entity.
 */

@Data
@Builder
public class ChargePointDTO {

    @NotNull
    private String charBoxId;

    private String description;

    private String status;

    private ConnectionStatus connectionStatus;

    private String errorCode;

    private List<ConnectorDTO> connectors;

}
