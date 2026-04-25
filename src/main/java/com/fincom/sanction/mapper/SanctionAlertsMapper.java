package com.fincom.sanction.mapper;

import com.fincom.sanction.contract.AlertDTO;
import com.fincom.sanction.contract.CreateAlertRequestDTO;
import com.fincom.sanction.contract.EscalateAlertRequestDTO;
import com.fincom.sanction.contract.UpdateAlertDecisionRequestDTO;
import com.fincom.sanction.domain.alert.Alert;
import com.fincom.sanction.domain.alert.CreateAlertRequest;
import com.fincom.sanction.domain.alert.EscalateAlertRequest;
import com.fincom.sanction.domain.alert.UpdateAlertDecisionRequest;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SanctionAlertsMapper {

	CreateAlertRequest toDomain(CreateAlertRequestDTO dto);

	default UpdateAlertDecisionRequest toDomain(
			UUID alertId, UpdateAlertDecisionRequestDTO dto) {
		return new UpdateAlertDecisionRequest(
				alertId, dto.tenantId(), dto.statusDecision(), dto.decisionNote());
	}

	default EscalateAlertRequest toDomain(
			UUID alertId, EscalateAlertRequestDTO dto) {
		return new EscalateAlertRequest(alertId, dto.tenantId(), dto.assignedTo());
	}

	AlertDTO toDTO(Alert alert);

	List<AlertDTO> toDTO(List<Alert> alerts);
}
