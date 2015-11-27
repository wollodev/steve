package de.rwth.idsg.steve.stanApi.rest;

import de.rwth.idsg.steve.repository.ChargePointRepository;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.stanApi.dto.ChargePointDTO;
import de.rwth.idsg.steve.stanApi.repository.StanChargePointRepository;
import de.rwth.idsg.steve.stanApi.service.ChargePointService;
import de.rwth.idsg.steve.web.dto.ChargeBoxForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Wolfgang Kluth <kluth@dbis.rwth-aachen.de>
 * @since 27.10.2015
 */

@RestController
@RequestMapping("/stan-api/chargepoints")
public class StanChargePointsController {

    @Autowired private ChargePointRepository chargePointRepository;
    @Autowired private StanChargePointRepository stanChargePointRepository;
    @Autowired private ChargePointService chargePointService;

    // -------------------------------------------------------------------------
    // Paths
    // -------------------------------------------------------------------------

    private static final String DETAILS_PATH = "/{chargeBoxId}";
    private static final String ADD_PATH = "/add";
    private static final String DELETE_PATH = "/{chargeBoxId}/delete";

    private static final String UNLOCK_CONNECTOR = "/{chargeBoxId}/unlockConnector}";

    private static final String START_TRANSACTION = "/{chargeBoxId}/startTransaction}";
    private static final String STOP_TRANSACTION = "/{chargeBoxId}/stopTransaction}";

    // -------------------------------------------------------------------------
    // HTTP methods
    // -------------------------------------------------------------------------

    @RequestMapping(value = DETAILS_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChargePoint getDetails(@PathVariable("chargeBoxId") String chargeBoxId, HttpServletRequest request) {

        return chargePointRepository.getDetails(chargeBoxId);
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ChargePointDTO> getChargePoints() {

        return stanChargePointRepository.getChargeBoxes();
    }

    @RequestMapping(value = ADD_PATH, method = RequestMethod.POST)
    public void addChargePoint(@RequestBody ChargePointDTO chargePointDTO) {
        ChargeBoxForm chargeBoxForm = new ChargeBoxForm();
        chargeBoxForm.setChargeBoxId(chargePointDTO.getCharBoxId());
        chargePointRepository.addChargePoint(chargeBoxForm);
    }

    @RequestMapping(value = DELETE_PATH, method = RequestMethod.POST)
    public void deleteChargePoint(@PathVariable("chargeBoxId") String chargeBoxId) {
        chargePointRepository.deleteChargePoint(chargeBoxId);
    }

    @RequestMapping(value = UNLOCK_CONNECTOR, method = RequestMethod.POST)
    public void unlockSlot(@PathVariable("chargeBoxId") String chargeBoxId,
                           @RequestParam("connector") int connectorId) {
        chargePointService.unlockConnector(chargeBoxId, connectorId);
    }

    @RequestMapping(value = START_TRANSACTION, method = RequestMethod.POST)
    public void startTransaction(@PathVariable("chargeBoxId") String chargeBoxId,
                                 @RequestParam("connector") int connectorId,
                                 @RequestParam("user") String userIdTag) {
        chargePointService.startTransaction(chargeBoxId, connectorId, userIdTag);
    }

    @RequestMapping(value = STOP_TRANSACTION, method = RequestMethod.POST)
    public void stopTransaction(@PathVariable("chargeBoxId") String chargeBoxId,
                                 @RequestParam("transaction") int transactionId) {
        chargePointService.stopTransaction(chargeBoxId, transactionId);
    }

}
