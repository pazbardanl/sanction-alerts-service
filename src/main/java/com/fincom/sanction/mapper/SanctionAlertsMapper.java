package com.fincom.sanction.mapper;

import com.fincom.sanction.contract.AlertDTO;
import com.fincom.sanction.contract.CreateAlertRequestDTO;
import com.fincom.sanction.contract.UpdateAlertDecisionRequestDTO;
import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.CreateAlertRequest;
import com.fincom.sanction.domain.UpdateAlertDecisionRequest;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SanctionAlertsMapper {

	CreateAlertRequest toDomain(CreateAlertRequestDTO dto);

	default UpdateAlertDecisionRequest toUpdateAlertDecisionRequest(
			UUID alertId, UpdateAlertDecisionRequestDTO dto) {
		return new UpdateAlertDecisionRequest(
				alertId, dto.tenantId(), dto.statusDecision(), dto.decisionNote());
	}

	AlertDTO toDTO(Alert alert);

	List<AlertDTO> toDTO(List<Alert> alerts);
}
