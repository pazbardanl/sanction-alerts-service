package com.fincom.sanction.mapper;

import com.fincom.sanction.contract.AlertDTO;
import com.fincom.sanction.contract.CreateAlertRequestDTO;
import com.fincom.sanction.domain.Alert;
import com.fincom.sanction.domain.CreateAlertRequest;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SanctionAlertsMapper {

	CreateAlertRequest toDomain(CreateAlertRequestDTO dto);
	AlertDTO toDTO(Alert alert);

	List<AlertDTO> toDTO(List<Alert> alerts);
}
