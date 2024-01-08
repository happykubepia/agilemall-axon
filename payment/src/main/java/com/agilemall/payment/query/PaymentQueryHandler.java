package com.agilemall.payment.query;

import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.PaymentDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.payment.entity.Payment;
import com.agilemall.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PaymentQueryHandler {
    @Autowired
    PaymentRepository paymentRepository;

    @QueryHandler(queryName = Constants.QUERY_REPORT)
    private PaymentDTO handle(String orderId) {
        log.info("[@QueryHandler] Handle <{}> for Order Id: {}", Constants.QUERY_REPORT,orderId);

        Payment payment = paymentRepository.findByOrderId(orderId);
        if(payment != null) {
            PaymentDTO paymentDTO = new PaymentDTO();
            BeanUtils.copyProperties(payment, paymentDTO);
            List<PaymentDetailDTO> newDetails = payment.getPaymentDetails().stream()
                    .map(o -> new PaymentDetailDTO(orderId, o.getPaymentDetailIdentity().getPaymentId(), o.getPaymentDetailIdentity().getPaymentGbcd(), o.getPaymentAmt()))
                    .collect(Collectors.toList());
            paymentDTO.setPaymentDetails(newDetails);

            return paymentDTO;
        } else {
            return null;
        }
    }

}
